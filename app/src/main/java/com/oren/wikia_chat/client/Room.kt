package com.oren.wikia_chat.client

import android.net.Uri
import android.util.Log
import io.socket.client.Socket
import org.json.JSONObject

class Room(
    val id: Int,
    private val mUsername: String,
    private val mSocket: Socket,
    private val parent: Room? = null,
) {
    private val mUsers = mutableMapOf<String, User>()
    val privateRooms = if (isPrivate) null else mutableListOf<Room>()
    var unreadMessages = 0

    val isPrivate: Boolean
        get() {
            return parent != null
        }

    val users: List<User>
        get() = mUsers.values.toList()

    fun getUser(username: String) = mUsers[username.toLowerCase()]!!

    fun connect() {
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("Room", "connect")
            send(
                JSONObject()
                    .put("msgType", "command")
                    .put("command", "initquery")
            )
        }
        mSocket.on(Socket.EVENT_DISCONNECT) { Log.d("Room", "disconnect") }
        mSocket.on(Socket.EVENT_CONNECT_ERROR) { Log.d("Room", "connect_error") }
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT) { Log.d("Room", "connect_timeout") }
        onEvent("initial", ::onInitial)
        onEvent("join") { data -> updateUser(data.getJSONObject("attrs")) }
        onEvent("updateUser") { data -> updateUser(data.getJSONObject("attrs")) }
        onEvent("chat:add") { onMessage() }
        onEvent("part") { data -> onLogout(data.getJSONObject("attrs")) }
        onEvent("logout") { data -> onLogout(data.getJSONObject("attrs")) }
        mSocket.connect()
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
        mSocket.send(
            JSONObject()
                .put("id", JSONObject.NULL)
                .put("attrs", attrs)
                .toString()
        )
    }

    fun sendMessage(message: String) {
        if (isPrivate) {
            // TODO: contract
            parent!!.send(
                JSONObject()
                    .put("msgType", "command")
                    .put("command", "openprivate")
                    .put("roomId", id)
            )
        }
        send(
            JSONObject()
                .put("msgType", "chat")
                .put("name", mUsername)
                .put("text", message)
        )
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
        val models = data.getJSONObject("collections").getJSONObject("users").getJSONArray("models")
        for (i in 0 until models.length()) {
            updateUser(models.getJSONObject(i).getJSONObject("attrs"))
        }
    }

    private fun updateUser(attrs: JSONObject) {
        // TODO: Rank
        val username = attrs.getString("name")
        var avatarUri = Uri.parse(attrs.getString("avatarSrc"))
        val segments = avatarUri.pathSegments.slice(0 until avatarUri.pathSegments.size - 2)
        avatarUri = avatarUri.buildUpon().path(segments.joinToString("/")).build()
        updateUser(User(username, avatarUri))
    }

    private fun updateUser(user: User) {
        mUsers[user.name.toLowerCase()] = user
    }

    private fun onMessage() = unreadMessages++

    private fun onLogout(attrs: JSONObject) {
        val username = attrs.getString("name")
        mUsers.remove(username.toLowerCase())
    }
}
