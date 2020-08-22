package com.oren.wikia_chat

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.oren.wikia_chat.client.WikiaApi
import org.json.JSONObject
import retrofit2.Retrofit

class AutoCompleteAdapter(context: Context, textViewResourceId: Int) :
    ArrayAdapter<Wiki>(context, textViewResourceId), Filterable {
    private val mData = mutableListOf<Wiki>()

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            if (constraint == null) {
                return FilterResults()
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("https://wikia.com/")
                .build()
            val wikiaApi = retrofit.create(WikiaApi::class.java)
            val response = wikiaApi.getWikis(constraint.toString()).execute()
            val items = JSONObject(response.body()?.string()!!).getJSONArray("items")
            mData.clear()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                mData.add(
                    Wiki(
                        item.getInt("id"),
                        item.getString("name"),
                        item.getString("domain"),
                        item.getString("wordmark"), // TODO: handle empty string
                    )
                )
            }
            return FilterResults().apply {
                values = mData
                count = mData.size
            }
        }

        override fun publishResults(contraint: CharSequence?, results: FilterResults?) {
            if (results != null && results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }
    }

    override fun getCount() = mData.size

    override fun getItem(index: Int) = mData[index]
}
