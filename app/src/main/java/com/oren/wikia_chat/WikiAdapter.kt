package com.oren.wikia_chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class WikiAdapter(private var mWikis: MutableList<Wiki>) :
    RecyclerView.Adapter<WikiAdapter.ViewHolder>() {

    private var listener: ((Wiki) -> Unit)? = null

    fun setOnClickListener(listener: (Wiki) -> Unit) {
        this.listener = listener
    }

    fun deleteItem(position: Int) {
        mWikis.removeAt(position)
        notifyItemRemoved(position)
    }

    // TODO: remove?
    fun getItemAtPosition(position: Int): Wiki = mWikis[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.chat_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wiki = mWikis[position]
        holder.name = wiki.name
        holder.logo = wiki.wordmarkUrl
        holder.itemView.setOnClickListener {
            listener?.invoke(wiki)
        }
    }

    override fun getItemCount() = mWikis.size

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
