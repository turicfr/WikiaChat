package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import io.socket.client.Socket

class LoginActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextInputEditText
    private lateinit var mPasswordView: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ChatApplication
        val username = app.sharedPref.getString(getString(R.string.username_key), null)
        val password = app.sharedPref.getString(getString(R.string.password_key), null)
        if (username != null && password != null) {
            login(username, password) {
                show()
            }
            return
        }

        show()
    }

    private fun show() {
        setContentView(R.layout.activity_login)

        mUsernameView = findViewById(R.id.username)
        mPasswordView = findViewById(R.id.password)
        findViewById<Button>(R.id.sign_in_button).setOnClickListener {
            attemptLogin()
        }

        mPasswordView.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_GO -> {
                    attemptLogin()
                    true
                }
                else -> false
            }
        }
    }

    private fun attemptLogin() {
        mUsernameView.error = null
        mPasswordView.error = null

        val username = mUsernameView.text.toString()
        val password = mPasswordView.text.toString()

        if (username == "") {
            mUsernameView.error = getString(R.string.error_field_required)
            mUsernameView.requestFocus()
            return
        }
        if (password == "") {
            mPasswordView.error = getString(R.string.error_field_required)
            mPasswordView.requestFocus()
            return
        }

        login(username, password) { throwable ->
            when (throwable.message) {
                "NotExists" -> {
                    mUsernameView.error = getString(R.string.user_does_not_exist)
                    mUsernameView.requestFocus()
                }
                "WrongPass" -> {
                    mPasswordView.error = getString(R.string.wrong_password)
                    mPasswordView.requestFocus()
                }
            }
        }
    }

    private fun login(username: String, password: String, onFailure: (throwable: Throwable) -> Unit) {
        val app = application as ChatApplication
        app.client = Client("https://vintagepenguin.fandom.com", username, password)
        app.client.login(object : Client.LoginCallback {
            override fun onSuccess() {
                app.client.socket.on(Socket.EVENT_CONNECT) {
                    Log.d("Chat", "connect")
                    with (app.sharedPref.edit()) {
                        putString(getString(R.string.username_key), username)
                        putString(getString(R.string.password_key), password)
                        apply()
                    }
                    setResult(RESULT_OK, Intent())
                    finish()
                }
                app.client.socket.on(Socket.EVENT_DISCONNECT) {
                    Log.d("Chat", "disconnect")
                }
                app.client.socket.on(Socket.EVENT_CONNECT_ERROR) {
                    Log.d("Chat", "connect_error")
                }
                app.client.socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
                    Log.d("Chat", "connect_timeout")
                }
                app.client.socket.connect()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("Chat", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
                onFailure(throwable)
            }
        })
    }
}
