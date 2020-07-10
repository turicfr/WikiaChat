package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatSelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var chatData = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_selection)

        viewManager = LinearLayoutManager(this)

        mAdapter = MyAdapter(chatData) { textView ->
            choose("https://${textView.text}.fandom.com")
        }

        recyclerView = findViewById<RecyclerView>(R.id.chats).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL))
        }

        findViewById<View>(R.id.fab).setOnClickListener {
            openDialog()
        }
    }

    private fun openDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add chat")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Wiki name"
        }
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            chatData.add(input.text.toString())
            mAdapter.notifyItemInserted(chatData.size - 1)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun choose(url: String) {
        val client = (application as ChatApplication).client
        client.init(url, object : Client.LoginCallback {
            override fun onSuccess() {
                startActivity(Intent(this@ChatSelectionActivity, ChatActivity::class.java))
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("ServerSelectionActivity", "Init socket failed: ${throwable.message}")
                throwable.printStackTrace()
                onFailure(throwable)
            }
        })
    }
}
