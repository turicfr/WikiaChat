package com.oren.wikia_chat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oren.wikia_chat.client.Client
import com.oren.wikia_chat.client.Room
import com.oren.wikia_chat.client.User
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private lateinit var mMessagesView: RecyclerView
    private lateinit var mChatAdapter: ChatAdapter
    private var mChatItems = mutableListOf<ChatItem>()
    private var mCurrentItemPosition = 0
    private lateinit var mInputMessageView: EditText

    private lateinit var mClient: Client
    private lateinit var mRoom: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mClient = (application as ChatApplication).client
        mRoom = mClient.getRoom(intent.getIntExtra("roomId", 0))

        supportActionBar!!.apply {
            title = ""
            Picasso.get()
                .load(mClient.wikiImageUrl)
                .into(object : Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        setIcon(BitmapDrawable(resources, bitmap))
                        setDisplayShowHomeEnabled(true)
                        setDisplayHomeAsUpEnabled(true)
                    }
                })
        }

        mChatAdapter = ChatAdapter(this, mChatItems).apply {
            onCreateContextMenuListener = { position -> mCurrentItemPosition = position }
        }

        mMessagesView = findViewById<RecyclerView>(R.id.messages).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = mChatAdapter
        }
        registerForContextMenu(mMessagesView)

        mInputMessageView = findViewById<EditText>(R.id.message_input).apply {
            setOnEditorActionListener { _, actionId, _ ->
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

        mRoom.apply {
            onEvent("join") { data -> runOnUiThread { onJoin(data) } }
            onEvent("logout") { data -> runOnUiThread { onLogout(data) } }
            onEvent("part") { data -> runOnUiThread { onLogout(data) } }
            onEvent("kick") { data -> runOnUiThread { onKick(data) } }
            onEvent("ban") { data -> runOnUiThread { onBan(data) } }
            onEvent("chat:add") { data -> runOnUiThread { onMessage(data) } }
            connect()
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menuInflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.private_chat -> {
                val chatItem = mChatItems[mCurrentItemPosition] as ChatItem.Message
                openPrivateChat(chatItem.user)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun openPrivateChat(user: User) {
        mClient.openPrivateChat(user, object : Client.Callback<Room> {
            override fun onSuccess(room: Room) {
                startActivity(Intent(this@ChatActivity, ChatActivity::class.java).apply {
                    putExtra("roomId", room.id)
                })
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("ChatActivity", "openPrivateChat failed: ${throwable.message}")
                throwable.printStackTrace()
            }
        })
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
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = UsersAdapter(mRoom.users).apply {
                setOnClickListener { user -> openPrivateChat(user) }
            }
            addItemDecoration(
                DividerItemDecoration(this@ChatActivity, DividerItemDecoration.VERTICAL)
            )
        }

        BottomSheetDialog(this).apply {
            setContentView(view)
            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRoom.disconnect()
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
        mRoom.sendMessage(mInputMessageView.text.toString())
        mInputMessageView.text.clear()
    }

    private fun onMessage(data: JSONObject) {
        val attrs = data.getJSONObject("attrs")
        val user = mRoom.getUser(attrs.getString("name"))
        val message = attrs.getString("text")
        val last = mChatItems.last()
        if (last is ChatItem.Message && last.user == user) {
            last.messages.add(message)
            mChatAdapter.notifyItemChanged(mChatItems.size - 1)
        } else {
            mChatItems.add(ChatItem.Message(user, mutableListOf(message)))
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
