package com.example.feature.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.data.model.ReadingListEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ReadingListSheet.kt
 * 
 * ইউজার সেভ করা রিডিং লিস্ট (Unread/Completed) এবং অফলাইন সেভ করা ওয়েব পেজ (MHTML আর্কাইভ) পড়ার ব্যবস্থা।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListSheet(
    items: List<ReadingListEntity>,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onToggleStatus: (ReadingListEntity) -> Unit,
    onDelete: (ReadingListEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Unread, 1: Completed, 2: All

    val filteredItems = remember(items, selectedTab) {
        when (selectedTab) {
            0 -> items.filter { it.readStatus == "UNREAD" || it.readStatus == "READING" }
            1 -> items.filter { it.readStatus == "COMPLETED" }
            else -> items
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ChromeReaderMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Reading List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Offline capable reading queue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (items.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Unread (${items.count { it.readStatus == "UNREAD" }})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Read (${items.count { it.readStatus == "COMPLETED" }})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("All (${items.size})") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No articles in this section", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap 'Add to Reading List' from any webpage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 450.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ReadingListItemCard(
                            item = item,
                            onOpen = {
                                if (item.offlineFilePath != null && File(item.offlineFilePath).exists()) {
                                    openOfflinePage(context, item.offlineFilePath, item.url, onOpenUrl)
                                } else {
                                    onOpenUrl(item.url)
                                    onDismiss()
                                }
                            },
                            onToggleStatus = { onToggleStatus(item) },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingListItemCard(
    item: ReadingListEntity,
    onOpen: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.savedDate) {
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(item.savedDate))
    }
    val isRead = item.readStatus == "COMPLETED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (item.offlineFilePath != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.offlineFilePath != null) Icons.Default.OfflinePin else Icons.Default.Public,
                        contentDescription = null,
                        tint = if (item.offlineFilePath != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.estimatedReadTime} min read",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    if (item.offlineFilePath != null) {
                        Text("Saved Offline", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleStatus, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isRead) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Read",
                        tint = if (isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun openOfflinePage(context: android.content.Context, filePath: String, liveUrl: String, onOpenUrl: (String) -> Unit) {
    try {
        val file = File(filePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "multipart/related") // MHTML MIME type
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Offline archive not found. Opening live page...", Toast.LENGTH_SHORT).show()
            onOpenUrl(liveUrl)
        }
    } catch (e: Exception) {
        // Fallback to opening the url in internal WebView
        Toast.makeText(context, "Opening live webpage in browser...", Toast.LENGTH_SHORT).show()
        onOpenUrl(liveUrl)
    }
}
