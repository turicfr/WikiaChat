package com.oren.wikia_chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class ChatAdapter(private val mContext: Context, private val mChatItems: List<ChatItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ChatItem.TYPE_MESSAGE -> MessageViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_message, parent, false)
            )
            ChatItem.TYPE_LOG -> LogViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_log, parent, false)
            )
            else -> throw RuntimeException()
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val message = mChatItems[position]
        when (viewHolder.itemViewType) {
            ChatItem.TYPE_MESSAGE -> {
                message as ChatItem.Message
                viewHolder as MessageViewHolder
                viewHolder.message = message.message
                viewHolder.username = message.username
            }
            ChatItem.TYPE_LOG -> {
                message as ChatItem.Log
                viewHolder as LogViewHolder
                viewHolder.message = message.message
            }
        }
    }

    override fun getItemCount() = mChatItems.size

    override fun getItemViewType(position: Int) = mChatItems[position].type

    private fun getUsernameColor(username: String): Int {
        val mUsernameColors: IntArray = mContext.resources.getIntArray(R.array.username_colors)
        var hash = 7
        var i = 0
        while (i < username.length) {
            hash = username.codePointAt(i) + (hash shl 5) - hash
            i++
        }
        val index = abs(hash % mUsernameColors.size)
        return mUsernameColors[index]
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView = itemView.findViewById<TextView>(R.id.username)
        private val mMessageView = itemView.findViewById<TextView>(R.id.message)

        var username: String
            get() = mUsernameView.text.toString()
            set(value) {
                mUsernameView.text = value
                mUsernameView.setTextColor(getUsernameColor(value))
            }

        var message: String
            get() = mMessageView.text.toString()
            set(value) {
                mMessageView.text = value
            }
    }

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mMessageView = itemView.findViewById<TextView>(R.id.message)

        var message: String
            get() = mMessageView.text.toString()
            set(value) {
                mMessageView.text = value
            }
    }
}
