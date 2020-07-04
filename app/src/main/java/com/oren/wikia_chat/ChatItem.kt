package com.oren.wikia_chat

abstract class ChatItem(val type: Int) {
    companion object {
        const val TYPE_MESSAGE = 0
        const val TYPE_LOG = 1
    }

    class Message(val username: String, val messages: MutableList<String>, val avatarSrc: String) : ChatItem(TYPE_MESSAGE)

    class Log(val message: String) : ChatItem(TYPE_LOG)
}
