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
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var mMessagesView: RecyclerView
    private lateinit var mChatAdapter: ChatAdapter
    private var mChatItems = ArrayList<ChatItem>()

    private lateinit var mParticipantsAdapter: UsersAdapter
    private var mParticipants = listOf(User("aaa"), User("bbb"), User("ccc"))

    private lateinit var mInputMessageView: EditText

    private lateinit var mClient: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mChatAdapter = ChatAdapter(this, mChatItems)
        mMessagesView = findViewById(R.id.messages)
        mMessagesView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mChatAdapter
        }

        mParticipantsAdapter = UsersAdapter(mParticipants)

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
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_participants -> {
                showParticipants()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showParticipants() {
        val view = layoutInflater.inflate(R.layout.dialog_users, null)
        view.findViewById<RecyclerView>(R.id.participants).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mParticipantsAdapter
        }

        BottomSheetDialog(this).apply {
            setContentView(view)
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mClient = (application as ChatApplication).client
        mClient.apply {
            onEvent("meta") {}
            onEvent("join") { data -> runOnUiThread { onJoin(data) } }
            onEvent("logout") { data -> runOnUiThread { onLogout(data) } }
            onEvent("part") { data -> runOnUiThread { onLogout(data) } }
            onEvent("kick") { data -> runOnUiThread { onKick(data) } }
            onEvent("ban") { data -> runOnUiThread { onBan(data) } }
            onEvent("chat:add") { data -> runOnUiThread { onMessage(data) } }
            // onEvent("updateUser") { data -> runOnUiThread { onUpdateUser(data) } }
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
        mChatAdapter.notifyItemChanged(mChatItems.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        mMessagesView.scrollToPosition(mChatAdapter.itemCount - 1)
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
            mChatAdapter.notifyItemChanged(mChatItems.size - 1)
        } else {
            val avatarSrc = Uri.parse(attrs.getString("avatarSrc"))
            val segments = avatarSrc.pathSegments.slice(0 until avatarSrc.pathSegments.size - 2)
            val newUri = avatarSrc.buildUpon().path(segments.joinToString("/")).build()
            mChatItems.add(ChatItem.Message(username, mutableListOf(message), newUri.toString()))
            mChatAdapter.notifyItemInserted(mChatItems.size - 1)
        }
        scrollToBottom()
    }

    /*private fun onUpdateUser(data: JSONObject) {
        val attrs = data.getJSONObject("attrs")
        val username = attrs.getString("name")
        mParticipants.add(User(username))
        mParticipantsAdapter.notifyItemInserted(mParticipants.size - 1)
    }*/

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
}
