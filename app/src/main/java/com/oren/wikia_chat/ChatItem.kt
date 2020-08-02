package com.oren.wikia_chat

import com.oren.wikia_chat.client.User

abstract class ChatItem(val type: Int) {
    companion object {
        const val TYPE_MESSAGE = 0
        const val TYPE_LOG = 1
    }

    class Message(val user: User, val messages: MutableList<String>) : ChatItem(TYPE_MESSAGE)

    class Log(val message: String) : ChatItem(TYPE_LOG)
}
