package com.oren.wikia_chat.client

import android.net.Uri

class User(val name: String, val avatarUri: Uri) {
    constructor(name: String, avatarSrc: String) : this(name, Uri.parse(avatarSrc))

    override fun equals(other: Any?) = this === other || (other is User && name == other.name)
    override fun hashCode() = name.hashCode()
}
