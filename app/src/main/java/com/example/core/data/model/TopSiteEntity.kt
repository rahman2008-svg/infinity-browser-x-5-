package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TopSiteEntity.kt
 * 
 * ইউজার সর্বাধিক ভিজিট করা বা পিন করা ওয়েবসাইটের ডেটা।
 */
@Entity(tableName = "top_sites")
data class TopSiteEntity(
    @PrimaryKey
    val domain: String,
    val title: String,
    val url: String,
    val visitCount: Int = 1,
    val lastVisited: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)
