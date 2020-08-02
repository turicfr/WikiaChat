package com.oren.wikia_chat.client

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


class Client {
    private lateinit var wikiaApi: WikiaApi
    internal lateinit var wikiaData: JSONObject
    internal lateinit var siteInfo: JSONObject

    private lateinit var mHttpClient: OkHttpClient

    private lateinit var mUsername: String
    private val mRooms = mutableMapOf<Int, Room>()

    val wikiName: String
        get() = wikiaData.getJSONObject("themeSettings").getString("wordmark-text")

    val username: String
        get() = mUsername

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
        mUsername = username
        mHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor())
            /*.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })*/
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .build()
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

        val siteInfoCallback: retrofit2.Callback<ResponseBody> =
            object : ObjectCallback<Room>(callback) {
                override fun onObject(obj: JSONObject) {
                    siteInfo = obj.getJSONObject("query")
                    val roomId = wikiaData.getInt("roomId")
                    val room = Room(this@Client, roomId)
                    mRooms[roomId] = room
                    callback.onSuccess(room)
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

    fun openPrivateChat(user: User, callback: Callback<Room>) {
        wikiaApi.getPrivateRoomId(
            JSONArray(listOf(mUsername, user.name)),
            siteInfo.getJSONObject("pages").getJSONObject("-1").getString("edittoken")
        )
            .enqueue(object : ObjectCallback<Room>(callback) {
                override fun onObject(obj: JSONObject) {
                    val roomId = obj.getInt("id")
                    val room = Room(this@Client, roomId)
                    mRooms[roomId] = room
                    room.openPrivateChat(user)
                    callback.onSuccess(room)
                }
            })
    }

    fun logout() {
        mRooms.values.forEach { it.disconnect() }
    }
}
