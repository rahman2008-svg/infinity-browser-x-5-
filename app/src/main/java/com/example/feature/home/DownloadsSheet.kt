package com.example.feature.home

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.core.data.model.DownloadEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * DownloadsSheet.kt
 * 
 * ইউজার ডাউনলোড লিস্ট, স্ট্যাটাস অনুযায়ী গ্রুপ করা (Downloading / Completed / Failed) এবং ফাইল অপশন।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    downloads: List<DownloadEntity>,
    onDismiss: () -> Unit,
    onDelete: (DownloadEntity) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Active, 2: Completed

    val filteredDownloads = remember(downloads, selectedTab) {
        when (selectedTab) {
            1 -> downloads.filter { it.status == "DOWNLOADING" || it.status == "PENDING" || it.status == "PAUSED" }
            2 -> downloads.filter { it.status == "COMPLETED" }
            else -> downloads
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
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Downloads Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(text = "${downloads.size} total files", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (downloads.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear List", style = MaterialTheme.typography.labelMedium)
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("All (${downloads.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Active (${downloads.count { it.status == "DOWNLOADING" || it.status == "PENDING" }})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Completed (${downloads.count { it.status == "COMPLETED" }})") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDownloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No download records found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 450.dp)
                ) {
                    items(filteredDownloads, key = { it.id }) { item ->
                        DownloadItemCard(
                            item = item,
                            onOpen = {
                                openDownloadedFile(context, item)
                            },
                            onShare = {
                                shareDownloadedFile(context, item)
                            },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sizeStr = remember(item.sizeBytes) { Formatter.formatFileSize(context, item.sizeBytes) }
    val dateStr = remember(item.downloadDate) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(item.downloadDate))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (item.status == "COMPLETED") onOpen() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = when (item.status) {
                        "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
                        "FAILED" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (item.status) {
                                "COMPLETED" -> Icons.Default.InsertDriveFile
                                "FAILED" -> Icons.Default.ErrorOutline
                                else -> Icons.Default.ArrowDownward
                            },
                            contentDescription = null,
                            tint = when (item.status) {
                                "COMPLETED" -> MaterialTheme.colorScheme.primary
                                "FAILED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (item.sizeBytes > 0) sizeStr else "Unknown size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(" • ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == "COMPLETED") {
                        IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Progress Bar if downloading
            if (item.status == "DOWNLOADING" || item.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { item.progress / 100f },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${item.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (item.status == "FAILED") {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Download failed. Check network connection.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun openDownloadedFile(context: android.content.Context, item: DownloadEntity) {
    val path = item.savedPath ?: return
    try {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File no longer exists on storage", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback without strict provider
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(File(path)), item.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareDownloadedFile(context: android.content.Context, item: DownloadEntity) {
    val path = item.savedPath ?: return
    try {
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File no longer exists", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share file via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show()
    }
}
