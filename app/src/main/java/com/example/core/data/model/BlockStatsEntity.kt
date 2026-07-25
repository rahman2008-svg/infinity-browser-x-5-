package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BlockStatsEntity.kt
 * 
 * দৈনিক ব্লক হওয়া অ্যাড ও ট্র্যাকারের পরিসংখ্যান সংরক্ষণ করে।
 */
@Entity(tableName = "block_stats")
data class BlockStatsEntity(
    @PrimaryKey
    val date: String, // e.g. "2026-07-24"
    val blockedCount: Int = 0
)
