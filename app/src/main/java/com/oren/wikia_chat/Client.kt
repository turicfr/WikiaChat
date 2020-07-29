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
import java.lang.Exception
import java.net.CookieManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Client(val username: String, private val password: String) {
    private lateinit var wikiaApi: WikiaApi
    private lateinit var wikiaData: JSONObject
    private lateinit var siteInfo: JSONObject

    private lateinit var mHttpClient: OkHttpClient
    private lateinit var mSocket: Socket

    private val mUsersMap = mutableMapOf<String, User>()

    val wikiName: String
        get() = wikiaData.getJSONObject("themeSettings").getString("wordmark-text")

    val users: List<User>
        get() = mUsersMap.values.toList()

    fun getUser(username: String) = mUsersMap[username.toLowerCase()]!!

    // TODO: Remove?
    interface LoginCallback {
        fun onSuccess()
        fun onFailure(throwable: Throwable)
    }

    private abstract class ObjectCallback(private val callback: LoginCallback) : Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
            val body = if (response.isSuccessful) response.body() else response.errorBody()
            val obj = JSONObject(body?.string()!!)
            try {
                onObject(obj)
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
                    "&serverId=${siteInfo.getJSONObject("wikidesc").getString("id")}"
        }
        mSocket = IO.socket("https://${wikiaData.getString("chatServerHost")}", options)
    }

    fun login(callback: LoginCallback) {
        mHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor())
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://services.fandom.com/")
            .client(mHttpClient)
            .build()
        val loginApi = retrofit.create(LoginApi::class.java)

        loginApi.login(username, password).enqueue(object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                if (obj.has("error")) {
                    throw Exception(obj.getString("error_description"))
                }
                callback.onSuccess()
            }
        })
    }

    fun init(url: String, callback: LoginCallback) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(mHttpClient)
            .build()
        wikiaApi = retrofit.create(WikiaApi::class.java)

        val siteInfoCallback: Callback<ResponseBody> = object : ObjectCallback(callback) {
            override fun onObject(obj: JSONObject) {
                siteInfo = obj.getJSONObject("query")
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
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("Chat", "connect")
            send(JSONObject().apply {
                put("msgType", "command")
                put("command", "initquery")
            })
        }
        mSocket.on(Socket.EVENT_DISCONNECT) {
            Log.d("Chat", "disconnect")
        }
        mSocket.on(Socket.EVENT_CONNECT_ERROR) {
            Log.d("Chat", "connect_error")
        }
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT) {
            Log.d("Chat", "connect_timeout")
        }
        onEvent("initial") { data ->
            val models = data
                .getJSONObject("collections")
                .getJSONObject("users")
                .getJSONArray("models")
            for (i in 0 until models.length()) {
                val user = models.getJSONObject(i)
                updateUser(user.getJSONObject("attrs"))
            }
        }
        onEvent("updateUser") { data ->
            updateUser(data.getJSONObject("attrs"))
        }
        onEvent("join") { data ->
            onJoin(data.getJSONObject("attrs"))
        }
        onEvent("logout") { data ->
            onLogout(data.getJSONObject("attrs"))
        }
        onEvent("part") { data ->
            onLogout(data.getJSONObject("attrs"))
        }
        mSocket.connect()
    }

    private fun updateUser(attrs: JSONObject) {
        // TODO: Rank
        val username = attrs.getString("name")
        val avatarSrc = attrs.getString("avatarSrc")
        val user = User(username, avatarSrc)
        mUsersMap[user.name.toLowerCase()] = user
    }

    private fun onJoin(attrs: JSONObject) {
        // TODO: Rank
        val username = attrs.getString("name")
        val avatarSrc = attrs.getString("avatarSrc")
        val user = User(username, avatarSrc)
        mUsersMap[user.name.toLowerCase()] = user
    }

    private fun onLogout(attrs: JSONObject) {
        val username = attrs.getString("name")
        mUsersMap.remove(username.toLowerCase())
    }

    fun disconnect() {
        if (!this::mSocket.isInitialized) return

        mSocket.disconnect()
        mSocket.off(Socket.EVENT_CONNECT)
        mSocket.off(Socket.EVENT_DISCONNECT)
        mSocket.off(Socket.EVENT_CONNECT_ERROR)
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT)
        // TODO: off "message"?
    }

    fun onEvent(event: String, handler: (data: JSONObject) -> Unit) {
        mSocket.on("message") { args ->
            val obj = args[0] as JSONObject
            val data = JSONObject(obj.getString("data"))
            if (obj.getString("event") == event) {
                handler(data)
            }
        }
    }

    fun send(attrs: JSONObject) {
        mSocket.send(JSONObject().apply {
            put("id", JSONObject.NULL)
            put("attrs", attrs)
        }.toString())
    }
}
