package com.example.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SiteProfileEntity.kt
 * 
 * নির্দিষ্ট ডোমেইনের জন্য পার-সাইট কাস্টম অটোমেশন সেটিংস (Desktop Mode, Dark Mode, JS, Text Zoom ইত্যাদি)।
 */
@Entity(tableName = "site_profiles")
data class SiteProfileEntity(
    @PrimaryKey
    val domain: String,
    val darkModeOverride: Boolean? = null,
    val desktopMode: Boolean = false,
    val jsEnabled: Boolean = true,
    val textZoom: Int = 100,
    val autoTranslate: Boolean = false
)
