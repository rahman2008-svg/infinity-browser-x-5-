package com.example.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.core.browser.DownloadWorker
import com.example.core.browser.TabSession
import com.example.core.browser.TabSessionManager
import com.example.core.browser.WebViewController
import com.example.core.data.db.BrowserDatabase
import com.example.core.data.model.BlockStatsEntity
import com.example.core.data.model.BookmarkEntity
import com.example.core.data.model.DownloadEntity
import com.example.core.data.model.HistoryEntity
import com.example.core.data.model.NoteEntity
import com.example.core.data.model.ReadingListEntity
import com.example.core.data.model.SiteProfileEntity
import com.example.core.data.model.TopSiteEntity
import com.example.core.data.repository.BrowserRepository
import com.example.core.ruleengine.ClipboardSuggestion
import com.example.core.ruleengine.ClipboardTypeDetector
import com.example.core.ruleengine.Command
import com.example.core.ruleengine.CommandParser
import com.example.core.ruleengine.FilterMatcher
import com.example.core.ruleengine.FireButtonEngine
import com.example.core.ruleengine.SearchEngine
import com.example.core.ruleengine.SearchResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DownloadRequest(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)

/**
 * BrowserViewModel.kt
 * 
 * হোম ড্যাশবোর্ড, ট্যাব ম্যানেজার, Ad-blocker, ডাউনলোড, রিডিং লিস্ট এবং ব্রাউজার কোর ইন্টিগ্রেশনের প্রধান ViewModel।
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = BrowserDatabase.getDatabase(application)
    val repository = BrowserRepository(
        db.historyDao(),
        db.bookmarkDao(),
        db.tabSessionDao(),
        db.blockStatsDao(),
        db.downloadDao(),
        db.topSiteDao(),
        db.readingListDao(),
        db.siteProfileDao(),
        db.noteDao()
    )

    val tabSessionManager = TabSessionManager()

    private val _pendingDownload = MutableStateFlow<DownloadRequest?>(null)
    val pendingDownload: StateFlow<DownloadRequest?> = _pendingDownload.asStateFlow()

    val webViewController = WebViewController(
        application,
        tabSessionManager,
        onRecordHistory = { url, title ->
            viewModelScope.launch {
                repository.recordVisit(url, title)
            }
        },
        onDownloadRequested = { url, _, contentDisposition, mimetype, contentLength ->
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
            _pendingDownload.value = DownloadRequest(url, fileName, mimetype, contentLength)
        },
        onBlockedRequest = {
            viewModelScope.launch {
                repository.recordBlockedRequest()
            }
        }
    ).apply {
        getSiteProfile = { domain -> repository.getSiteProfile(domain) }
        onAccentColorExtracted = { color ->
            if (_isAutoAccentColorEnabled.value) {
                _extractedAccentColor.value = color
            }
        }
    }

    // --- UI State ---
    val tabs: StateFlow<List<TabSession>> = tabSessionManager.tabs
    val activeTabId: StateFlow<String?> = tabSessionManager.activeTabId
    val activeSpace: StateFlow<String> = tabSessionManager.activeSpace
    val spaces: StateFlow<List<String>> = tabSessionManager.spaces

    val activeTab: StateFlow<TabSession?> = combine(tabs, activeTabId) { tabList, id ->
        tabList.find { it.id == id } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tabSessionManager.getActiveTab())

    val topSites: StateFlow<List<TopSiteEntity>> = repository.topSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBlockStats: StateFlow<List<BlockStatsEntity>> = repository.allBlockStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReadingListItems: StateFlow<List<ReadingListEntity>> = repository.allReadingListItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSiteProfiles: StateFlow<List<SiteProfileEntity>> = repository.allSiteProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedNotes: StateFlow<List<NoteEntity>> = repository.pinnedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _extractedAccentColor = MutableStateFlow<Int?>(null)
    val extractedAccentColor: StateFlow<Int?> = _extractedAccentColor.asStateFlow()

    private val _isAutoAccentColorEnabled = MutableStateFlow(true)
    val isAutoAccentColorEnabled: StateFlow<Boolean> = _isAutoAccentColorEnabled.asStateFlow()

    private val _clipboardSuggestion = MutableStateFlow<ClipboardSuggestion?>(null)
    val clipboardSuggestion: StateFlow<ClipboardSuggestion?> = _clipboardSuggestion.asStateFlow()

    private val _isClipboardMonitorEnabled = MutableStateFlow(true)
    val isClipboardMonitorEnabled: StateFlow<Boolean> = _isClipboardMonitorEnabled.asStateFlow()

    private val _isAdBlockEnabled = MutableStateFlow(true)
    val isAdBlockEnabled: StateFlow<Boolean> = _isAdBlockEnabled.asStateFlow()

    private val _currentSearchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val currentSearchEngine: StateFlow<SearchEngine> = _currentSearchEngine.asStateFlow()

    private val _isSidebarMode = MutableStateFlow(false)
    val isSidebarMode: StateFlow<Boolean> = _isSidebarMode.asStateFlow()

    private val _isDarkThemeOverride = MutableStateFlow<Boolean?>(true)
    val isDarkThemeOverride: StateFlow<Boolean?> = _isDarkThemeOverride.asStateFlow()

    // Modals / Sheets State
    private val _showTabSwitcher = MutableStateFlow(false)
    val showTabSwitcher: StateFlow<Boolean> = _showTabSwitcher.asStateFlow()

    private val _showBookmarksSheet = MutableStateFlow(false)
    val showBookmarksSheet: StateFlow<Boolean> = _showBookmarksSheet.asStateFlow()

    private val _showHistorySheet = MutableStateFlow(false)
    val showHistorySheet: StateFlow<Boolean> = _showHistorySheet.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _showDownloadsSheet = MutableStateFlow(false)
    val showDownloadsSheet: StateFlow<Boolean> = _showDownloadsSheet.asStateFlow()

    private val _showReadingListSheet = MutableStateFlow(false)
    val showReadingListSheet: StateFlow<Boolean> = _showReadingListSheet.asStateFlow()

    private val _showQRScannerSheet = MutableStateFlow(false)
    val showQRScannerSheet: StateFlow<Boolean> = _showQRScannerSheet.asStateFlow()

    private val _showQRGeneratorSheet = MutableStateFlow(false)
    val showQRGeneratorSheet: StateFlow<Boolean> = _showQRGeneratorSheet.asStateFlow()

    private val _showNotesSheet = MutableStateFlow(false)
    val showNotesSheet: StateFlow<Boolean> = _showNotesSheet.asStateFlow()

    private val _showSiteProfileSheet = MutableStateFlow(false)
    val showSiteProfileSheet: StateFlow<Boolean> = _showSiteProfileSheet.asStateFlow()

    private val _showPrivacyStatsSheet = MutableStateFlow(false)
    val showPrivacyStatsSheet: StateFlow<Boolean> = _showPrivacyStatsSheet.asStateFlow()

    private val _showAboutSheet = MutableStateFlow(false)
    val showAboutSheet: StateFlow<Boolean> = _showAboutSheet.asStateFlow()

    private val _isVoiceSearching = MutableStateFlow(false)
    val isVoiceSearching: StateFlow<Boolean> = _isVoiceSearching.asStateFlow()

    private val _fireAnimationActive = MutableStateFlow(false)
    val fireAnimationActive: StateFlow<Boolean> = _fireAnimationActive.asStateFlow()

    // --- User Actions ---

    fun onSearchOrNavigate(input: String) {
        val resolved = SearchResolver.resolve(input, _currentSearchEngine.value)
        val targetUrl = resolved.targetUrl
        val current = activeTab.value

        if (current != null) {
            webViewController.bindAndLoad(current.id, targetUrl)
        } else {
            val newTab = tabSessionManager.createNewTab(targetUrl, input)
            webViewController.bindAndLoad(newTab.id, targetUrl)
        }
    }

    fun loadUrlInActiveTab(url: String) {
        val current = activeTab.value
        if (current != null) {
            webViewController.bindAndLoad(current.id, url)
        } else {
            val newTab = tabSessionManager.createNewTab(url, url)
            webViewController.bindAndLoad(newTab.id, url)
        }
    }

    fun openNewTab(url: String = "about:blank", title: String = "New Tab", isIncognito: Boolean = false) {
        val newTab = tabSessionManager.createNewTab(url, title, isIncognito = isIncognito)
        if (url != "about:blank") {
            webViewController.bindAndLoad(newTab.id, url)
        }
        _showTabSwitcher.value = false
    }

    fun selectTab(tabId: String) {
        tabSessionManager.selectTab(tabId)
        val tab = tabs.value.find { it.id == tabId }
        if (tab != null && tab.url != "about:blank") {
            webViewController.bindAndLoad(tab.id, tab.url)
        }
        _showTabSwitcher.value = false
    }

    fun closeTab(tabId: String) {
        tabSessionManager.closeTab(tabId)
        val current = activeTab.value
        if (current != null && current.url != "about:blank") {
            webViewController.bindAndLoad(current.id, current.url)
        }
    }

    fun switchSpace(space: String) {
        tabSessionManager.setSpace(space)
        val current = activeTab.value
        if (current != null && current.url != "about:blank") {
            webViewController.bindAndLoad(current.id, current.url)
        }
    }

    fun addSpace(spaceName: String) {
        tabSessionManager.addSpace(spaceName)
    }

    fun renameSpace(oldName: String, newName: String) {
        tabSessionManager.renameSpace(oldName, newName)
    }

    fun toggleSidebarMode() {
        _isSidebarMode.value = !_isSidebarMode.value
    }

    fun setSearchEngine(engine: SearchEngine) {
        _currentSearchEngine.value = engine
    }

    fun setThemeOverride(isDark: Boolean?) {
        _isDarkThemeOverride.value = isDark
        webViewController.isGlobalDarkTheme = isDark ?: true
    }

    fun toggleAdBlock() {
        val newState = !_isAdBlockEnabled.value
        _isAdBlockEnabled.value = newState
        FilterMatcher.isAdBlockEnabled = newState
    }

    // --- Bookmarks & History ---
    fun toggleBookmarkForActiveTab() {
        val current = activeTab.value ?: return
        if (current.url == "about:blank" || current.url.isEmpty()) return
        viewModelScope.launch {
            repository.toggleBookmark(current.url, current.title, current.faviconUrl)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearAllHistory() }
    }

    // --- Top Sites ---
    fun togglePinSite(domain: String, isPinned: Boolean) {
        viewModelScope.launch { repository.togglePinSite(domain, isPinned) }
    }

    fun removeTopSite(domain: String) {
        viewModelScope.launch { repository.removeTopSite(domain) }
    }

    // --- Downloads ---
    fun confirmDownload(request: DownloadRequest) {
        viewModelScope.launch {
            val downloadEntity = DownloadEntity(
                fileName = request.fileName,
                url = request.url,
                sizeBytes = request.sizeBytes,
                mimeType = request.mimeType,
                status = "PENDING",
                sourceWebsite = activeTab.value?.url
            )
            val id = repository.addDownload(downloadEntity)
            _pendingDownload.value = null

            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_DOWNLOAD_ID to id,
                        DownloadWorker.KEY_URL to request.url,
                        DownloadWorker.KEY_FILE_NAME to request.fileName,
                        DownloadWorker.KEY_MIME_TYPE to request.mimeType
                    )
                )
                .build()
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
        }
    }

    fun cancelPendingDownload() {
        _pendingDownload.value = null
    }

    fun deleteDownload(item: DownloadEntity) {
        viewModelScope.launch {
            repository.deleteDownload(item)
            item.savedPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearDownloads() {
        viewModelScope.launch { repository.clearAllDownloads() }
    }

    // --- Reading List & Offline Saving ---
    fun addToReadingListCurrentTab() {
        val current = activeTab.value ?: return
        if (current.url == "about:blank" || current.url.isEmpty()) return
        viewModelScope.launch {
            val id = repository.addToReadingList(current.url, current.title)
            val dir = File(getApplication<Application>().filesDir, "offline_pages")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "page_${id}.mhtml")
            webViewController.saveOfflineArchive(file.absolutePath) { success ->
                if (success) {
                    viewModelScope.launch {
                        repository.updateOfflineFilePath(id, file.absolutePath)
                    }
                }
            }
        }
    }

    fun toggleReadingStatus(item: ReadingListEntity) {
        val nextStatus = if (item.readStatus == "UNREAD") "COMPLETED" else "UNREAD"
        viewModelScope.launch { repository.updateReadingStatus(item.id, nextStatus) }
    }

    fun deleteReadingListItem(item: ReadingListEntity) {
        viewModelScope.launch {
            repository.deleteReadingListItem(item)
            item.offlineFilePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearReadingList() {
        viewModelScope.launch { repository.clearReadingList() }
    }

    /**
     * DuckDuckGo-inspired Fire Button: এক ট্যাপে সব ট্যাব ও কুকি/সেশন মুছে ফেলা
     */
    fun onTriggerFireButton() {
        viewModelScope.launch {
            _fireAnimationActive.value = true
            FireButtonEngine.clearAllSessionData(webViewController, tabSessionManager)
            repository.clearAllTabSessions()
            
            delay(2000)
            _fireAnimationActive.value = false
            _showSettingsSheet.value = false
            _showTabSwitcher.value = false
        }
    }

    // --- Site Profiles ---
    fun saveSiteProfile(profile: SiteProfileEntity) {
        viewModelScope.launch { repository.saveSiteProfile(profile) }
    }

    fun deleteSiteProfile(domain: String) {
        viewModelScope.launch { repository.deleteSiteProfile(domain) }
    }

    // --- Notes ---
    fun saveNote(note: NoteEntity) {
        viewModelScope.launch { repository.saveNote(note) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun clearAllNotes() {
        viewModelScope.launch { repository.clearAllNotes() }
    }

    // --- Clipboard ---
    fun toggleClipboardMonitor() {
        _isClipboardMonitorEnabled.value = !_isClipboardMonitorEnabled.value
        if (!_isClipboardMonitorEnabled.value) {
            _clipboardSuggestion.value = null
        }
    }

    fun checkClipboard(text: String?) {
        if (_isClipboardMonitorEnabled.value) {
            _clipboardSuggestion.value = ClipboardTypeDetector.detect(text)
        } else {
            _clipboardSuggestion.value = null
        }
    }

    fun dismissClipboardSuggestion() {
        _clipboardSuggestion.value = null
    }

    // --- Accent Color ---
    fun toggleAutoAccentColor() {
        _isAutoAccentColorEnabled.value = !_isAutoAccentColorEnabled.value
        if (!_isAutoAccentColorEnabled.value) {
            _extractedAccentColor.value = null
        }
    }

    // --- Command Palette Execution ---
    fun executeCommand(command: Command) {
        when (command) {
            Command.OpenDownloads -> setDownloadsVisible(true)
            Command.OpenHistory -> setHistoryVisible(true)
            Command.OpenBookmarks -> setBookmarksVisible(true)
            Command.OpenSettings -> setSettingsVisible(true)
            Command.NewTab -> openNewTab()
            Command.ClearCache -> onTriggerFireButton()
            Command.NewIncognitoTab -> openNewTab(isIncognito = true)
            Command.OpenNotes -> setNotesVisible(true)
            Command.OpenReadingList -> setReadingListVisible(true)
        }
    }

    // Modal Togglers
    fun setTabSwitcherVisible(show: Boolean) { _showTabSwitcher.value = show }
    fun setBookmarksVisible(show: Boolean) { _showBookmarksSheet.value = show }
    fun setHistoryVisible(show: Boolean) { _showHistorySheet.value = show }
    fun setSettingsVisible(show: Boolean) { _showSettingsSheet.value = show }
    fun setDownloadsVisible(show: Boolean) { _showDownloadsSheet.value = show }
    fun setReadingListVisible(show: Boolean) { _showReadingListSheet.value = show }
    fun setQRScannerVisible(show: Boolean) { _showQRScannerSheet.value = show }
    fun setQRGeneratorVisible(show: Boolean) { _showQRGeneratorSheet.value = show }
    fun setNotesVisible(show: Boolean) { _showNotesSheet.value = show }
    fun setSiteProfileVisible(show: Boolean) { _showSiteProfileSheet.value = show }
    fun setPrivacyStatsVisible(show: Boolean) { _showPrivacyStatsSheet.value = show }
    fun setAboutVisible(show: Boolean) { _showAboutSheet.value = show }
    fun setVoiceSearchActive(active: Boolean) { _isVoiceSearching.value = active }

    override fun onCleared() {
        super.onCleared()
        webViewController.destroy()
    }
}
