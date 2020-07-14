package com.oren.wikia_chat

import android.net.Uri

class User(val name: String , avatarSrc: String) {
    val avatarUri: Uri

    init {
        val avatarUri = Uri.parse(avatarSrc)
        val segments = avatarUri.pathSegments.slice(0 until avatarUri.pathSegments.size - 2)
        this.avatarUri = avatarUri.buildUpon().path(segments.joinToString("/")).build()
    }
}
