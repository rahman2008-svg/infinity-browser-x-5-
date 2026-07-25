package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.ReadingListEntity
import kotlinx.coroutines.flow.Flow

/**
 * ReadingListDao.kt
 * 
 * রিডিং লিস্ট এবং অফলাইন ওয়েবসাইটের ডেটাবেস অপারেশন।
 */
@Dao
interface ReadingListDao {
    @Query("SELECT * FROM reading_list ORDER BY savedDate DESC")
    fun getAllItems(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_list WHERE readStatus = :status ORDER BY savedDate DESC")
    fun getItemsByStatus(status: String): Flow<List<ReadingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ReadingListEntity): Long

    @Query("UPDATE reading_list SET readStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE reading_list SET offlineFilePath = :filePath WHERE id = :id")
    suspend fun updateOfflinePath(id: Long, filePath: String?)

    @Delete
    suspend fun deleteItem(item: ReadingListEntity)

    @Query("DELETE FROM reading_list")
    suspend fun clearAll()
}
