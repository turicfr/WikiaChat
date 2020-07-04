package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var mMessagesView: RecyclerView
    private lateinit var mAdapter: ChatAdapter
    private var mChatItems = ArrayList<ChatItem>()
    private lateinit var mInputMessageView: EditText
    private lateinit var mClient: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = ChatAdapter(this, mChatItems)
        mMessagesView = findViewById(R.id.messages)
        mMessagesView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
        }

        mInputMessageView = findViewById(R.id.message_input)
        mInputMessageView.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.send_button).setOnClickListener {
            sendMessage()
        }

        startLogin()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val app = application as ChatApplication
        mClient = app.client
        mClient.socket.on("message", onEvent)
    }

    override fun onDestroy() {
        super.onDestroy()

        mClient.socket.disconnect()
        mClient.socket.off(Socket.EVENT_CONNECT)
        mClient.socket.off(Socket.EVENT_DISCONNECT)
        mClient.socket.off(Socket.EVENT_CONNECT_ERROR)
        mClient.socket.off(Socket.EVENT_CONNECT_TIMEOUT)
    }

    private fun startLogin() {
        startActivityForResult(Intent(this, LoginActivity::class.java), 0)
    }

    private fun logout() {
        val app = application as ChatApplication
        with (app.sharedPref.edit()) {
            remove(getString(R.string.username_key))
            remove(getString(R.string.password_key))
            apply()
        }
        mClient.socket.disconnect()
        startLogin()
    }

    private fun addMessage(username: String, message: String) {
        mChatItems.add(ChatItem.Message(username, message))
        mAdapter.notifyItemInserted(mChatItems.size - 1)
        scrollToBottom()
    }

    private fun addLog(message: String) {
        mChatItems.add(ChatItem.Log(message))
        mAdapter.notifyItemChanged(mChatItems.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.itemCount - 1)
    }

    private fun send(attrs: JSONObject) {
        mClient.socket.send(JSONObject().apply {
            put("id", JSONObject.NULL)
            put("attrs", attrs)
        }.toString())
    }

    private fun sendMessage() {
        send(JSONObject().apply {
            put("msgType", "chat")
            put("name", mClient.username)
            put("text", mInputMessageView.text.toString())
        })
        mInputMessageView.text.clear()
    }

    private val onEvent = Emitter.Listener { args ->
        runOnUiThread {
            val obj = args[0] as JSONObject
            Log.d("Chat", "got message: $obj")
            val data = JSONObject(obj.getString("data"))
            when (obj.getString("event")) {
                "meta" -> {}
                "initial" -> onInitial()
                "updateUser" -> onUpdateUser(data)
                "join" -> onJoin(data)
                "logout" -> onLogout(data)
                "part" -> onLogout(data)
                "kick" -> onKick(data)
                "ban" -> onBan(data)
                "chat:add" -> onMessage(data)
            }
        }
    }

    private fun onMessage(data: JSONObject) {
        val username = data.getJSONObject("attrs").getString("name")
        val message = data.getJSONObject("attrs").getString("text")
        addMessage(username, message)
    }

    private fun onBan(data: JSONObject) {
        // TODO
    }

    private fun onKick(data: JSONObject) {
        // TODO
    }

    private fun onLogout(data: JSONObject) {
        val username = data.getJSONObject("attrs").getString("name")
        addLog(resources.getString(R.string.message_user_left, username))
    }

    private fun onJoin(data: JSONObject) {
        val username = data.getJSONObject("attrs").getString("name")
        addLog(resources.getString(R.string.message_user_joined, username));
    }

    private fun onUpdateUser(data: JSONObject) {
        // TODO
    }

    private fun onInitial() {
        // TODO
    }
}
