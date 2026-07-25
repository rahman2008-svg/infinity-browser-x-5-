package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HistoryEntity.kt
 * 
 * ব্রাউজিং হিস্টোরি ডেটাবেস এনটিটি। ব্যবহারকারীর ভিজিট করা প্রতিটি ওয়েবসাইটের তথ্য সংরক্ষণ করে।
 */
@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null,
    val visitCount: Int = 1
)
