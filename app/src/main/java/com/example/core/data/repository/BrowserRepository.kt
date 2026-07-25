package com.example.core.data.repository

import com.example.core.data.dao.*
import com.example.core.data.model.*
import kotlinx.coroutines.flow.Flow
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BrowserRepository.kt
 * 
 * Single Source of Truth (SSOT)। ViewModel বা UseCase সরাসরি DAO বা Room অ্যাকসেস না করে এই রিপোজিটরি ব্যবহার করে।
 */
class BrowserRepository(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val tabSessionDao: TabSessionDao,
    private val blockStatsDao: BlockStatsDao,
    private val downloadDao: DownloadDao,
    private val topSiteDao: TopSiteDao,
    private val readingListDao: ReadingListDao,
    private val siteProfileDao: SiteProfileDao,
    private val noteDao: NoteDao
) {
    // --- History & Top Sites ---
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val topSites: Flow<List<TopSiteEntity>> = topSiteDao.getTopSites()

    suspend fun recordVisit(url: String, title: String, faviconUrl: String? = null) {
        if (url.startsWith("about:") || url.startsWith("chrome:") || url.isBlank()) return
        
        val existing = historyDao.getHistoryByUrl(url)
        if (existing != null) {
            historyDao.insertOrUpdate(
                existing.copy(
                    title = title.ifBlank { existing.title },
                    timestamp = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1,
                    faviconUrl = faviconUrl ?: existing.faviconUrl
                )
            )
        } else {
            historyDao.insertOrUpdate(
                HistoryEntity(
                    url = url,
                    title = title.ifBlank { url },
                    timestamp = System.currentTimeMillis(),
                    faviconUrl = faviconUrl,
                    visitCount = 1
                )
            )
        }

        // Update TopSites
        val domain = extractDomain(url)
        if (domain.isNotBlank()) {
            val existingTop = topSiteDao.getSiteByDomain(domain)
            if (existingTop != null) {
                topSiteDao.incrementVisit(domain, title.ifBlank { existingTop.title }, url)
            } else {
                topSiteDao.insertOrUpdateSite(
                    TopSiteEntity(
                        domain = domain,
                        title = title.ifBlank { domain },
                        url = url,
                        visitCount = 1,
                        lastVisited = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return ""
            host.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun deleteHistoryItem(id: Long) = historyDao.deleteById(id)
    suspend fun clearAllHistory() {
        historyDao.clearAllHistory()
    }

    // --- Top Sites Management ---
    suspend fun togglePinSite(domain: String, isPinned: Boolean) {
        topSiteDao.setPinned(domain, isPinned)
    }

    suspend fun removeTopSite(domain: String) {
        topSiteDao.deleteByDomain(domain)
    }

    // --- Bookmarks ---
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    suspend fun toggleBookmark(url: String, title: String, faviconUrl: String? = null): Boolean {
        val existing = bookmarkDao.getBookmarkByUrl(url)
        return if (existing != null) {
            bookmarkDao.deleteByUrl(url)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    url = url,
                    title = title.ifBlank { url },
                    faviconUrl = faviconUrl
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteById(id)

    // --- Tab Sessions ---
    val allTabs: Flow<List<TabSessionEntity>> = tabSessionDao.getAllTabs()

    suspend fun saveTabSession(tab: TabSessionEntity) = tabSessionDao.insertOrUpdateTab(tab)
    suspend fun saveAllTabs(tabs: List<TabSessionEntity>) = tabSessionDao.insertAllTabs(tabs)
    suspend fun deleteTabSession(id: String) = tabSessionDao.deleteTabById(id)
    suspend fun clearAllTabSessions() = tabSessionDao.clearAllTabs()

    // --- Ad & Tracker Block Stats ---
    val allBlockStats: Flow<List<BlockStatsEntity>> = blockStatsDao.getAllStats()

    suspend fun recordBlockedRequest() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = blockStatsDao.getStatsForDate(dateStr)
        if (existing != null) {
            blockStatsDao.incrementBlockedCount(dateStr)
        } else {
            blockStatsDao.insertOrUpdate(BlockStatsEntity(date = dateStr, blockedCount = 1))
        }
    }

    // --- Downloads ---
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun addDownload(download: DownloadEntity): Long {
        return downloadDao.insertDownload(download)
    }

    suspend fun updateDownloadProgress(id: Long, status: String, progress: Int, savedPath: String? = null) {
        downloadDao.updateProgressAndStatus(id, status, progress, savedPath)
    }

    suspend fun deleteDownload(download: DownloadEntity) = downloadDao.deleteDownload(download)
    suspend fun clearAllDownloads() = downloadDao.clearAllDownloads()

    // --- Reading List & Offline Saving ---
    val allReadingListItems: Flow<List<ReadingListEntity>> = readingListDao.getAllItems()

    suspend fun addToReadingList(url: String, title: String, description: String = "", thumbnailUrl: String? = null): Long {
        val wordCountEstimate = title.length * 10 // Quick approximation if text not extracted
        val readTime = (wordCountEstimate / 200).coerceAtLeast(1)
        return readingListDao.insertItem(
            ReadingListEntity(
                url = url,
                title = title.ifBlank { url },
                description = description,
                thumbnailUrl = thumbnailUrl,
                estimatedReadTime = readTime
            )
        )
    }

    suspend fun updateReadingStatus(id: Long, status: String) {
        readingListDao.updateStatus(id, status)
    }

    suspend fun updateOfflineFilePath(id: Long, filePath: String?) {
        readingListDao.updateOfflinePath(id, filePath)
    }

    suspend fun deleteReadingListItem(item: ReadingListEntity) = readingListDao.deleteItem(item)
    suspend fun clearReadingList() = readingListDao.clearAll()

    // --- Site Profiles ---
    val allSiteProfiles: Flow<List<SiteProfileEntity>> = siteProfileDao.getAllProfiles()

    suspend fun getSiteProfile(domain: String): SiteProfileEntity? {
        if (domain.isBlank()) return null
        return siteProfileDao.getProfileByDomain(domain)
    }

    suspend fun saveSiteProfile(profile: SiteProfileEntity) = siteProfileDao.insertProfile(profile)
    suspend fun deleteSiteProfile(domain: String) = siteProfileDao.deleteByDomain(domain)

    // --- Notes ---
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val pinnedNotes: Flow<List<NoteEntity>> = noteDao.getPinnedNotes()

    suspend fun saveNote(note: NoteEntity) = noteDao.insertNote(note)
    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)
    suspend fun clearAllNotes() = noteDao.clearAllNotes()
}
