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
import com.oren.wikia_chat.client.Client
import com.oren.wikia_chat.client.Room

class ChatSelectionActivity : AppCompatActivity() {
    private lateinit var mAdapter: ChatSelectionAdapter
    private val mChatData = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_selection)

        mAdapter = ChatSelectionAdapter(mChatData).apply {
            setOnClickListener { name ->
                choose(name)
            }
        }

        findViewById<RecyclerView>(R.id.chats).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ChatSelectionActivity)
            adapter = mAdapter
            addItemDecoration(
                DividerItemDecoration(this@ChatSelectionActivity, DividerItemDecoration.VERTICAL)
            )
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
        // TODO: export to layout
        AlertDialog.Builder(this)
            .setTitle("Add chat")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                mChatData.add(view.findViewById<EditText>(R.id.chat).text.toString())
                mAdapter.notifyItemInserted(mChatData.size - 1)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun choose(name: String) {
        (application as ChatApplication).client.init("https://$name.fandom.com/",
            object : Client.Callback<Room> {
                override fun onSuccess(room: Room) {
                    startActivity(Intent(this@ChatSelectionActivity, ChatActivity::class.java)
                        .apply {
                            putExtra("roomId", room.id)
                        })
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("ChatSelectionActivity", "init failed: ${throwable.message}")
                    throwable.printStackTrace()
                }
            })
    }
}
