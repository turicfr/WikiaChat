package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oren.wikia_chat.client.User
import com.squareup.picasso.Picasso

class UsersAdapter(private val mUsers: List<User>) :
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private var listener: ((User) -> Unit)? = null

    fun setOnClickListener(listener: (User) -> Unit) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_user, parent, false)
        )
    }

    override fun onBindViewHolder(viewHolder: UserViewHolder, position: Int) {
        val user = mUsers[position]
        viewHolder.itemView.setOnClickListener {
            listener?.invoke(user)
        }
        viewHolder.username = user.name
        Picasso.get()
            .load(user.avatarUri)
            .transform(CircleTransform())
            .into(viewHolder.avatar)
    }

    override fun getItemCount() = mUsers.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView = itemView.findViewById<TextView>(R.id.username)
        val avatar: ImageView = itemView.findViewById(R.id.avatar)

        var username: String
            get() = mUsernameView.text.toString()
            set(value) {
                mUsernameView.text = value
            }
    }
}
