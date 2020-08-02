package com.oren.wikia_chat

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.oren.wikia_chat.client.Client

class ChatApplication : Application() {
    private companion object {
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    private lateinit var sharedPref: SharedPreferences
    lateinit var client: Client

    override fun onCreate() {
        super.onCreate()
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun login(context: Context, onFailure: (throwable: Throwable) -> Unit): Boolean {
        val username = sharedPref.getString(USERNAME_KEY, null)
        val password = sharedPref.getString(PASSWORD_KEY, null)
        if (username == null || password == null) {
            return false
        }
        login(context, username, password, onFailure)
        return true
    }

    fun login(
        context: Context,
        username: String,
        password: String,
        onFailure: (throwable: Throwable) -> Unit
    ) {
        client = Client()
        client.login(username, password, object : Client.Callback<Unit> {
            override fun onSuccess(value: Unit) {
                with(sharedPref.edit()) {
                    putString(USERNAME_KEY, username)
                    putString(PASSWORD_KEY, password)
                    apply()
                }
                context.startActivity(Intent(context, ChatSelectionActivity::class.java))
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("ChatApplication", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
                onFailure(throwable)
            }
        })
    }

    fun logout(context: Context) {
        with(sharedPref.edit()) {
            remove(USERNAME_KEY)
            remove(PASSWORD_KEY)
            apply()
        }
        client.logout()
        context.startActivity(Intent(context, LoginActivity::class.java))
    }
}
