package com.example.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ruleengine.SearchEngine
import com.example.ui.theme.FireRed

/**
 * SettingsSheet.kt
 * 
 * ব্রাউজার সেটিংস: সার্চ ইঞ্জিন পরিবর্তন, থিম টগল, Ad-blocker, সাইডবার মোড এবং Fire Button ট্রিগার।
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentEngine: SearchEngine,
    isSidebarMode: Boolean,
    isDarkTheme: Boolean?,
    isAdBlockEnabled: Boolean,
    blockedCount: Int,
    onSelectEngine: (SearchEngine) -> Unit,
    onToggleSidebar: () -> Unit,
    onSelectTheme: (Boolean?) -> Unit,
    onToggleAdBlock: () -> Unit,
    onTriggerFireButton: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Infinity X সেটিংস",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Ad & Tracker Blocker
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAdBlock() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ad & Tracker Blocker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isAdBlockEnabled) "Active • $blockedCount requests blocked offline" else "Disabled • Ads and scripts allowed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(checked = isAdBlockEnabled, onCheckedChange = { onToggleAdBlock() })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Search Engine
            Text(
                text = "ডিফল্ট সার্চ ইঞ্জিন",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchEngine.entries.forEach { engine ->
                    FilterChip(
                        selected = currentEngine == engine,
                        onClick = { onSelectEngine(engine) },
                        label = { Text(engine.displayName) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 3. Theme Toggle
            Text(
                text = "থিম ও অ্যাপিয়ারেন্স",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isDarkTheme == null,
                    onClick = { onSelectTheme(null) },
                    label = { Text("সিস্টেম ডিফল্ট") }
                )
                FilterChip(
                    selected = isDarkTheme == false,
                    onClick = { onSelectTheme(false) },
                    label = { Text("লাইট মোড ☀️") }
                )
                FilterChip(
                    selected = isDarkTheme == true,
                    onClick = { onSelectTheme(true) },
                    label = { Text("ডার্ক মোড 🌙") }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 4. Sidebar / Spaces Mode Toggle (Arc-inspired)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSidebar() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewSidebar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Arc-স্টাইল সাইডবার মোড", fontWeight = FontWeight.SemiBold)
                        Text("ট্যাব বারের বদলে সাইডবার স্পেস নেভিগেশন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(checked = isSidebarMode, onCheckedChange = { onToggleSidebar() })
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 5. About Developer & Company
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenAbout()
                        onDismiss()
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("About Developer & Company", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Prince AR Abdur Rahman • NexVora Lab's Ofc", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // 6. Fire Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = FireRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fire Button — ওয়ান ট্যাপ ক্লিনআপ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "সব সক্রিয় ট্যাব, কুকিজ এবং ক্যাশ অবিলম্বে মুছে ফেলুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTriggerFireButton,
                        colors = ButtonDefaults.buttonColors(containerColor = FireRed)
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("এখনই সব মুছুন 🔥", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}
