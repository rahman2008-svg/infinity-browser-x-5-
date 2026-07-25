package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.browser.TabSession
import com.example.ui.theme.FireRed
import com.example.ui.theme.InfinityCyan

/**
 * TabSwitcherSheet.kt
 * 
 * Arc-inspired Spaces এবং মাল্টি-ট্যাব ম্যানেজমেন্ট UI।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<TabSession>,
    activeTabId: String?,
    activeSpace: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (Boolean) -> Unit, // isIncognito
    onSwitchSpace: (String) -> Unit,
    onFireButton: () -> Unit,
    onDismiss: () -> Unit
) {
    val spaces = listOf("Work", "Personal", "Study")
    var showFireConfirm by remember { mutableStateOf(false) }

    if (showFireConfirm) {
        AlertDialog(
            onDismissRequest = { showFireConfirm = false },
            icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = FireRed) },
            title = { Text("Fire Button — সব সেশন মুছুন?") },
            text = { Text("আপনার সব খোলা ট্যাব, কুকিজ, ক্যাশ ও সেশন ডেটা স্থায়ীভাবে মুছে যাবে এবং একটি নতুন ফ্রেশ সেশন চালু হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        showFireConfirm = false
                        onFireButton()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireRed)
                ) {
                    Text("মুছে ফেলুন 🔥", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFireConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header & Fire Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ট্যাব ও স্পেস ম্যানেজার",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showFireConfirm = true },
                    modifier = Modifier.testTag("fire_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Clear All Session Data",
                        tint = FireRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arc-inspired Spaces Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                spaces.forEach { space ->
                    val isSelected = space == activeSpace
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSwitchSpace(space) },
                        label = { Text(space, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (space == "Work") Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter tabs for active space
            val spaceTabs = tabs.filter { it.spaceCategory == activeSpace }

            if (spaceTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এই স্পেসে কোনো খোলা ট্যাব নেই",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp)
                ) {
                    items(spaceTabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectTab(tab.id) }
                                .testTag("tab_item_${tab.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (tab.isIncognito) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Incognito",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = if (tab.url == "about:blank") "Home" else tab.url.substringBefore("/").substringAfter("://"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onCloseTab(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Tab",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Active Indicator
                                if (isActive) {
                                    Text(
                                        text = "● সক্রিয়",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: New Tab & Incognito
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNewTab(false) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নতুন ট্যাব")
                }

                OutlinedButton(
                    onClick = { onNewTab(true) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ইনকগনিটো", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
