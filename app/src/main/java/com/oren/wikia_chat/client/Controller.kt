package com.oren.wikia_chat.client

import io.socket.client.IO
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit

class Controller(private val mClient: Client) {
    private lateinit var mWikiaApi: WikiaApi
    private lateinit var mWikiaData: JSONObject
    private lateinit var mSiteInfo: JSONObject

    lateinit var mainRoom: Room
        private set
    private val mRooms = mutableMapOf<Int, Room>()

    val wikiName: String
        get() = mWikiaData.getJSONObject("themeSettings").getString("wordmark-text")

    val wikiImageUrl: String
        get() = mWikiaData.getJSONObject("themeSettings").getString("wordmark-image-url")

    val user
        get() = mClient.user

    fun getRoom(id: Int) = mRooms[id]

    fun addPrivateRoom(id: Int): Room {
        val room = Room(id, mClient.user.name, createSocket(id), mainRoom)
        mainRoom.privateRooms!!.add(room)
        mRooms[id] = room
        return room
    }

    interface Callback<T> {
        fun onSuccess(value: T)
        fun onFailure(throwable: Throwable)
    }

    fun init(httpClient: OkHttpClient, url: String, callback: Callback<Controller>) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(httpClient)
            .build()
        mWikiaApi = retrofit.create(WikiaApi::class.java)

        val siteInfoCallback = object : ObjectCallback<Controller>(callback) {
            override fun onObject(obj: JSONObject) {
                mSiteInfo = obj.getJSONObject("query")
                val roomId = mWikiaData.getInt("roomId")
                mainRoom = Room(roomId, mClient.user.name, createSocket(roomId))
                mRooms[roomId] = mainRoom
                callback.onSuccess(this@Controller)
            }
        }

        val wikiDataCallback = object : ObjectCallback<Controller>(callback) {
            override fun onObject(obj: JSONObject) {
                mWikiaData = obj
                mWikiaApi.siteInfo().enqueue(siteInfoCallback)
            }
        }

        mWikiaApi.wikiaData().enqueue(wikiDataCallback)
    }

    private fun createSocket(roomId: Int) =
        IO.socket("https://${mWikiaData.getString("chatServerHost")}", IO.Options().apply {
            path = "/socket.io"
            query = "name=${mClient.user.name}" +
                    "&key=${mWikiaData.getString("chatkey")}" +
                    "&roomId=$roomId" +
                    "&serverId=${mSiteInfo.getJSONObject("wikidesc").getString("id")}"
        })

    fun openPrivateChat(user: User, callback: Callback<Room>) {
        mWikiaApi.getPrivateRoomId(
            JSONArray(listOf(mClient.user.name, user.name)),
            mSiteInfo.getJSONObject("pages").getJSONObject("-1").getString("edittoken"),
        ).enqueue(object : ObjectCallback<Room>(callback) {
            override fun onObject(obj: JSONObject) {
                callback.onSuccess(addPrivateRoom(obj.getInt("id")))
            }
        })
    }

    fun logout() {
        mRooms.values.forEach {
            it.disconnect()
        }
    }
}
