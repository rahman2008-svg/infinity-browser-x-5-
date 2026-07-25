package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ReadingListEntity.kt
 * 
 * ইউজার সেভ করা রিডিং লিস্ট ও অফলাইন এমএইচটিএমএল আর্কাইভ ফাইল।
 */
@Entity(tableName = "reading_list")
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String? = null,
    val estimatedReadTime: Int = 3, // in minutes (~200 words/min)
    val savedDate: Long = System.currentTimeMillis(),
    val readStatus: String = "UNREAD", // UNREAD, READING, COMPLETED
    val offlineFilePath: String? = null
)
