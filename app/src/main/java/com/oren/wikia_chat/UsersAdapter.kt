package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.oren.wikia_chat.client.User
import com.squareup.picasso.Picasso

class UsersAdapter :
    ListAdapter<User, UsersAdapter.UserViewHolder>(object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = false
        override fun areContentsTheSame(oldItem: User, newItem: User) = false
    }) {

    private var listener: ((User) -> Unit)? = null

    fun setOnClickListener(listener: (User) -> Unit) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
    )

    override fun onBindViewHolder(viewHolder: UserViewHolder, position: Int) {
        val user = getItem(position)
        val privateRoom = user.privateRoom
        viewHolder.itemView.setOnClickListener {
            if (privateRoom != null) {
                privateRoom.unreadMessages = 0
            }
            viewHolder.unread = false
            listener?.invoke(user)
        }
        viewHolder.username = user.name
        Picasso.get()
            .load(user.avatarUri)
            .transform(CircleTransform())
            .into(viewHolder.avatar)
        viewHolder.unread = privateRoom != null && privateRoom.unreadMessages > 0
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView = itemView.findViewById<TextView>(R.id.username)
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        private val mUnreadMessageBadge: View = itemView.findViewById(R.id.badge)

        var username: String
            get() = mUsernameView.text.toString()
            set(value) {
                mUsernameView.text = value
            }

        var unread: Boolean
            get() = mUnreadMessageBadge.isVisible
            set(value) {
                mUnreadMessageBadge.isVisible = value
            }
    }
}
