package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.oren.wikia_chat.client.Client

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as ChatApplication).login(this, object : Client.Callback<Unit> {
            override fun onSuccess(value: Unit) {
                finish()
                overridePendingTransition(0, android.R.anim.fade_out)
            }

            override fun onFailure(throwable: Throwable) {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        })
    }
}
