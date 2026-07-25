package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DownloadEntity.kt
 * 
 * ইউজার ডাউনলোড করা বা চলমান ফাইলসমূহের তথ্য।
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val url: String,
    val sizeBytes: Long,
    val mimeType: String,
    val status: String, // PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val savedPath: String? = null,
    val downloadDate: Long = System.currentTimeMillis(),
    val sourceWebsite: String? = null,
    val progress: Int = 0 // 0 to 100
)
