package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.TabSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * TabSessionDao.kt
 * 
 * ট্যাব সেশনগুলো সেভ এবং রিস্টোর করার জন্য DAO।
 */
@Dao
interface TabSessionDao {
    @Query("SELECT * FROM tab_sessions ORDER BY lastAccessed DESC")
    fun getAllTabs(): Flow<List<TabSessionEntity>>

    @Query("SELECT * FROM tab_sessions WHERE spaceCategory = :space ORDER BY lastAccessed DESC")
    fun getTabsBySpace(space: String): Flow<List<TabSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTab(tab: TabSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTabs(tabs: List<TabSessionEntity>)

    @Query("DELETE FROM tab_sessions WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM tab_sessions")
    suspend fun clearAllTabs()
}
