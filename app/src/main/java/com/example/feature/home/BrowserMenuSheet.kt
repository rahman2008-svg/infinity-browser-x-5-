package com.example.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FireRed

data class MenuActionItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
    val tint: Color? = null,
    val onClick: () -> Unit
)

/**
 * BrowserMenuSheet.kt
 * 
 * ব্রাউজারের সকল টুলস, শর্টকাট, ডাউনলোডস, রিডিং লিস্ট, কিউআর কোড এবং সেটিংসের ওয়ান-স্টপ মেনু।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    downloadsCount: Int,
    readingListCount: Int,
    notesCount: Int,
    isIncognitoTab: Boolean,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenReadingList: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSiteProfile: () -> Unit,
    onSaveOffline: () -> Unit,
    onScanQR: () -> Unit,
    onGenerateQR: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onFireClean: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Infinity X Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val menuItems = listOf(
                MenuActionItem("New Tab", Icons.Default.AddBox, onClick = { onNewTab(); onDismiss() }),
                MenuActionItem("Incognito", Icons.Default.Lock, tint = MaterialTheme.colorScheme.error, onClick = { onNewIncognitoTab(); onDismiss() }),
                MenuActionItem("Bookmarks", Icons.Default.Bookmark, onClick = { onOpenBookmarks(); onDismiss() }),
                MenuActionItem("History", Icons.Default.History, onClick = { onOpenHistory(); onDismiss() }),
                MenuActionItem("Downloads", Icons.Default.Download, badgeCount = if (downloadsCount > 0) downloadsCount else null, onClick = { onOpenDownloads(); onDismiss() }),
                MenuActionItem("Reading List", Icons.Default.ChromeReaderMode, badgeCount = if (readingListCount > 0) readingListCount else null, onClick = { onOpenReadingList(); onDismiss() }),
                MenuActionItem("Notes", Icons.Default.NoteAlt, badgeCount = if (notesCount > 0) notesCount else null, tint = Color(0xFF2196F3), onClick = { onOpenNotes(); onDismiss() }),
                MenuActionItem("Site Profile", Icons.Default.Tune, tint = Color(0xFF9C27B0), onClick = { onOpenSiteProfile(); onDismiss() }),
                MenuActionItem("Save Offline", Icons.Default.OfflinePin, tint = Color(0xFF4CAF50), onClick = { onSaveOffline(); onDismiss() }),
                MenuActionItem("Scan QR", Icons.Default.QrCodeScanner, onClick = { onScanQR(); onDismiss() }),
                MenuActionItem("Share QR", Icons.Default.QrCode2, onClick = { onGenerateQR(); onDismiss() }),
                MenuActionItem("Settings", Icons.Default.Settings, tint = MaterialTheme.colorScheme.primary, onClick = { onOpenSettings(); onDismiss() }),
                MenuActionItem("About Dev", Icons.Default.Info, tint = Color(0xFF00BCD4), onClick = { onOpenAbout(); onDismiss() }),
                MenuActionItem("Fire Clean 🔥", Icons.Default.LocalFireDepartment, tint = FireRed, onClick = { onFireClean(); onDismiss() })
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(menuItems) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { item.onClick() }
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = item.tint?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.tint ?: MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp)
                                )

                                if (item.badgeCount != null) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "${item.badgeCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
