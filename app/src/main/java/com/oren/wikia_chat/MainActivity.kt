package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.PollingXHR
import java.util.logging.Level
import java.util.logging.Logger

class MainActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextView
    private lateinit var mPasswordView: TextView
    private lateinit var client: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AndroidLoggingHandler.reset(AndroidLoggingHandler())
        Logger.getLogger(Socket::class.java.name).level = Level.ALL
        Logger.getLogger(io.socket.engineio.client.Socket::class.java.name).level = Level.ALL
        Logger.getLogger(Manager::class.java.name).level = Level.ALL
        Logger.getLogger(PollingXHR::class.java.name).level = Level.ALL

        mUsernameView = findViewById(R.id.username)
        mPasswordView = findViewById(R.id.password)
        val loginButton: Button = findViewById(R.id.button)
        loginButton.setOnClickListener {
            attemptLogin()
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

        client = Client("https://vintagepenguin.fandom.com", username, password)
        client.login(object : Client.LoginCallback {
            override fun onSuccess() {
                client.socket.on(Socket.EVENT_CONNECT, onConnect)
                client.socket.on(Socket.EVENT_DISCONNECT) {
                    Log.d("Chat", "disconnect")
                }
                client.socket.on(Socket.EVENT_CONNECT_ERROR) {
                    Log.d("Chat", "connect_error")
                }
                client.socket.on(Socket.EVENT_CONNECT_TIMEOUT) {
                    Log.d("Chat", "connect_timeout")
                }
                client.socket.connect()
            }

            override fun onFailure(throwable: Throwable) {
                when (throwable.message) {
                    "NotExists" -> {
                        mUsernameView.error = "User does not exist"
                        mUsernameView.requestFocus()
                    }
                    "WrongPass" -> {
                        mPasswordView.error = "Wrong password"
                        mPasswordView.requestFocus()
                    }
                }

                Log.e("Chat", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        client.socket.off(Socket.EVENT_CONNECT, onConnect)
    }

    // TODO
    private val onConnect = Emitter.Listener {
        Log.d("Chat", "connect")
        val intent = Intent()
        setResult(RESULT_OK, intent)
        finish()
    }
}
