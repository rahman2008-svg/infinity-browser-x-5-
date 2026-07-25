package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.BlockStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * BlockStatsDao.kt
 * 
 * অ্যাড ও ট্র্যাকার ব্লক করার দৈনিক পরিসংখ্যানের ডেটাবেস অপারেশন।
 */
@Dao
interface BlockStatsDao {
    @Query("SELECT * FROM block_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<BlockStatsEntity>>

    @Query("SELECT * FROM block_stats WHERE date = :date LIMIT 1")
    suspend fun getStatsForDate(date: String): BlockStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: BlockStatsEntity)

    @Query("UPDATE block_stats SET blockedCount = blockedCount + 1 WHERE date = :date")
    suspend fun incrementBlockedCount(date: String)
}
