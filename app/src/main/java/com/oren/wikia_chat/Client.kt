package com.oren.wikia_chat

import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.CookieManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Client(url: String, private val username: String, private val password: String) {
    private var api: LoginApi
    private lateinit var wikiaData: JSONObject
    private lateinit var siteInfo: JSONObject
    lateinit var socket: Socket

    init {
        val interceptor = HttpLoggingInterceptor()
        val cookieHandler = CookieManager()
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(interceptor)
            .cookieJar(JavaNetCookieJar(cookieHandler))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .build()
        api = retrofit.create(LoginApi::class.java)
    }

    interface LoginCallback {
        fun onSuccess()
        fun onFailure(throwable: Throwable)
    }

    private abstract class ObjectCallback(private val callback: LoginCallback) : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            val obj = try {
                JSONObject(response.body()?.string()!!)
            } catch (e: IOException) {
                callback.onFailure(e)
                return
            } catch (e: JSONException) {
                callback.onFailure(e)
                return
            }
            onObject(obj)
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
        options.callFactory = okHttpClient
        options.path = "/socket.io"
        options.query =
            "name=${username}" +
            "&key=${wikiaData.getString("chatkey")}" +
            "&roomId=${wikiaData.getString("roomId")}" +
            "&serverId=${siteInfo.getJSONObject("query").getJSONObject("wikidesc").getString("id")}"
        socket = IO.socket("https://${wikiaData.getString("chatServerHost")}", options)
    }

    fun login(callback: LoginCallback) {
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
                api.siteInfo().enqueue(siteInfoCallback)
            }
        }

        val checkSuccess = object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                val result = obj.getJSONObject("login").getString("result")
                if (result != "Success") {
                    throw Exception(result)
                }
                api.wikiaData().enqueue(wikiDataCallback)
            }
        }

        api.login(username, password).enqueue(object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                val result = obj.getJSONObject("login").getString("result")
                if (result != "NeedToken") {
                    checkSuccess.onObject(obj)
                    return
                }
                val token = obj.getJSONObject("login").getString("token")
                api.login(username, password, token).enqueue(checkSuccess)
            }
        })
    }
}
