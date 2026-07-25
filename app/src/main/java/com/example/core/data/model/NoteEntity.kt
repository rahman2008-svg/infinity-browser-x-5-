package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NoteEntity.kt
 * 
 * ব্রাউজারে ইন্টিগ্রেটেড নোটস উইজেট এবং ফুল নোটস অ্যাপের জন্য ডেটা মডেল।
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val color: Long = 0xFFE3F2FD, // Default soft blue color
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val folder: String? = null,
    val reminderDateTime: Long? = null
)
