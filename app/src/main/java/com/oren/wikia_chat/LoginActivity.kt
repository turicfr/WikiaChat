package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.PollingXHR
import java.util.logging.Level
import java.util.logging.Logger

class LoginActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextInputLayout
    private lateinit var mPasswordView: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
        Logger.getLogger(Socket::class.java.name).level = Level.ALL
        Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.ALL
        Logger.getLogger(Manager::class.java.name).level = Level.ALL
        Logger.getLogger(PollingXHR::class.java.name).level = Level.ALL

        mUsernameView = findViewById(R.id.username)
        mPasswordView = findViewById(R.id.password)
        val loginButton: Button = findViewById(R.id.sign_in_button)
        loginButton.setOnClickListener {
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        mUsernameView.error = null
        mPasswordView.error = null

        val username = mUsernameView.editText?.text.toString()
        val password = mPasswordView.editText?.text.toString()

        if (username == "") {
            mUsernameView.error = getString(R.string.error_field_required)
            mUsernameView.requestFocus()
            return
        }

        val app = application as ChatApplication
        app.client = Client("https://vintagepenguin.fandom.com", username, password)
        app.client.login(object : Client.LoginCallback {
            override fun onSuccess() {
                app.client.socket.on(Socket.EVENT_CONNECT) {
                    Log.d("Chat", "connect")
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

                Log.e("Chat", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
            }
        })
    }
}
