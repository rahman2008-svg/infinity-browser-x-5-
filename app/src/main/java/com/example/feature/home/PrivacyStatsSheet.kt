package com.example.feature.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.browser.TabSession
import com.example.core.data.model.BlockStatsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyStatsSheet(
    blockStats: List<BlockStatsEntity>,
    tabs: List<TabSession>,
    onDismiss: () -> Unit
) {
    var selectedTabIdx by remember { mutableIntStateOf(0) } // 0 = Trackers, 1 = Resources

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Privacy & Resource Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time tracker interception & memory analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTabIdx) {
                Tab(
                    selected = selectedTabIdx == 0,
                    onClick = { selectedTabIdx = 0 },
                    text = { Text("🛡 Blocked Trackers (${blockStats.size})") }
                )
                Tab(
                    selected = selectedTabIdx == 1,
                    onClick = { selectedTabIdx = 1 },
                    text = { Text("⚡ Active Tabs (${tabs.size})") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIdx == 0) {
                if (blockStats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No trackers detected in this browsing session!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val sortedStats = remember(blockStats) {
                        blockStats.sortedByDescending { it.date }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedStats) { entry ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = "Date: ${entry.date}", fontWeight = FontWeight.SemiBold)
                                    }
                                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                        Text("${entry.blockedCount} blocked", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Resources Tab
                val totalEstMB = remember(tabs) { tabs.size * 35 } // rough estimate ~35MB per Chromium tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated Memory Footprint", style = MaterialTheme.typography.labelMedium)
                            Text("~$totalEstMB MB RAM allocated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tabs) { tab ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = tab.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(text = tab.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("~35 MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
