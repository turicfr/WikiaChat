package com.oren.wikia_chat

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class ChatApplication : Application() {
    lateinit var client: Client
    lateinit var sharedPref: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    }
}
