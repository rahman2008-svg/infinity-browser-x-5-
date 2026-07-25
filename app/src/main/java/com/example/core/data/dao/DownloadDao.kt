package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.DownloadEntity
import kotlinx.coroutines.flow.Flow

/**
 * DownloadDao.kt
 * 
 * ডাউনলোড ম্যানেজারের ডেটাবেস অপারেশন।
 */
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadDate DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY downloadDate DESC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, progress = :progress, savedPath = :savedPath WHERE id = :id")
    suspend fun updateProgressAndStatus(id: Long, status: String, progress: Int, savedPath: String? = null)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
