package com.skb8.translateservice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Insert
    suspend fun insert(log: TranslationLog): Long

    @Query("SELECT * FROM translation_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TranslationLog>>

    @Query("DELETE FROM translation_log")
    suspend fun clear()
}
