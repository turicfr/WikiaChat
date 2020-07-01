package com.oren.wikia_chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.socket.emitter.Emitter
import org.json.JSONObject

class MainFragment : Fragment() {
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityForResult(Intent(activity, LoginActivity::class.java), 0)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val app = activity?.application as ChatApplication
        val socket = app.client.socket

        socket.on("message", onEvent)
    }

    private val onEvent = Emitter.Listener { args ->
        val data = args[0] as JSONObject
        /*when (data) {
            meta -> {}
            "initial" -> onInitial()
        }
        val username = data.getString("username")
        val message = data.getString("message")
        addMessage(username, message)*/
    }

    private fun onInitial() {
        TODO("Not yet implemented")
    }
}
