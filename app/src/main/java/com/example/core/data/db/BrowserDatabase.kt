package com.example.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.data.dao.BlockStatsDao
import com.example.core.data.dao.BookmarkDao
import com.example.core.data.dao.DownloadDao
import com.example.core.data.dao.HistoryDao
import com.example.core.data.dao.NoteDao
import com.example.core.data.dao.ReadingListDao
import com.example.core.data.dao.SiteProfileDao
import com.example.core.data.dao.TabSessionDao
import com.example.core.data.dao.TopSiteDao
import com.example.core.data.model.BlockStatsEntity
import com.example.core.data.model.BookmarkEntity
import com.example.core.data.model.DownloadEntity
import com.example.core.data.model.HistoryEntity
import com.example.core.data.model.NoteEntity
import com.example.core.data.model.ReadingListEntity
import com.example.core.data.model.SiteProfileEntity
import com.example.core.data.model.TabSessionEntity
import com.example.core.data.model.TopSiteEntity

/**
 * BrowserDatabase.kt
 * 
 * Room ডেটাবেস হোল্ডার। অ্যাপ্লিকেশনের সব লোকাল ডেটা এখানে পরিচালিত হয়।
 */
@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        TabSessionEntity::class,
        BlockStatsEntity::class,
        DownloadEntity::class,
        TopSiteEntity::class,
        ReadingListEntity::class,
        SiteProfileEntity::class,
        NoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tabSessionDao(): TabSessionDao
    abstract fun blockStatsDao(): BlockStatsDao
    abstract fun downloadDao(): DownloadDao
    abstract fun topSiteDao(): TopSiteDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun siteProfileDao(): SiteProfileDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "infinity_browser_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
