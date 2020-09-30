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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.oren.wikia_chat.client.Controller
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

    private lateinit var mUnreadMessageBadge: View

    private var wikiId = 0
    private lateinit var mController: Controller
    private lateinit var mRoom: Room

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        wikiId = intent.getIntExtra("wikiId", 0)
        mController = (application as ChatApplication).client.getController(wikiId)!!
        mRoom = mController.getRoom(intent.getIntExtra("roomId", 0))!!

        if (mController.wikiImageUrl.isEmpty()) {
            title = mController.wikiName
        } else {
            title = ""
            Picasso.get()
                .load(mController.wikiImageUrl)
                .into(object : Target {
                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        supportActionBar!!.apply {
                            setIcon(BitmapDrawable(resources, bitmap))
                            setDisplayShowHomeEnabled(true)
                        }
                    }
                })
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (wikiId in (application as ChatApplication).chats) {
            mChatItems.addAll((application as ChatApplication).chats[wikiId]!!)
        }

        mChatAdapter = ChatAdapter(this).apply {
            submitList(mChatItems)
            onCreateContextMenuListener = { position -> mCurrentItemPosition = position }
        }

        mMessagesView = findViewById<RecyclerView>(R.id.messages).apply {
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
            onEvent("openPrivateRoom") { data -> runOnUiThread { onOpenPrivateRoom(data) } }
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

    override fun onContextItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.private_chat -> {
                val chatItem = mChatItems[mCurrentItemPosition] as ChatItem.Message
                openPrivateChat(chatItem.user)
                true
            }
            else -> super.onContextItemSelected(item)
        }

    private fun openPrivateChat(user: User) {
        mController.openPrivateChat(user, object : Controller.Callback<Room> {
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
        val menuItem = menu!!.findItem(R.id.action_participants)
        mUnreadMessageBadge = menuItem.actionView.findViewById(R.id.badge)
        // this is necessary because the item in menu_chat has app:actionLayout
        menuItem.actionView.setOnClickListener {
            onOptionsItemSelected(menuItem)
        }
        menuInflater.inflate(R.menu.menu_logout, menu)
        setupBadge()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
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

    private fun showParticipants() {
        val view = layoutInflater.inflate(R.layout.dialog_users, null)
        view.findViewById<RecyclerView>(R.id.participants).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = UsersAdapter().apply {
                submitList(mRoom.users)
                setOnClickListener { user ->
                    setupBadge()
                    openPrivateChat(user)
                }
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
        (application as ChatApplication).chats[wikiId] = mChatItems
        mRoom.offEvent("message")
    }

    private fun setupBadge() {
        if (!mRoom.isPrivate) {
            // TODO: contract
            val count = mRoom.privateRooms!!.sumBy { it.unreadMessages }
            mUnreadMessageBadge.isVisible = count != 0
            // mUnreadMessageBadge.text = count.coerceAtMost(99)
        }
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
        val last = mChatItems.lastOrNull()
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

    private fun onOpenPrivateRoom(data: JSONObject) {
        mUnreadMessageBadge.isVisible = true

        val users = data.getJSONObject("attrs").getJSONArray("users")
        var user: User? = null
        for (i in 0 until users.length()) {
            val username = users.getString(i)
            if (username != mController.user.name) {
                user = mRoom.getUser(username)
                break
            }
        }

        val roomId = data.getJSONObject("attrs").getInt("roomId")
        val room = mController.getRoom(roomId) ?: mController.addPrivateRoom(roomId)
        room.unreadMessages++
        user!!.privateRoom = room
        // TODO: always call connect?
        room.connect()
    }
}
