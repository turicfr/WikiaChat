package com.oren.wikia_chat.client

import android.net.Uri

class User(val name: String, val avatarUri: Uri, room: Room? = null) {
    var privateRoom = if (room?.isPrivate == true) room else null

    constructor(name: String, avatarSrc: String, room: Room? = null) : this(
        name,
        Uri.parse(avatarSrc),
        room,
    )

    override fun equals(other: Any?) = this === other || (other is User && name == other.name)
    override fun hashCode() = name.hashCode()
}
