package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * HistoryDao.kt
 * 
 * হিস্টোরি ডেটাবেসের সাথে যোগাযোগের জন্য Data Access Object (DAO)।
 */
@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_items ORDER BY visitCount DESC, timestamp DESC LIMIT :limit")
    fun getTopSites(limit: Int = 8): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_items WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: HistoryEntity)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history_items")
    suspend fun clearAllHistory()
}
