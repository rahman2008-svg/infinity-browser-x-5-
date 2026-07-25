package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.data.model.NoteEntity
import java.text.SimpleDateFormat
import java.util.*

val NOTE_COLORS = listOf(
    0xFFE3F2FD, // Soft Blue
    0xFFE8F5E9, // Soft Green
    0xFFFFF9C4, // Soft Yellow
    0xFFFCE4EC, // Soft Pink
    0xFFF3E5F5, // Soft Purple
    0xFFFFE0B2  // Soft Orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(
    allNotes: List<NoteEntity>,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var currentEditingNote by remember { mutableStateOf<NoteEntity?>(null) }

    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(NOTE_COLORS[0]) }
    var isPinnedInput by remember { mutableStateOf(false) }

    fun startEdit(note: NoteEntity?) {
        currentEditingNote = note
        if (note != null) {
            titleInput = note.title
            descInput = note.description
            selectedColor = note.color
            isPinnedInput = note.isPinned
        } else {
            titleInput = ""
            descInput = ""
            selectedColor = NOTE_COLORS[0]
            isPinnedInput = false
        }
        isEditing = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (isEditing) {
                // Edit / Add View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { isEditing = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = if (currentEditingNote == null) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        if (titleInput.isNotBlank() || descInput.isNotBlank()) {
                            val noteToSave = currentEditingNote?.copy(
                                title = titleInput.ifBlank { "Untitled Note" },
                                description = descInput,
                                color = selectedColor,
                                updatedDate = System.currentTimeMillis(),
                                isPinned = isPinnedInput
                            ) ?: NoteEntity(
                                title = titleInput.ifBlank { "Untitled Note" },
                                description = descInput,
                                color = selectedColor,
                                isPinned = isPinnedInput
                            )
                            onSaveNote(noteToSave)
                        }
                        isEditing = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Note content...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    maxLines = 15
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Picker & Pin Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NOTE_COLORS.forEach { colorVal ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable { selectedColor = colorVal }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == colorVal) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    FilterChip(
                        selected = isPinnedInput,
                        onClick = { isPinnedInput = !isPinnedInput },
                        label = { Text("Pin") },
                        leadingIcon = {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = if (isPinnedInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Notes List View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Integrated Notes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allNotes.size} saved notes & clips",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (allNotes.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { startEdit(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Note")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (allNotes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SpeakerNotesOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No notes found. Create a quick note or save snippets from websites!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                onClick = { startEdit(note) },
                                onDelete = { onDeleteNote(note.id) },
                                onTogglePin = {
                                    onSaveNote(note.copy(isPinned = !note.isPinned, updatedDate = System.currentTimeMillis()))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val dateStr = remember(note.updatedDate) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note.updatedDate))
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(note.color)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = Color.Black.copy(alpha = if (note.isPinned) 1f else 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (note.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.5f)
            )
        }
    }
}
