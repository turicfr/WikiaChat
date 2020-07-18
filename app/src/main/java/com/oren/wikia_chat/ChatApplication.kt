package com.oren.wikia_chat

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

class ChatApplication : Application() {
    lateinit var client: Client
    lateinit var sharedPref: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun login(context: Context, username: String, password: String, onFailure: (throwable: Throwable) -> Unit) {
        client = Client(username, password)
        client.login(object : Client.LoginCallback {
            override fun onSuccess() {
                with(sharedPref.edit()) {
                    putString(getString(R.string.username_key), username)
                    putString(getString(R.string.password_key), password)
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
            remove(getString(R.string.username_key))
            remove(getString(R.string.password_key))
            apply()
        }
        client.disconnect()
        context.startActivity(Intent(context, LoginActivity::class.java))
    }
}
