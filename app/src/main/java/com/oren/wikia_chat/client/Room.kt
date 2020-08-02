package com.oren.wikia_chat.client

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class Room(private val mClient: Client, val id: Int) {
    private var mSocket: Socket
    private val mUsers = mutableMapOf<String, User>()

    init {
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

        mSocket = IO.socket("https://${mClient.wikiaData.getString("chatServerHost")}",
            IO.Options().apply {
                callFactory = okHttpClient
                path = "/socket.io"
                query = "name=${mClient.username}" +
                        "&key=${mClient.wikiaData.getString("chatkey")}" +
                        "&roomId=$id" +
                        "&serverId=${mClient.siteInfo.getJSONObject("wikidesc").getString("id")}"
            })
    }

    val users: List<User>
        get() = mUsers.values.toList()

    fun getUser(username: String) = mUsers[username.toLowerCase()]!!

    fun connect() {
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("Chat", "connect")
            send(JSONObject().apply {
                put("msgType", "command")
                put("command", "initquery")
            })
        }
        mSocket.on(Socket.EVENT_DISCONNECT) { Log.d("Chat", "disconnect") }
        mSocket.on(Socket.EVENT_CONNECT_ERROR) { Log.d("Chat", "connect_error") }
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT) { Log.d("Chat", "connect_timeout") }
        onEvent("initial", ::onInitial)
        onEvent("join") { data -> onJoin(data.getJSONObject("attrs")) }
        onEvent("updateUser") { data -> updateUser(data.getJSONObject("attrs")) }
        onEvent("openPrivateRoom") { data -> onOpenPrivateRoom(data.getJSONObject("attrs")) }
        onEvent("part") { data -> onLogout(data.getJSONObject("attrs")) }
        onEvent("logout") { data -> onLogout(data.getJSONObject("attrs")) }
        mSocket.connect()
    }

    fun sendMessage(message: String) {
        send(JSONObject().apply {
            put("msgType", "chat")
            put("name", mClient.username)
            put("text", message)
        })
    }

    fun openPrivateChat(user: User) {
        send(JSONObject().apply {
            put("msgType", "command")
            put("command", "openprivate")
            put("roomId", id)
            put("users", JSONArray(listOf(mClient.username, user.name)))
        })
    }

    private fun updateUser(attrs: JSONObject) {
        // TODO: Rank
        val username = attrs.getString("name")
        val avatarSrc = attrs.getString("avatarSrc")
        val user = User(username, avatarSrc)
        mUsers[user.name.toLowerCase()] = user
    }

    fun disconnect() {
        mSocket.disconnect()
        mSocket.off(Socket.EVENT_CONNECT)
        mSocket.off(Socket.EVENT_DISCONNECT)
        mSocket.off(Socket.EVENT_CONNECT_ERROR)
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT)
        // TODO: off "message"?
    }

    private fun send(attrs: JSONObject) {
        mSocket.send(JSONObject().apply {
            put("id", JSONObject.NULL)
            put("attrs", attrs)
        }.toString())
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

    private fun onInitial(data: JSONObject) {
        val models = data
            .getJSONObject("collections")
            .getJSONObject("users")
            .getJSONArray("models")
        for (i in 0 until models.length()) {
            val user = models.getJSONObject(i)
            updateUser(user.getJSONObject("attrs"))
        }
    }

    private fun onJoin(attrs: JSONObject) {
        // TODO: Rank
        val username = attrs.getString("name")
        val avatarSrc = attrs.getString("avatarSrc")
        val user = User(username, avatarSrc)
        mUsers[user.name.toLowerCase()] = user
    }

    private fun onLogout(attrs: JSONObject) {
        val username = attrs.getString("name")
        mUsers.remove(username.toLowerCase())
    }

    private fun onOpenPrivateRoom(attrs: JSONObject) {
        // TODO
    }
}
