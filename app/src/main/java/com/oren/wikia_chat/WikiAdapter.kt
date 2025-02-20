package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class WikiAdapter : ListAdapter<Wiki, WikiAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Wiki>() {
    override fun areItemsTheSame(oldItem: Wiki, newItem: Wiki) = false
    override fun areContentsTheSame(oldItem: Wiki, newItem: Wiki) = false
}) {
    private var listener: ((Wiki) -> Unit)? = null

    fun setOnClickListener(listener: (Wiki) -> Unit) {
        this.listener = listener
    }

    public override fun getItem(position: Int) = super.getItem(position)!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.wiki_item, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wiki = getItem(position)
        holder.name = wiki.name
        holder.logo = wiki.wordmarkUrl
        holder.itemView.setOnClickListener {
            listener?.invoke(wiki)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mTextView = itemView.findViewById<TextView>(R.id.wiki_name)
        private val mLogoView = itemView.findViewById<ImageView>(R.id.logo)

        var name: String
            get() = mTextView.text.toString()
            set(value) {
                mTextView.text = value
            }

        var logo: String
            get() = throw Exception()
            set(value) {
                if (value.isEmpty()) {
                    // TODO: error drawable
                    return
                }
                Picasso.get()
                    .load(value)
                    .transform(CircleTransform())
                    .into(mLogoView)
            }
    }
}
