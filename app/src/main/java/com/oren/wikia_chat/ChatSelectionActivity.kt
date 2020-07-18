package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatSelectionActivity : AppCompatActivity() {
    private lateinit var mAdapter: MyAdapter
    private val mChatData = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_selection)

        mAdapter = MyAdapter(mChatData) { name ->
            choose(name)
        }

        findViewById<RecyclerView>(R.id.chats).apply {
            addItemDecoration(
                DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
            )
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatSelectionActivity)
            adapter = mAdapter
        }

        findViewById<View>(R.id.fab).setOnClickListener {
            openDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                (application as ChatApplication).logout(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_chat, null)
        AlertDialog.Builder(this)
            .setTitle("Add chat")
            .setView(view)
            .setPositiveButton("OK") { dialog, which ->
                mChatData.add(view.findViewById<EditText>(R.id.chat).text.toString())
                mAdapter.notifyItemInserted(mChatData.size - 1)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.cancel()
            }
            .show()
    }

    private fun choose(name: String) {
        (application as ChatApplication).client.init("https://$name.fandom.com",
            object : Client.LoginCallback {
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
