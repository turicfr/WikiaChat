package com.oren.wikia_chat

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.oren.wikia_chat.client.Client
import com.oren.wikia_chat.client.Room
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.runBlocking

class WikiSelectionActivity : AppCompatActivity() {
    private lateinit var mClient: Client
    private lateinit var mAdapter: WikiAdapter
    private lateinit var mWikis: MutableList<Wiki>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wiki_selection)

        mClient = (application as ChatApplication).client

        supportActionBar!!.title = mClient.user.name
        // TODO
        Picasso.get()
            // .load(mClient.user.avatarUri)
            .load("https://vignette.wikia.nocookie.net/messaging/images/1/19/Avatar.jpg")
            .resize(72, 72)
            .transform(CircleTransform())
            .into(object : Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    Log.d("onPrepareLoad", "Error!!! $placeHolderDrawable")
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Log.d("onBitmapFailed", "Error!!! $e, $errorDrawable")
                    /*supportActionBar!!.setIcon(errorDrawable)*/
                    /*supportActionBar!!.setIcon(
                        BitmapDrawable(
                            resources
                            Picasso.get()
                                .load("https://vignette.wikia.nocookie.net/messaging/images/1/19/Avatar.jpg")
                                .get()
                        )
                    )*/
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    supportActionBar!!.setDisplayShowHomeEnabled(true)
                    supportActionBar!!.setIcon(BitmapDrawable(resources, bitmap))
                }
            })

        runBlocking {
            mWikis = (application as ChatApplication).mDatabase.wikiDao().getAll().toMutableList()
        }
        mAdapter = WikiAdapter(mWikis).apply {
            setOnClickListener(::choose)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.wikis).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@WikiSelectionActivity)
            adapter = mAdapter
            addItemDecoration(
                DividerItemDecoration(this@WikiSelectionActivity, DividerItemDecoration.VERTICAL)
            )
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                private var itemCount = 0
                private val background =
                    ColorDrawable(ContextCompat.getColor(context, R.color.colorDelete))

                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    if (parent.itemAnimator!!.isRunning && (state.itemCount <= itemCount || itemCount == 0)) {
                        itemCount = state.itemCount

                        var lastViewComingDown: View? = null
                        var firstViewComingUp: View? = null

                        val childCount = parent.layoutManager!!.childCount
                        for (i in 0 until childCount) {
                            val child = parent.layoutManager!!.getChildAt(i)!!
                            if (child.translationY < 0) {
                                lastViewComingDown = child
                            } else if (child.translationY > 0 && firstViewComingUp == null) {
                                firstViewComingUp = child
                            }
                        }

                        var top = 0
                        var bottom = 0
                        if (lastViewComingDown != null && firstViewComingUp != null) {
                            top =
                                lastViewComingDown.bottom + lastViewComingDown.translationY.toInt()
                            bottom = firstViewComingUp.top + firstViewComingUp.translationY.toInt()
                        } else if (lastViewComingDown != null) {
                            top =
                                lastViewComingDown.bottom + lastViewComingDown.translationY.toInt()
                            bottom = lastViewComingDown.bottom
                        } else if (firstViewComingUp != null) {
                            top = firstViewComingUp.top
                            bottom = firstViewComingUp.top + firstViewComingUp.translationY.toInt()
                        }

                        background.setBounds(0, top, parent.width, bottom)
                        background.draw(c)
                    }

                    super.onDraw(c, parent, state)
                }
            })
        }
        ItemTouchHelper(SwipeToDeleteCallback(this, mAdapter)).attachToRecyclerView(recyclerView)

        findViewById<View>(R.id.fab).setOnClickListener {
            openDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_logout -> {
                (application as ChatApplication).logout(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun openDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_chat, null)

        var wiki: Wiki? = null
        val button = AlertDialog.Builder(this)
            .setTitle("Add Wiki") // TODO: extract string
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                mWikis.add(wiki!!)
                mAdapter.notifyItemInserted(mWikis.size - 1)
                runBlocking {
                    (application as ChatApplication).mDatabase.wikiDao().insert(wiki!!)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
            .getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = false

        val adapter = AutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line)
        dialogView.findViewById<DelayAutoCompleteTextView>(R.id.wiki_name).apply {
            loadingIndicator = dialogView.findViewById(R.id.progress_bar)
            setAdapter(adapter)
            setOnItemClickListener { _, _, i, _ ->
                wiki = adapter.getItem(i)
                button.isEnabled = true
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    button.isEnabled = false
                }

                override fun afterTextChanged(p0: Editable?) {}
            })
        }
    }

    private fun choose(wiki: Wiki) {
        mClient.init("https://${wiki.domain}/", object : Client.Callback<Room> {
            override fun onSuccess(room: Room) {
                startActivity(Intent(this@WikiSelectionActivity, ChatActivity::class.java).apply {
                    putExtra("roomId", room.id)
                })
            }

            override fun onFailure(throwable: Throwable) {
                Snackbar.make(
                    findViewById(R.id.coordinator),
                    R.string.error_chat_not_enabled,
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
        })
    }
}
