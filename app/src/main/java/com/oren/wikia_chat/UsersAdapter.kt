package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class UsersAdapter(private val mUsers: List<User>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return UserViewHolder(LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_user, parent, false))
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val user = mUsers[position]
        viewHolder as UserViewHolder
        viewHolder.username = user.name
        Picasso.get()
            .load(user.avatarUri)
            .transform(CircleTransform())
            .into(viewHolder.avatar)
    }

    override fun getItemCount() = mUsers.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mUsernameView = itemView.findViewById<TextView>(R.id.username)
        val avatar = itemView.findViewById<ImageView>(R.id.avatar)

        var username: String
            get() = mUsernameView.text.toString()
            set(value) {
                mUsernameView.text = value
            }
    }
}
