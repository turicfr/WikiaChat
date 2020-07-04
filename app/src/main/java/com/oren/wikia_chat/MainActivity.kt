package com.oren.wikia_chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        mClient = (application as ChatApplication).client
        mClient.apply {
            onEvent("meta") {}
            onEvent("initial") { data -> runOnUiThread { onInitial(data) } }
            onEvent("updateUser") { data -> runOnUiThread { onUpdateUser(data) } }
            onEvent("join") { data -> runOnUiThread { onJoin(data) } }
            onEvent("logout") { data -> runOnUiThread { onLogout(data) } }
            onEvent("part") { data -> runOnUiThread { onLogout(data) } }
            onEvent("kick") { data -> runOnUiThread { onKick(data) } }
            onEvent("ban") { data -> runOnUiThread { onBan(data) } }
            onEvent("chat:add") { data -> runOnUiThread { onMessage(data) } }
            connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mClient.disconnect()
    }

    private fun startLogin() {
        startActivityForResult(Intent(this, LoginActivity::class.java), 0)
    }

    private fun logout() {
        val sharedPref = (application as ChatApplication).sharedPref
        with (sharedPref.edit()) {
            remove(getString(R.string.username_key))
            remove(getString(R.string.password_key))
            apply()
        }
        mClient.disconnect()
        startLogin()
    }

    private fun addLog(message: String) {
        mChatItems.add(ChatItem.Log(message))
        mAdapter.notifyItemChanged(mChatItems.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.itemCount - 1)
    }

    private fun sendMessage() {
        mClient.send(JSONObject().apply {
            put("msgType", "chat")
            put("name", mClient.username)
            put("text", mInputMessageView.text.toString())
        })
        mInputMessageView.text.clear()
    }

    private fun onMessage(data: JSONObject) {
        val attrs = data.getJSONObject("attrs")
        val username = attrs.getString("name")
        val message = attrs.getString("text")
        val last = mChatItems.last()
        if (last is ChatItem.Message && last.username == username) {
            last.messages.add(message)
            mAdapter.notifyItemChanged(mChatItems.size - 1)
        } else {
            val avatarSrc = Uri.parse(attrs.getString("avatarSrc"))
            val segments = avatarSrc.pathSegments.slice(0 until avatarSrc.pathSegments.size - 2)
            val newUri = avatarSrc.buildUpon().path(segments.joinToString("/")).build()
            mChatItems.add(ChatItem.Message(username, mutableListOf(message), newUri.toString()))
            mAdapter.notifyItemInserted(mChatItems.size - 1)
        }
        scrollToBottom()
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

    private fun onInitial(data: JSONObject) {
        // TODO
    }
}
