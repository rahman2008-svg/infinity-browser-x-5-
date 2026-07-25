package com.example.core.browser

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AccentColorExtractor.kt
 * 
 * Android Palette API ব্যবহার করে বর্তমান সাইটের favicon Bitmap থেকে dominant ও vibrant 
 * কালার এক্সট্র্যাক্ট করে, যা ব্রাউজার টুলবার বা ট্যাব ইন্ডিকেটরে ডাইনামিক থিমিং হিসেবে প্রয়োগ করা যায়।
 */
object AccentColorExtractor {

    suspend fun extractAccentColor(favicon: Bitmap?): Int? = withContext(Dispatchers.Default) {
        if (favicon == null || favicon.isRecycled) return@withContext null
        try {
            val palette = Palette.from(favicon)
                .maximumColorCount(16)
                .generate()

            // Try vibrant first, then dominant, then muted
            val vibrant = palette.getVibrantColor(0)
            if (vibrant != 0) return@withContext vibrant

            val dominant = palette.getDominantColor(0)
            if (dominant != 0) return@withContext dominant

            val lightVibrant = palette.getLightVibrantColor(0)
            if (lightVibrant != 0) return@withContext lightVibrant

            val darkVibrant = palette.getDarkVibrantColor(0)
            if (darkVibrant != 0) return@withContext darkVibrant

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
