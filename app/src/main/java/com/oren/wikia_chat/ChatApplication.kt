package com.oren.wikia_chat

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.oren.wikia_chat.client.Client
import kotlinx.coroutines.runBlocking

class ChatApplication : Application() {
    private companion object {
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    lateinit var database: AppDatabase
    private lateinit var mSharedPreferences: SharedPreferences
    lateinit var client: Client

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "app").build()
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    fun login(context: Context, callback: Client.Callback<Unit>) {
        val username = mSharedPreferences.getString(USERNAME_KEY, null)
        val password = mSharedPreferences.getString(PASSWORD_KEY, null)
        if (username == null || password == null) {
            callback.onFailure(Exception())
            return
        }
        login(context, username, password, callback)
    }

    fun login(
        context: Context,
        username: String,
        password: String,
        callback: Client.Callback<Unit>,
    ) {
        client = Client()
        client.login(username, password, object : Client.Callback<Unit> {
            override fun onSuccess(value: Unit) {
                mSharedPreferences.edit {
                    putString(USERNAME_KEY, username)
                    putString(PASSWORD_KEY, password)
                }
                context.startActivity(Intent(context, WikiSelectionActivity::class.java))
                callback.onSuccess(Unit)
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("ChatApplication", "Login failed: ${throwable.message}")
                throwable.printStackTrace()
                callback.onFailure(throwable)
            }
        })
    }

    fun logout(context: Context) {
        runBlocking {
            database.wikiDao().deleteAll()
        }
        mSharedPreferences.edit {
            remove(USERNAME_KEY)
            remove(PASSWORD_KEY)
        }
        client.logout()
        context.startActivity(Intent(context, LoginActivity::class.java))
    }
}
