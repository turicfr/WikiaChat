package com.oren.wikia_chat.client

import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.ConnectionPool
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.net.CookieManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Client {
    private lateinit var wikiaApi: WikiaApi
    private lateinit var wikiaData: JSONObject
    private lateinit var siteInfo: JSONObject

    private val mHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor())
        /*.addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })*/
        .cookieJar(JavaNetCookieJar(CookieManager()))
        .build()

    private lateinit var mUser: User
    private lateinit var mMainRoom: Room
    private val mRooms = mutableMapOf<Int, Room>()

    val wikiName: String
        get() = wikiaData.getJSONObject("themeSettings").getString("wordmark-text")

    val wikiImageUrl: String
        get() = wikiaData.getJSONObject("themeSettings").getString("wordmark-image-url")

    val user: User
        get() = mUser

    fun getRoom(id: Int) = mRooms[id]!!

    interface Callback<T> {
        fun onSuccess(value: T)
        fun onFailure(throwable: Throwable)
    }

    private abstract class ObjectCallback<T>(private val callback: Callback<T>) :
        retrofit2.Callback<ResponseBody> {
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

    fun login(username: String, password: String, callback: Callback<Unit>) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://services.fandom.com/")
            .client(mHttpClient)
            .build()
        val loginApi = retrofit.create(LoginApi::class.java)
        loginApi.login(username, password).enqueue(object : ObjectCallback<Unit>(callback) {
            override fun onObject(obj: JSONObject) {
                if (obj.has("error")) {
                    throw Exception(obj.getString("error_description"))
                }
                mUser = User(
                    username,
                    "https://services.fandom.com/user-avatar/user/${obj.getString("user_id")}/avatar"
                )
                callback.onSuccess(Unit)
            }
        })
    }

    fun init(url: String, callback: Callback<Room>) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(mHttpClient)
            .build()
        wikiaApi = retrofit.create(WikiaApi::class.java)

        val siteInfoCallback = object : ObjectCallback<Room>(callback) {
            override fun onObject(obj: JSONObject) {
                siteInfo = obj.getJSONObject("query")
                val roomId = wikiaData.getInt("roomId")
                mMainRoom = Room(roomId, mUser.name, createSocket(roomId))
                mRooms[roomId] = mMainRoom
                callback.onSuccess(mMainRoom)
            }
        }

        val wikiDataCallback = object : ObjectCallback<Room>(callback) {
            override fun onObject(obj: JSONObject) {
                wikiaData = obj
                wikiaApi.siteInfo().enqueue(siteInfoCallback)
            }
        }

        wikiaApi.wikiaData().enqueue(wikiDataCallback)
    }

    private fun createSocket(roomId: Int): Socket {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        val okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .build()
        IO.setDefaultOkHttpCallFactory(okHttpClient)

        return IO.socket("https://${wikiaData.getString("chatServerHost")}", IO.Options().apply {
            callFactory = okHttpClient
            path = "/socket.io"
            query = "name=${mUser.name}" +
                    "&key=${wikiaData.getString("chatkey")}" +
                    "&roomId=$roomId" +
                    "&serverId=${siteInfo.getJSONObject("wikidesc").getString("id")}"
        })
    }

    fun openPrivateChat(user: User, callback: Callback<Room>) {
        wikiaApi.getPrivateRoomId(
            JSONArray(listOf(mUser.name, user.name)),
            siteInfo.getJSONObject("pages").getJSONObject("-1").getString("edittoken")
        ).enqueue(object : ObjectCallback<Room>(callback) {
            override fun onObject(obj: JSONObject) {
                val roomId = obj.getInt("id")
                val room = Room(roomId, mUser.name, createSocket(roomId), mMainRoom)
                mRooms[roomId] = room
                callback.onSuccess(room)
            }
        })
    }

    fun logout() {
        mRooms.values.forEach { it.disconnect() }
    }
}
