package com.example.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.core.browser.TabSession
import com.example.core.data.model.NoteEntity
import com.example.core.data.model.SiteProfileEntity
import com.example.core.ruleengine.ClipboardSuggestion
import com.example.core.ruleengine.Command
import com.example.core.ruleengine.CommandParser
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.core.data.model.HistoryEntity
import com.example.core.data.model.TopSiteEntity
import com.example.core.ruleengine.SearchEngine
import com.example.ui.theme.FireRed
import com.example.ui.theme.InfinityCyan
import com.example.ui.theme.ShieldBadgeBg
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.ShieldPulseGreen
import com.example.ui.theme.ShieldTextGreen
import com.example.ui.theme.SophisticatedAccent

/**
 * HomeScreen.kt
 * 
 * Infinity Browser X এর প্রধান ইউজার ইন্টারফেস।
 * এখানে স্মার্ট অ্যাড্রেস বার, ওয়েবভিউ রেন্ডারিং এবং হোম ড্যাশবোর্ড রয়েছে।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: BrowserViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val topSites by viewModel.topSites.collectAsState()
    val allHistory by viewModel.allHistory.collectAsState()
    val allBookmarks by viewModel.allBookmarks.collectAsState()
    val currentEngine by viewModel.currentSearchEngine.collectAsState()
    val isSidebarMode by viewModel.isSidebarMode.collectAsState()
    val isDarkTheme by viewModel.isDarkThemeOverride.collectAsState()
    val activeSpace by viewModel.activeSpace.collectAsState()

    val allDownloads by viewModel.allDownloads.collectAsState()
    val allReadingListItems by viewModel.allReadingListItems.collectAsState()
    val isAdBlockEnabled by viewModel.isAdBlockEnabled.collectAsState()
    val allBlockStats by viewModel.allBlockStats.collectAsState()
    val blockedCount = remember(allBlockStats) { allBlockStats.sumOf { it.blockedCount } }

    val allNotes by viewModel.allNotes.collectAsState()
    val allSiteProfiles by viewModel.allSiteProfiles.collectAsState()
    val extractedAccentColor by viewModel.extractedAccentColor.collectAsState()
    val clipboardSuggestion by viewModel.clipboardSuggestion.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val showNotesSheet by viewModel.showNotesSheet.collectAsState()
    val showSiteProfileSheet by viewModel.showSiteProfileSheet.collectAsState()
    val showPrivacyStatsSheet by viewModel.showPrivacyStatsSheet.collectAsState()
    val showAboutSheet by viewModel.showAboutSheet.collectAsState()

    val showTabSwitcher by viewModel.showTabSwitcher.collectAsState()
    val showBookmarksSheet by viewModel.showBookmarksSheet.collectAsState()
    val showHistorySheet by viewModel.showHistorySheet.collectAsState()
    val showSettingsSheet by viewModel.showSettingsSheet.collectAsState()
    val showDownloadsSheet by viewModel.showDownloadsSheet.collectAsState()
    val showReadingListSheet by viewModel.showReadingListSheet.collectAsState()
    val showQRScannerSheet by viewModel.showQRScannerSheet.collectAsState()
    val showQRGeneratorSheet by viewModel.showQRGeneratorSheet.collectAsState()
    val fireAnimationActive by viewModel.fireAnimationActive.collectAsState()
    val pendingDownload by viewModel.pendingDownload.collectAsState()

    var showMenuSheet by remember { mutableStateOf(false) }
    var addressBarText by remember { mutableStateOf("") }
    var isEditingAddress by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val commandSuggestions = remember(addressBarText, isEditingAddress) {
        if (isEditingAddress && addressBarText.startsWith(">")) {
            CommandParser.getSuggestions(addressBarText)
        } else emptyList()
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onSearchOrNavigate(spokenText)
            }
        }
    }

    val startVoiceSearch: () -> Unit = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Infinity X — Speak your search query")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice Search not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Update local text when tab URL changes
    LaunchedEffect(activeTab?.url) {
        if (!isEditingAddress && activeTab?.url != null) {
            addressBarText = if (activeTab!!.url == "about:blank") "" else activeTab!!.url
        }
    }

    Scaffold(
        topBar = {
            BrowserTopBar(
                text = addressBarText,
                isEditing = isEditingAddress,
                onTextChange = { addressBarText = it },
                onFocusChange = { isEditingAddress = it },
                onSearchSubmit = {
                    focusManager.clearFocus()
                    isEditingAddress = false
                    viewModel.onSearchOrNavigate(addressBarText)
                },
                onReloadOrStop = {
                    if (activeTab?.isLoading == true) {
                        viewModel.webViewController.stopLoading()
                    } else {
                        viewModel.webViewController.reload()
                    }
                },
                isLoading = activeTab?.isLoading == true,
                isIncognito = activeTab?.isIncognito == true,
                onOpenMenu = { showMenuSheet = true },
                onStartVoiceSearch = startVoiceSearch,
                activeTabCount = tabs.size,
                accentColor = extractedAccentColor,
                onOpenPrivacyStats = { viewModel.setPrivacyStatsVisible(true) },
                blockedTrackerCount = blockedCount
            )
        },
        bottomBar = {
            if (!isSidebarMode) {
                BrowserBottomBar(
                    canGoBack = activeTab?.canGoBack == true,
                    canGoForward = activeTab?.canGoForward == true,
                    isBookmarked = allBookmarks.any { it.url == activeTab?.url },
                    tabCount = tabs.size,
                    onBack = { viewModel.webViewController.goBack() },
                    onForward = { viewModel.webViewController.goForward() },
                    onHome = { viewModel.loadUrlInActiveTab("about:blank") },
                    onToggleBookmark = { viewModel.toggleBookmarkForActiveTab() },
                    onOpenTabSwitcher = { viewModel.setTabSwitcherVisible(true) },
                    onOpenBookmarks = { viewModel.setBookmarksVisible(true) },
                    onOpenHistory = { viewModel.setHistoryVisible(true) }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSidebarMode) {
                ArcVerticalSidebar(
                    tabs = tabs,
                    activeTabId = activeTab?.id,
                    spaces = spaces,
                    activeSpace = activeSpace,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onNewTab = { viewModel.openNewTab("about:blank", "New Tab", false) },
                    onSwitchSpace = { viewModel.switchSpace(it) },
                    onOpenBookmarks = { viewModel.setBookmarksVisible(true) },
                    onOpenNotes = { viewModel.setNotesVisible(true) },
                    onOpenSettings = { viewModel.setSettingsVisible(true) },
                    onTriggerFire = { viewModel.onTriggerFireButton() }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val isStartPage = activeTab?.url == "about:blank" || activeTab?.url?.isEmpty() == true

                if (isStartPage) {
                    StartPageDashboard(
                        topSites = topSites,
                        currentEngine = currentEngine,
                        allNotes = allNotes,
                        onOpenUrl = { url -> viewModel.loadUrlInActiveTab(url) },
                        onSearchSubmit = { query -> viewModel.onSearchOrNavigate(query) },
                        onOpenBookmarks = { viewModel.setBookmarksVisible(true) },
                        onOpenHistory = { viewModel.setHistoryVisible(true) },
                        onOpenIncognito = { viewModel.openNewTab("about:blank", "Incognito", true) },
                        onStartVoiceSearch = startVoiceSearch,
                        onOpenQRScanner = { viewModel.setQRScannerVisible(true) },
                        onOpenNotes = { viewModel.setNotesVisible(true) },
                        onOpenPrivacyStats = { viewModel.setPrivacyStatsVisible(true) },
                        clipboardSuggestion = clipboardSuggestion,
                        onDismissClipboard = { viewModel.dismissClipboardSuggestion() }
                    )
                } else {
                    // WebView Rendering Container
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeTab?.isLoading == true) {
                            LinearProgressIndicator(
                                progress = { (activeTab?.progress ?: 0) / 100f },
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = extractedAccentColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                            )
                        }

                        AndroidView(
                            factory = { context ->
                                viewModel.webViewController.getOrCreateWebView()
                            },
                            update = { webView ->
                                activeTab?.let { tab ->
                                    if (tab.url != "about:blank" && webView.url != tab.url) {
                                        viewModel.webViewController.bindAndLoad(tab.id, tab.url)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (commandSuggestions.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                        CommandPaletteOverlay(
                            suggestions = commandSuggestions,
                            onSelectCommand = { cmd ->
                                focusManager.clearFocus()
                                isEditingAddress = false
                                viewModel.executeCommand(cmd)
                            }
                        )
                    }
                }

                // Fire Button Animation Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = fireAnimationActive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Fire Cleared",
                                modifier = Modifier.size(96.dp),
                                tint = FireRed
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "সব সেশন ও ডেটা মুছে ফেলা হয়েছে 🔥",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ফ্রেশ ব্রাউজিং সেশন প্রস্তুত",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheets Display
    if (showTabSwitcher) {
        TabSwitcherSheet(
            tabs = tabs,
            activeTabId = activeTab?.id,
            activeSpace = activeSpace,
            onSelectTab = { viewModel.selectTab(it) },
            onCloseTab = { viewModel.closeTab(it) },
            onNewTab = { isInc -> viewModel.openNewTab("about:blank", "New Tab", isInc) },
            onSwitchSpace = { viewModel.switchSpace(it) },
            onFireButton = { viewModel.onTriggerFireButton() },
            onDismiss = { viewModel.setTabSwitcherVisible(false) }
        )
    }

    if (showBookmarksSheet) {
        BookmarksSheet(
            bookmarks = allBookmarks,
            onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
            onDeleteBookmark = { viewModel.deleteBookmark(it) },
            onDismiss = { viewModel.setBookmarksVisible(false) }
        )
    }

    if (showHistorySheet) {
        HistorySheet(
            history = allHistory,
            onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
            onDeleteItem = { viewModel.deleteHistoryItem(it) },
            onClearAll = { viewModel.clearHistory() },
            onDismiss = { viewModel.setHistoryVisible(false) }
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            currentEngine = currentEngine,
            isSidebarMode = isSidebarMode,
            isDarkTheme = isDarkTheme,
            isAdBlockEnabled = isAdBlockEnabled,
            blockedCount = blockedCount,
            onSelectEngine = { viewModel.setSearchEngine(it) },
            onToggleSidebar = { viewModel.toggleSidebarMode() },
            onSelectTheme = { viewModel.setThemeOverride(it) },
            onToggleAdBlock = { viewModel.toggleAdBlock() },
            onTriggerFireButton = { viewModel.onTriggerFireButton() },
            onOpenAbout = { viewModel.setAboutVisible(true) },
            onDismiss = { viewModel.setSettingsVisible(false) }
        )
    }

    if (showMenuSheet) {
        BrowserMenuSheet(
            downloadsCount = allDownloads.size,
            readingListCount = allReadingListItems.size,
            isIncognitoTab = activeTab?.isIncognito == true,
            onNewTab = { viewModel.openNewTab("about:blank", "New Tab", false) },
            onNewIncognitoTab = { viewModel.openNewTab("about:blank", "Incognito", true) },
            onOpenBookmarks = { viewModel.setBookmarksVisible(true) },
            onOpenHistory = { viewModel.setHistoryVisible(true) },
            onOpenDownloads = { viewModel.setDownloadsVisible(true) },
            onOpenReadingList = { viewModel.setReadingListVisible(true) },
            notesCount = allNotes.size,
            onOpenNotes = { viewModel.setNotesVisible(true) },
            onOpenSiteProfile = { viewModel.setSiteProfileVisible(true) },
            onSaveOffline = {
                viewModel.addToReadingListCurrentTab()
                Toast.makeText(context, "Saved to Reading List & Offline Archive!", Toast.LENGTH_SHORT).show()
            },
            onScanQR = { viewModel.setQRScannerVisible(true) },
            onGenerateQR = { viewModel.setQRGeneratorVisible(true) },
            onOpenSettings = { viewModel.setSettingsVisible(true) },
            onOpenAbout = { viewModel.setAboutVisible(true) },
            onFireClean = { viewModel.onTriggerFireButton() },
            onDismiss = { showMenuSheet = false }
        )
    }

    if (showDownloadsSheet) {
        DownloadsSheet(
            downloads = allDownloads,
            onDismiss = { viewModel.setDownloadsVisible(false) },
            onDelete = { viewModel.deleteDownload(it) },
            onClearAll = { viewModel.clearDownloads() }
        )
    }

    if (showReadingListSheet) {
        ReadingListSheet(
            items = allReadingListItems,
            onDismiss = { viewModel.setReadingListVisible(false) },
            onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
            onToggleStatus = { viewModel.toggleReadingStatus(it) },
            onDelete = { viewModel.deleteReadingListItem(it) },
            onClearAll = { viewModel.clearReadingList() }
        )
    }

    if (showQRScannerSheet) {
        QRScannerSheet(
            onDismiss = { viewModel.setQRScannerVisible(false) },
            onOpenUrl = { viewModel.loadUrlInActiveTab(it) },
            onSwitchToGenerator = {
                viewModel.setQRScannerVisible(false)
                viewModel.setQRGeneratorVisible(true)
            }
        )
    }

    if (showQRGeneratorSheet) {
        QRGeneratorSheet(
            initialUrl = activeTab?.url ?: "https://google.com",
            onDismiss = { viewModel.setQRGeneratorVisible(false) },
            onSwitchToScanner = {
                viewModel.setQRGeneratorVisible(false)
                viewModel.setQRScannerVisible(true)
            }
        )
    }

    if (showNotesSheet) {
        NotesSheet(
            allNotes = allNotes,
            onSaveNote = { viewModel.saveNote(it) },
            onDeleteNote = { viewModel.deleteNote(it) },
            onClearAll = { viewModel.clearAllNotes() },
            onDismiss = { viewModel.setNotesVisible(false) }
        )
    }

    if (showSiteProfileSheet) {
        SiteProfileSheet(
            currentUrl = activeTab?.url,
            allProfiles = allSiteProfiles,
            onSaveProfile = { viewModel.saveSiteProfile(it) },
            onDeleteProfile = { viewModel.deleteSiteProfile(it) },
            onDismiss = { viewModel.setSiteProfileVisible(false) }
        )
    }

    if (showPrivacyStatsSheet) {
        PrivacyStatsSheet(
            blockStats = allBlockStats,
            tabs = tabs,
            onDismiss = { viewModel.setPrivacyStatsVisible(false) }
        )
    }

    if (showAboutSheet) {
        AboutSheet(
            onDismiss = { viewModel.setAboutVisible(false) }
        )
    }

    if (pendingDownload != null) {
        val req = pendingDownload!!
        val sizeStr = remember(req.sizeBytes) { if (req.sizeBytes > 0) "${req.sizeBytes / 1024} KB" else "Unknown Size" }
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingDownload() },
            icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Download File?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("File: ${req.fileName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Type: ${req.mimeType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Size: $sizeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmDownload(req) }) {
                    Text("Start Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPendingDownload() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BrowserTopBar(
    text: String,
    isEditing: Boolean,
    onTextChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onReloadOrStop: () -> Unit,
    isLoading: Boolean,
    isIncognito: Boolean,
    onOpenMenu: () -> Unit,
    onStartVoiceSearch: () -> Unit,
    activeTabCount: Int,
    accentColor: Int? = null,
    onOpenPrivacyStats: () -> Unit = {},
    blockedTrackerCount: Int = 0
) {
    Surface(
        color = if (isIncognito) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sophisticated Dark Address Bar Container
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("address_bar_input"),
                    placeholder = {
                        Text(
                            text = if (isIncognito) "ইনকগনিটো সার্চ বা URL লিখুন..." else "Search or type URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isIncognito) Icons.Default.Lock else Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isIncognito) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (isLoading) onReloadOrStop() else onTextChange("")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Clear,
                                        contentDescription = if (isLoading) "Stop" else "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (isLoading) {
                                IconButton(onClick = onReloadOrStop, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                }
                            } else {
                                IconButton(onClick = onStartVoiceSearch, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Search", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSearchSubmit() }),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (blockedTrackerCount > 0) {
                Surface(
                    onClick = onOpenPrivacyStats,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$blockedTrackerCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Menu Button
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("menu_button")
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.testTag("back_button")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.testTag("forward_button")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Sophisticated Dark Center Home Button
            Surface(
                onClick = onHome,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("home_button"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(onClick = onToggleBookmark, modifier = Modifier.testTag("bookmark_button")) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Tab Switcher Badge Button
            Surface(
                onClick = onOpenTabSwitcher,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("tab_switcher_button"),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$tabCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun StartPageDashboard(
    topSites: List<TopSiteEntity>,
    currentEngine: SearchEngine,
    allNotes: List<NoteEntity> = emptyList(),
    onOpenUrl: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenIncognito: () -> Unit,
    onStartVoiceSearch: () -> Unit,
    onOpenQRScanner: () -> Unit,
    onOpenNotes: () -> Unit = {},
    onOpenPrivacyStats: () -> Unit = {},
    clipboardSuggestion: ClipboardSuggestion? = null,
    onDismissClipboard: () -> Unit = {}
) {
    var queryText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative Background Accent
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .blur(100.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), shape = CircleShape)
        )

        // Privacy Indicator Badge (Top Right)
        Surface(
            onClick = onOpenPrivacyStats,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = ShieldBadgeBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ShieldPulseGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Shield Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldTextGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Sophisticated Dark Brand Logo Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(76.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(3.5.dp, MaterialTheme.colorScheme.onPrimary)
                        ) {}
                    }
                }

                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("Infinity ") }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("X") }
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "FAST • PRIVATE • POWERFUL",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Search Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Search (${currentEngine.displayName}) or type alias:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input"),
                        placeholder = { 
                            Text(
                                "e.g. yt, fb, gh, or any topic...", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ) 
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onStartVoiceSearch) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = onOpenQRScanner) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (queryText.isNotBlank()) onSearchSubmit(queryText)
                        }),
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (clipboardSuggestion != null) {
                ClipboardSuggestionCard(
                    suggestion = clipboardSuggestion,
                    onExecute = { url -> onOpenUrl(url) },
                    onDismiss = onDismissClipboard
                )
            }

            WeatherWidgetCard()

            NotesPreviewWidget(
                notes = allNotes,
                onOpenAllNotes = onOpenNotes,
                onNoteClick = { onOpenNotes() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Shortcuts Grid (Smart Aliases)
            Text(
                text = "Quick Links",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            val shortcuts = listOf(
                "g" to "Google",
                "yt" to "YouTube",
                "fb" to "Facebook",
                "gh" to "GitHub",
                "rd" to "Reddit",
                "w" to "Wikipedia",
                "ddg" to "DuckDuckGo",
                "+" to "Add"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                items(shortcuts) { (alias, name) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { if (alias != "+") onSearchSubmit(alias) else onOpenBookmarks() }
                            .testTag("shortcut_$alias")
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (alias == "+") Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (alias == "+") BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (alias == "+") "+" else alias.take(2).uppercase(),
                                    style = if (alias == "+") MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alias == "+") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Top Visited Sites
            if (topSites.isNotEmpty()) {
                Text(
                    text = "Top Sites",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(topSites) { site ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUrl(site.url) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = site.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = site.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Access Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenBookmarks() }
                        .testTag("open_bookmarks_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Bookmarks", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenHistory() }
                        .testTag("open_history_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("History", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
