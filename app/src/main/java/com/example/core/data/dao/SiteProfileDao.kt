package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.SiteProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * SiteProfileDao.kt
 * 
 * পার-সাইট কাস্টম সেটিংস ডেটাবেস পরিচালনা করার জন্য DAO।
 */
@Dao
interface SiteProfileDao {
    @Query("SELECT * FROM site_profiles ORDER BY domain ASC")
    fun getAllProfiles(): Flow<List<SiteProfileEntity>>

    @Query("SELECT * FROM site_profiles WHERE domain = :domain LIMIT 1")
    suspend fun getProfileByDomain(domain: String): SiteProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SiteProfileEntity)

    @Query("DELETE FROM site_profiles WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)
}
