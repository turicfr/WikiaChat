package com.oren.wikia_chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import kotlin.math.abs

class MessageAdapter(
    context: Context,
    private val mMessages: List<Message>
) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private val mUsernameColors: IntArray = context.resources.getIntArray(R.array.username_colors)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layout = -1
        when (viewType) {
            Message.TYPE_MESSAGE -> layout = R.layout.item_message
            Message.TYPE_LOG -> layout = R.layout.item_log
            Message.TYPE_ACTION -> layout = R.layout.item_action
        }
        return ViewHolder(
            LayoutInflater
            .from(parent.context)
            .inflate(layout, parent, false)
        )
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val message = mMessages[position]
        viewHolder.setMessage(message.message)
        viewHolder.setUsername(message.username)
    }

    override fun getItemCount(): Int {
        return mMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        return mMessages[position].type
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView: TextView = itemView.findViewById(R.id.username)
        private val mMessageView: TextView = itemView.findViewById(R.id.message)

        fun setUsername(username: String) {
            mUsernameView.text = username
            mUsernameView.setTextColor(getUsernameColor(username))
        }

        fun setMessage(message: String) {
            mMessageView.text = message
        }

        private fun getUsernameColor(username: String): Int {
            var hash = 7
            var i = 0
            while (i < username.length) {
                hash = username.codePointAt(i) + (hash shl 5) - hash
                i++
            }
            val index = abs(hash % mUsernameColors.size)
            return mUsernameColors[index]
        }
    }
}
