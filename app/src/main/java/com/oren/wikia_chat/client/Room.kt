package com.oren.wikia_chat.client

import android.net.Uri
import android.util.Log
import io.socket.client.Socket
import io.socket.emitter.Emitter
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
            .on(Socket.EVENT_DISCONNECT) { Log.d("Room", "disconnect") }
            .on(Socket.EVENT_CONNECT_ERROR) { Log.d("Room", "connect_error") }
            .on(Socket.EVENT_CONNECT_TIMEOUT) { Log.d("Room", "connect_timeout") }
            .on("message", listener)
        mSocket.connect()
    }

    fun disconnect() {
        mSocket.disconnect()
            .off(Socket.EVENT_CONNECT)
            .off(Socket.EVENT_DISCONNECT)
            .off(Socket.EVENT_CONNECT_ERROR)
            .off(Socket.EVENT_CONNECT_TIMEOUT)
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

    // TODO: remove duplicated code
    private val listener = Emitter.Listener { args ->
        val obj = args[0] as JSONObject
        val data = JSONObject(obj.getString("data"))
        when (obj.getString("event")) {
            "initial" -> onInitial(data)
            "join" -> updateUser(data.getJSONObject("attrs"))
            "updateUser" -> updateUser(data.getJSONObject("attrs"))
            "chat:add" -> onMessage()
            "part" -> onLogout(data.getJSONObject("attrs"))
            "logout" -> onLogout(data.getJSONObject("attrs"))
        }
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

    fun offEvent(event: String) {
        for (i in mSocket.listeners(event)) {
            if (i != listener) {
                mSocket.off(event, i)
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
