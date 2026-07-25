package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/**
 * BookmarkDao.kt
 * 
 * বুকমার্ক ডেটাবেস পরিচালনা করার জন্য DAO।
 */
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY isPinned DESC, timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folder = :folder ORDER BY timestamp DESC")
    fun getBookmarksByFolder(folder: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()
}
