package com.example.core.browser

import com.example.core.data.model.TabSessionEntity
import com.example.core.data.repository.BrowserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * TabSessionManager.kt
 * 
 * ব্রাউজারের ট্যাব এবং Arc-inspired Spaces (Work, Personal, Study) পরিচালনা করে।
 * প্রতিটি ট্যাবের আলাদা State (URL, Title, Scroll, Navigation history, Progress) ট্র্যাকিং করে।
 */
data class TabSession(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://www.google.com",
    val title: String = "New Tab",
    val faviconUrl: String? = null,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val spaceCategory: String = "Work",
    val isIncognito: Boolean = false
)

class TabSessionManager {

    private val _tabs = MutableStateFlow<List<TabSession>>(emptyList())
    val tabs: StateFlow<List<TabSession>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _activeSpace = MutableStateFlow("Work")
    val activeSpace: StateFlow<String> = _activeSpace.asStateFlow()

    private val _spaces = MutableStateFlow(listOf("Work", "Personal", "Study"))
    val spaces: StateFlow<List<String>> = _spaces.asStateFlow()

    init {
        // শুরুতে একটি ডিফল্ট ট্যাব তৈরি করা হচ্ছে
        if (_tabs.value.isEmpty()) {
            createNewTab("https://www.google.com", "Google")
        }
    }

    /**
     * নতুন ট্যাব তৈরি করে এবং সেটিকে সক্রিয় (active) করে।
     */
    fun createNewTab(
        url: String = "https://www.google.com",
        title: String = "New Tab",
        space: String = _activeSpace.value,
        isIncognito: Boolean = false
    ): TabSession {
        val newTab = TabSession(
            url = url,
            title = title,
            spaceCategory = space,
            isIncognito = isIncognito
        )
        _tabs.update { current -> current + newTab }
        _activeTabId.value = newTab.id
        return newTab
    }

    fun openNewTab(
        url: String = "https://www.google.com",
        title: String = "New Tab",
        spaceCategory: String = _activeSpace.value,
        isIncognito: Boolean = false
    ): TabSession = createNewTab(url, title, spaceCategory, isIncognito)

    fun addSpace(spaceName: String) {
        if (spaceName.isNotBlank() && !_spaces.value.contains(spaceName)) {
            _spaces.update { it + spaceName.trim() }
        }
    }

    fun renameSpace(oldName: String, newName: String) {
        if (newName.isNotBlank() && !_spaces.value.contains(newName)) {
            _spaces.update { list -> list.map { if (it == oldName) newName.trim() else it } }
            if (_activeSpace.value == oldName) {
                _activeSpace.value = newName.trim()
            }
            _tabs.update { list ->
                list.map { if (it.spaceCategory == oldName) it.copy(spaceCategory = newName.trim()) else it }
            }
        }
    }

    /**
     * নির্দিষ্ট ট্যাব নির্বাচন করে।
     */
    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            // যে স্পেসে এই ট্যাবটি আছে সেই স্পেসে সুইচ করা
            _tabs.value.find { it.id == tabId }?.let { tab ->
                if (_activeSpace.value != tab.spaceCategory) {
                    _activeSpace.value = tab.spaceCategory
                }
            }
        }
    }

    /**
     * ট্যাব বন্ধ করে। যদি সক্রিয় ট্যাব বন্ধ হয়, তবে অন্য ট্যাব নির্বাচন করা হয়।
     */
    fun closeTab(tabId: String) {
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            // শেষ ট্যাব বন্ধ করলে নতুন একটি ফ্রেশ ট্যাব খুলবে
            _tabs.value = emptyList()
            createNewTab("https://www.google.com", "New Tab")
            return
        }

        val wasActive = _activeTabId.value == tabId
        val updated = currentTabs.filterNot { it.id == tabId }
        _tabs.value = updated

        if (wasActive) {
            // একই স্পেসের পরবর্তী বা পূর্ববর্তী ট্যাব নির্বাচন করা
            val sameSpaceTabs = updated.filter { it.spaceCategory == _activeSpace.value }
            val nextTab = sameSpaceTabs.firstOrNull() ?: updated.firstOrNull()
            _activeTabId.value = nextTab?.id
        }
    }

    /**
     * সব ট্যাব বন্ধ করে দেয় (Fire Button বা Clear Session এর জন্য)।
     */
    fun closeAllTabs() {
        _tabs.value = emptyList()
        _activeTabId.value = null
        createNewTab("https://www.google.com", "New Tab")
    }

    /**
     * Arc-Inspired Space পরিবর্তন করে (Work ⇄ Personal ⇄ Study)।
     */
    fun setSpace(space: String) {
        _activeSpace.value = space
        val spaceTabs = _tabs.value.filter { it.spaceCategory == space }
        if (spaceTabs.isNotEmpty()) {
            // এই স্পেসের প্রথম ট্যাবে ফোকাস
            if (spaceTabs.none { it.id == _activeTabId.value }) {
                _activeTabId.value = spaceTabs.first().id
            }
        } else {
            // এই স্পেসে কোনো ট্যাব না থাকলে নতুন একটি খুলবে
            createNewTab("https://www.google.com", "New Tab", space = space)
        }
    }

    /**
     * পেজ লোডিং বা নেভিগেশনের পর ট্যাবের URL এবং Title আপডেট করে।
     */
    fun updateTabInfo(tabId: String, url: String, title: String, faviconUrl: String? = null) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = url.ifBlank { tab.url },
                        title = title.ifBlank { tab.title },
                        faviconUrl = faviconUrl ?: tab.faviconUrl
                    )
                } else tab
            }
        }
    }

    /**
     * পেজ লোড প্রোগ্রেস এবং Back/Forward সক্ষমতা আপডেট করে।
     */
    fun updateTabProgress(tabId: String, progress: Int, canGoBack: Boolean, canGoForward: Boolean) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        progress = progress,
                        isLoading = progress < 100,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward
                    )
                } else tab
            }
        }
    }

    /**
     * বর্তমান সক্রিয় ট্যাব প্রদান করে।
     */
    fun getActiveTab(): TabSession? {
        val currentId = _activeTabId.value ?: return _tabs.value.firstOrNull()
        return _tabs.value.find { it.id == currentId } ?: _tabs.value.firstOrNull()
    }

    /**
     * Room ডেটাবেসে ট্যাবগুলো সংরক্ষণ করে।
     */
    suspend fun persistToDatabase(repository: BrowserRepository) {
        val entities = _tabs.value.map { tab ->
            TabSessionEntity(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                faviconUrl = tab.faviconUrl,
                spaceCategory = tab.spaceCategory,
                isIncognito = tab.isIncognito,
                isActive = tab.id == _activeTabId.value
            )
        }
        repository.clearAllTabSessions()
        repository.saveAllTabs(entities)
    }
}
