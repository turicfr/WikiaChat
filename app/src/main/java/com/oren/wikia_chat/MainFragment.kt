package com.oren.wikia_chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject

class MainFragment : Fragment() {
    private lateinit var mMessagesView: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<MessageAdapter.ViewHolder>
    private var mMessages = ArrayList<Message>()
    private lateinit var mInputMessageView: EditText
    private var mUsername: String? = null
    private lateinit var mSocket: Socket

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mAdapter = MessageAdapter(context, mMessages)
    }

     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityForResult(Intent(activity, LoginActivity::class.java), 0)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMessagesView = view.findViewById(R.id.messages)
        mMessagesView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
        }

        mInputMessageView = view.findViewById(R.id.message_input)
        mInputMessageView.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    sendMessage(mInputMessageView.text.toString())
                    true
                }
                else -> false
            }
        }

        val sendButton: ImageButton = view.findViewById(R.id.send_button)
        sendButton.setOnClickListener {
            sendMessage(mInputMessageView.text.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val app = activity?.application as ChatApplication
        mUsername = app.client.username
        mSocket = app.client.socket
        mSocket.on("message", onEvent)
    }

    override fun onDestroy() {
        super.onDestroy()

        mSocket.disconnect()
        mSocket.off(Socket.EVENT_CONNECT)
        mSocket.off(Socket.EVENT_DISCONNECT)
        mSocket.off(Socket.EVENT_CONNECT_ERROR)
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT)
    }

    private fun addMessage(username: String, message: String) {
        mMessages.add(Message.Builder(Message.TYPE_MESSAGE)
            .username(username)
            .message(message)
            .build()
        )
        mAdapter.notifyItemInserted(mMessages.size - 1)
        scrollToBottom()
    }

    private fun addLog(message: String) {
        mMessages.add(Message.Builder(Message.TYPE_LOG)
            .message(message)
            .build()
        )
        mAdapter.notifyItemChanged(mMessages.size - 1)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.itemCount - 1)
    }

    private fun send(attrs: JSONObject) {
        val root = JSONObject().apply {
            put("id", JSONObject.NULL)
            put("attrs", attrs)
        }
        Log.d("Chat", "send -> $root")
        // TODO: is toString needed?
        mSocket.send(root.toString())
    }

    private fun sendMessage(message: String) {
        send(JSONObject().apply {
            put("msgType", "chat")
            put("name", mUsername)
            put("text", message)
        })
    }

    private val onEvent = Emitter.Listener { args ->
        activity?.runOnUiThread {
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
