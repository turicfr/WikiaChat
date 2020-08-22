package com.oren.wikia_chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wikis")
data class Wiki(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "wordmark_url") val wordmarkUrl: String,
) {
    override fun toString() = name
}
