package com.oren.wikia_chat

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.ProgressIndicator
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    private lateinit var mUsernameView: TextInputLayout
    private lateinit var mPasswordView: TextInputLayout
    private lateinit var mErrorMessageView: TextView
    private lateinit var mProgressIndicator: ProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!(application as ChatApplication).login(this) { show() }) {
            show()
        }
    }

    private fun show() {
        setContentView(R.layout.activity_login)

        mUsernameView = findViewById(R.id.username)
        mPasswordView = findViewById(R.id.password)
        mErrorMessageView = findViewById(R.id.error_message)
        mProgressIndicator = findViewById(R.id.progress_indicator)
        findViewById<Button>(R.id.sign_in_button).setOnClickListener {
            attemptLogin()
        }

        mPasswordView.editText!!.setOnEditorActionListener { _, actionId, _ ->
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

        if (username.isBlank()) {
            mUsernameView.error = getString(R.string.error_field_required)
            mUsernameView.requestFocus()
            return
        }
        if (password.isBlank()) {
            mPasswordView.error = getString(R.string.error_field_required)
            mPasswordView.requestFocus()
            return
        }

        mProgressIndicator.show()
        (application as ChatApplication).login(this, username, password) { throwable ->
            mErrorMessageView.apply {
                text = throwable.message
                visibility = View.VISIBLE
            }
            mProgressIndicator.hide()
        }
    }
}
