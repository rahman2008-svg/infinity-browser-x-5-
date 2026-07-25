package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BookmarkEntity.kt
 * 
 * বুকমার্ক ও রিডিং লিস্ট ডেটাবেস এনটিটি। ফোল্ডার ও পিন স্ট্যাটাস সহ ওয়েবপেজ সংরক্ষণ করে।
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val folder: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val faviconUrl: String? = null
)
