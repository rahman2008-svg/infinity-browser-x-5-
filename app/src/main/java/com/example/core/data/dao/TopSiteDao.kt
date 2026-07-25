package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.TopSiteEntity
import kotlinx.coroutines.flow.Flow

/**
 * TopSiteDao.kt
 * 
 * Top Sites ও Pin করার ডেটাবেস অপারেশন।
 */
@Dao
interface TopSiteDao {
    @Query("SELECT * FROM top_sites ORDER BY isPinned DESC, visitCount DESC, lastVisited DESC LIMIT 12")
    fun getTopSites(): Flow<List<TopSiteEntity>>

    @Query("SELECT * FROM top_sites WHERE domain = :domain LIMIT 1")
    suspend fun getSiteByDomain(domain: String): TopSiteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSite(site: TopSiteEntity)

    @Query("UPDATE top_sites SET visitCount = visitCount + 1, lastVisited = :timestamp, title = :title, url = :url WHERE domain = :domain")
    suspend fun incrementVisit(domain: String, title: String, url: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE top_sites SET isPinned = :isPinned WHERE domain = :domain")
    suspend fun setPinned(domain: String, isPinned: Boolean)

    @Delete
    suspend fun deleteSite(site: TopSiteEntity)

    @Query("DELETE FROM top_sites WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)
}
