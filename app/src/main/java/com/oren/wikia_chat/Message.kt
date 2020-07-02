package com.oren.wikia_chat

class Message {
    companion object {
        const val TYPE_MESSAGE = 0
        const val TYPE_LOG = 1
        const val TYPE_ACTION = 2
    }

    var type = 0
    lateinit var message: String
    lateinit var username: String

    class Builder(private val mType: Int) {
        private lateinit var mUsername: String
        private lateinit var mMessage: String

        fun username(username: String): Builder {
            mUsername = username
            return this
        }

        fun message(message: String): Builder {
            mMessage = message
            return this
        }

        fun build(): Message {
            return Message().apply {
                type = mType
                username = mUsername
                message = mMessage
            }
        }
    }
}
