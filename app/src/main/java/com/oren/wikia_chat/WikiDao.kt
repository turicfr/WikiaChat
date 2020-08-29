package com.oren.wikia_chat

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WikiDao {
    @Query("SELECT * FROM wikis")
    suspend fun getAll(): List<Wiki>

    @Insert
    suspend fun insert(wiki: Wiki)

    @Delete
    suspend fun delete(wiki: Wiki)

    @Query("DELETE FROM wikis")
    suspend fun deleteAll()
}
