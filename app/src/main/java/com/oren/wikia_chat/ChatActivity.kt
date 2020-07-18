package com.oren.wikia_chat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private lateinit var mMessagesView: RecyclerView
    private lateinit var mChatAdapter: ChatAdapter
    private var mChatItems = ArrayList<ChatItem>()

    private lateinit var mInputMessageView: EditText

    private lateinit var mClient: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mClient = (application as ChatApplication).client

        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            title = mClient.wikiName
        }

        mChatAdapter = ChatAdapter(this, mChatItems)
        mMessagesView = findViewById<RecyclerView>(R.id.messages).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = mChatAdapter
        }

        mInputMessageView = findViewById<EditText>(R.id.message_input).apply {
            setOnEditorActionListener { v, actionId, event ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_SEND -> {
                        sendMessage()
                        true
                    }
                    else -> false
                }
            }
        }

        findViewById<ImageButton>(R.id.send_button).setOnClickListener {
            sendMessage()
        }

        mClient.apply {
            onEvent("meta") {}
            onEvent("join") { data -> runOnUiThread { onJoin(data) } }
            onEvent("logout") { data -> runOnUiThread { onLogout(data) } }
            onEvent("part") { data -> runOnUiThread { onLogout(data) } }
            onEvent("kick") { data -> runOnUiThread { onKick(data) } }
            onEvent("ban") { data -> runOnUiThread { onBan(data) } }
            onEvent("chat:add") { data -> runOnUiThread { onMessage(data) } }
            connect()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        menuInflater.inflate(R.menu.menu_logout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                (application as ChatApplication).logout(this)
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
            addItemDecoration(
                DividerItemDecoration(this@ChatActivity, DividerItemDecoration.VERTICAL)
            )
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = UsersAdapter(mClient.users)
        }

        BottomSheetDialog(this).apply {
            setContentView(view)
            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mClient.disconnect()
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
            val user = mClient.getUser(username)
            mChatItems.add(
                ChatItem.Message(username, mutableListOf(message), user.avatarUri.toString())
            )
            mChatAdapter.notifyItemInserted(mChatItems.size - 1)
        }
        scrollToBottom()
    }

    private fun onBan(data: JSONObject) {
        val attrs = data.getJSONObject("attrs")
        val username = attrs.getString("kickedUserName")
        val moderator = attrs.getString("moderatorName")
        val actionId = if (attrs.getInt("time") == 0)
            R.string.message_user_banned else R.string.message_user_unbanned
        addLog(resources.getString(actionId, username, moderator))
    }

    private fun onKick(data: JSONObject) {
        val attrs = data.getJSONObject("attrs")
        val username = attrs.getString("kickedUserName")
        val moderator = attrs.getString("moderatorName")
        addLog(resources.getString(R.string.message_user_kicked, username, moderator))
    }

    private fun onLogout(data: JSONObject) {
        val username = data.getJSONObject("attrs").getString("name")
        addLog(resources.getString(R.string.message_user_left, username))
    }

    private fun onJoin(data: JSONObject) {
        val username = data.getJSONObject("attrs").getString("name")
        addLog(resources.getString(R.string.message_user_joined, username))
    }
}
