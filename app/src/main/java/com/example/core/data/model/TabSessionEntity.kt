package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TabSessionEntity.kt
 * 
 * সক্রিয় ও স্থগিত (Suspended) ট্যাবগুলোর সেশন স্টেট সংরক্ষণ করার এনটিটি।
 * অ্যাপ বন্ধ করে খুললেও যাতে আগের ট্যাবগুলো এবং তাদের স্ক্রল পজিশন ঠিক থাকে।
 */
@Entity(tableName = "tab_sessions")
data class TabSessionEntity(
    @PrimaryKey
    val id: String, // UUID as string
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val scrollPosition: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis(),
    val spaceCategory: String = "Work", // Arc-inspired spaces: Work, Personal, Study
    val isIncognito: Boolean = false,
    val isActive: Boolean = false
)
