package com.oren.wikia_chat

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextInputLayout
    private lateinit var mPasswordView: TextInputLayout
    private lateinit var mErrorMessageView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = (application as ChatApplication).sharedPref
        val username = sharedPref.getString(getString(R.string.username_key), null)
        val password = sharedPref.getString(getString(R.string.password_key), null)
        if (username != null && password != null) {
            (application as ChatApplication).login(this, username, password) {
                show()
            }
        } else {
            show()
        }
    }

    private fun show() {
        setContentView(R.layout.activity_login)

        mUsernameView = findViewById(R.id.username)
        mPasswordView = findViewById(R.id.password)
        mErrorMessageView = findViewById(R.id.error_message)
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
        mErrorMessageView.visibility = View.GONE

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

        (application as ChatApplication).login(this, username, password) { throwable ->
            mErrorMessageView.apply {
                text = throwable.message
                visibility = View.VISIBLE
            }
        }
    }
}
