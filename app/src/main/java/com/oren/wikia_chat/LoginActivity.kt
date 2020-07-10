package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextInputLayout
    private lateinit var mPasswordView: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = (application as ChatApplication).sharedPref
        val username = sharedPref.getString(getString(R.string.username_key), null)
        val password = sharedPref.getString(getString(R.string.password_key), null)
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

        mPasswordView.editText!!.setOnEditorActionListener { v, actionId, event ->
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

        val username = mUsernameView.editText!!.text.toString()
        val password = mPasswordView.editText!!.text.toString()

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
        app.client = Client(username, password)
        app.client.login(object : Client.LoginCallback {
            override fun onSuccess() {
                with (app.sharedPref.edit()) {
                    putString(getString(R.string.username_key), username)
                    putString(getString(R.string.password_key), password)
                    apply()
                }
                startActivity(Intent(this@LoginActivity, ChatSelectionActivity::class.java))
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("LoginActivity", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
                onFailure(throwable)
            }
        })
    }
}
