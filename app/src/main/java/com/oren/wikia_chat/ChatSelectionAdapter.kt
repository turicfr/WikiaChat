package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatSelectionAdapter(private val mChats: MutableList<String>) :
    RecyclerView.Adapter<ChatSelectionAdapter.ViewHolder>() {

    private var listener: ((String) -> Unit)? = null

    fun setOnClickListener(listener: (String) -> Unit) {
        this.listener = listener
    }

    fun deleteItem(position: Int) {
        mChats.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.chat_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name = mChats[position]
        holder.itemView.setOnClickListener {
            listener?.invoke(holder.name)
        }
    }

    override fun getItemCount() = mChats.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mTextView = itemView.findViewById<TextView>(R.id.chat)

        var name: String
            get() = mTextView.text.toString()
            set(value) {
                mTextView.text = value
            }
    }
}
