package com.oren.wikia_chat

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.net.CookieManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Client(val username: String, private val password: String) {
    private lateinit var wikiaData: JSONObject
    private lateinit var siteInfo: JSONObject
    private lateinit var client: OkHttpClient
    private lateinit var socket: Socket

    interface LoginCallback {
        fun onSuccess()
        fun onFailure(throwable: Throwable)
    }

    private abstract class ObjectCallback(private val callback: LoginCallback) : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            try {
                onObject(JSONObject(response.body()?.string()!!))
            } catch (e: Throwable) {
                callback.onFailure(e)
                return
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            callback.onFailure(t)
        }

        abstract fun onObject(obj: JSONObject)
    }

    private fun initSocket() {
        val mySSLContext = SSLContext.getInstance("TLS")
        mySSLContext.init(null, arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }

            override fun checkClientTrusted(
                p0: Array<out X509Certificate>?,
                p1: String?
            ) {}

            override fun checkServerTrusted(
                p0: Array<out X509Certificate>?,
                p1: String?
            ) {}
        }), SecureRandom())

        val okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .hostnameVerifier(HostnameVerifier { hostname, session -> true })
            .sslSocketFactory(mySSLContext.socketFactory, object : X509TrustManager {
                override fun checkClientTrusted(
                    p0: Array<out X509Certificate>?,
                    p1: String?
                ) {}

                override fun checkServerTrusted(
                    p0: Array<out X509Certificate>?,
                    p1: String?
                ) {}

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
            .build()
        IO.setDefaultOkHttpCallFactory(okHttpClient)

        val options = IO.Options()
        options.apply {
            callFactory = okHttpClient
            path = "/socket.io"
            query = "name=${username}" +
                    "&key=${wikiaData.getString("chatkey")}" +
                    "&roomId=${wikiaData.getString("roomId")}" +
                    "&serverId=${siteInfo.getJSONObject("query").getJSONObject("wikidesc").getString("id")}"
        }
        socket = IO.socket("https://${wikiaData.getString("chatServerHost")}", options)
    }

    fun login(callback: LoginCallback) {
        client = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor())
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://services.fandom.com/")
            .client(client)
            .build()
        val loginApi = retrofit.create(LoginApi::class.java)

        loginApi.login(username, password).enqueue(object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                if (obj.has("error")) {
                    // TODO: handle errors
                    return
                }
                callback.onSuccess()
            }
        })
    }

    fun init(url: String, callback: LoginCallback) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .build()
        val wikiaApi = retrofit.create(WikiaApi::class.java)

        val siteInfoCallback: Callback<ResponseBody> = object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                siteInfo = obj
                initSocket()
                callback.onSuccess()
            }
        }

        val wikiDataCallback = object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                wikiaData = obj
                wikiaApi.siteInfo().enqueue(siteInfoCallback)
            }
        }

        wikiaApi.wikiaData().enqueue(wikiDataCallback)
    }

    fun connect() {
        socket.on(Socket.EVENT_CONNECT) {
            Log.d("Chat", "connect")
            send(JSONObject().apply {
                put("msgType", "command")
                put("command", "initquery")
            })
        }
        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d("Chat", "disconnect")
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d("Chat", "connect_error")
        }
        socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Log.d("Chat", "connect_timeout")
        }
        socket.connect()
    }

    fun disconnect() {
        socket.disconnect()

        socket.off(Socket.EVENT_CONNECT)
        socket.off(Socket.EVENT_DISCONNECT)
        socket.off(Socket.EVENT_CONNECT_ERROR)
        socket.off(Socket.EVENT_CONNECT_TIMEOUT)
        // TODO: off "message"?
    }

    fun onEvent(event: String, handler: (data: JSONObject) -> Unit) {
        socket.on("message") { args ->
            val obj = args[0] as JSONObject
            val data = JSONObject(obj.getString("data"))
            if (obj.getString("event") == event) {
                handler(data)
            }
        }
    }

    fun send(attrs: JSONObject) {
        socket.send(JSONObject().apply {
            put("id", JSONObject.NULL)
            put("attrs", attrs)
        }.toString())
    }
}
