package com.oren.wikia_chat

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Wiki::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wikiDao(): WikiDao
}
