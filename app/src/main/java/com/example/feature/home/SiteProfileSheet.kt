package com.example.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.model.SiteProfileEntity
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteProfileSheet(
    currentUrl: String?,
    allProfiles: List<SiteProfileEntity>,
    onSaveProfile: (SiteProfileEntity) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val domain = remember(currentUrl) {
        try {
            if (currentUrl.isNullOrBlank()) "" else URI(currentUrl).host?.removePrefix("www.") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val existingProfile = remember(allProfiles, domain) {
        allProfiles.find { it.domain == domain }
    }

    var desktopMode by remember(existingProfile) { mutableStateOf(existingProfile?.desktopMode ?: false) }
    var jsEnabled by remember(existingProfile) { mutableStateOf(existingProfile?.jsEnabled ?: true) }
    var textZoom by remember(existingProfile) { mutableIntStateOf(existingProfile?.textZoom ?: 100) }
    var autoTranslate by remember(existingProfile) { mutableStateOf(existingProfile?.autoTranslate ?: false) }
    // darkModeOverride: null = default/global, true = force dark, false = force light
    var darkModeOverride by remember(existingProfile) { mutableStateOf(existingProfile?.darkModeOverride) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Site Automation Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (domain.isNotEmpty()) domain else "No website loaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (domain.isEmpty()) {
                Text(
                    text = "Please navigate to a valid website to customize rules for that domain.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                return@Column
            }

            HorizontalDivider()

            // Desktop Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Always Request Desktop Site", fontWeight = FontWeight.SemiBold)
                    Text(text = "Override User-Agent with desktop chrome headers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = desktopMode,
                    onCheckedChange = { desktopMode = it }
                )
            }

            // JavaScript Enable/Disable
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Enable JavaScript", fontWeight = FontWeight.SemiBold)
                    Text(text = "Disable to prevent scripts, popups, and trackings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = jsEnabled,
                    onCheckedChange = { jsEnabled = it }
                )
            }

            // Auto Translate
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Auto Translate Page", fontWeight = FontWeight.SemiBold)
                    Text(text = "Suggest translation for foreign language sites", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = autoTranslate,
                    onCheckedChange = { autoTranslate = it }
                )
            }

            // Dark Mode Override Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Dark Mode Preference", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = darkModeOverride == null,
                        onClick = { darkModeOverride = null },
                        label = { Text("Global Default") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = darkModeOverride == true,
                        onClick = { darkModeOverride = true },
                        label = { Text("Force Dark") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = darkModeOverride == false,
                        onClick = { darkModeOverride = false },
                        label = { Text("Force Light") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Text Zoom Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Text Zoom", fontWeight = FontWeight.SemiBold)
                    Text(text = "$textZoom%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = textZoom.toFloat(),
                    onValueChange = { textZoom = it.toInt() },
                    valueRange = 50f..200f,
                    steps = 14
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existingProfile != null) {
                    OutlinedButton(
                        onClick = {
                            onDeleteProfile(domain)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Rules")
                    }
                }

                Button(
                    onClick = {
                        val newProfile = SiteProfileEntity(
                            domain = domain,
                            darkModeOverride = darkModeOverride,
                            desktopMode = desktopMode,
                            jsEnabled = jsEnabled,
                            textZoom = textZoom,
                            autoTranslate = autoTranslate
                        )
                        onSaveProfile(newProfile)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Profile")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
