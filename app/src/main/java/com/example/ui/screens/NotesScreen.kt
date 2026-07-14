package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Note
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel
) {
    val notesList by viewModel.notes.collectAsState()

    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }

    var editorTabState by remember { mutableStateOf(0) } // 0: Write, 1: Markdown Preview

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("notes_screen")
    ) {
        // Search notes header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Markdown Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            IconButton(
                onClick = {
                    editingNote = null
                    noteTitleInput = "Untitled Note"
                    noteContentInput = ""
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Add, "New Note")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notesList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No notes found. Create your first markdown note above!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notesList) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingNote = note
                                noteTitleInput = note.title
                                noteContentInput = note.content
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.modifiedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteNote(note) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }

        // --- Rich Note editor Dialog with custom Markdown Parser! ---
        if (noteTitleInput.isNotEmpty() || editingNote != null) {
            Dialog(onDismissRequest = {
                noteTitleInput = ""
                noteContentInput = ""
                editingNote = null
            }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title header editing
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = noteTitleInput,
                                onValueChange = { noteTitleInput = it },
                                placeholder = { Text("Note Title") },
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(onClick = {
                                noteTitleInput = ""
                                noteContentInput = ""
                                editingNote = null
                            }) {
                                Icon(Icons.Default.Close, "Dismiss")
                            }
                        }

                        // Split tabs (Editor vs Markdown Render)
                        TabRow(selectedTabIndex = editorTabState) {
                            Tab(selected = editorTabState == 0, onClick = { editorTabState = 0 }) {
                                Text("Write", modifier = Modifier.padding(12.dp))
                            }
                            Tab(selected = editorTabState == 1, onClick = { editorTabState = 1 }) {
                                Text("Preview Markdown", modifier = Modifier.padding(12.dp))
                            }
                        }

                        // Workspace Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            if (editorTabState == 0) {
                                // Writing panel
                                OutlinedTextField(
                                    value = noteContentInput,
                                    onValueChange = { noteContentInput = it },
                                    placeholder = { Text("Write content in markdown...") },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else {
                                // Immersive Split-rendered Markdown previewer panel
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    MarkdownPreviewRender(noteContentInput)
                                }
                            }
                        }

                        // Bottom save controls
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (noteTitleInput.isNotBlank()) {
                                        val finalNote = editingNote?.copy(
                                            title = noteTitleInput,
                                            content = noteContentInput,
                                            modifiedAt = System.currentTimeMillis()
                                        ) ?: Note(
                                            id = UUID.randomUUID().toString(),
                                            title = noteTitleInput,
                                            content = noteContentInput,
                                            createdAt = System.currentTimeMillis(),
                                            modifiedAt = System.currentTimeMillis()
                                        )
                                        viewModel.updateNote(finalNote)
                                        noteTitleInput = ""
                                        noteContentInput = ""
                                        editingNote = null
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Note")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Beautiful simple markdown renderer utilizing Jetpack Compose components!
@Composable
fun MarkdownPreviewRender(content: String) {
    val lines = content.split("\n")
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(lines) { line ->
            when {
                // Header 1: # Header
                line.startsWith("# ") -> {
                    Text(
                        text = line.substring(2),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                // Header 2: ## Header
                line.startsWith("## ") -> {
                    Text(
                        text = line.substring(3),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                // Checkbox checked: - [x] task
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckBox, "Checked", tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                        Text(
                            text = line.substring(6),
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                            color = Color.Gray
                        )
                    }
                }
                // Checkbox unchecked: - [ ] task
                line.startsWith("- [ ] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckBoxOutlineBlank, "Unchecked", modifier = Modifier.size(16.dp))
                        Text(
                            text = line.substring(6),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                // Bullet points: - item
                line.startsWith("- ") -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("•", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = line.substring(2), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Normal Body line (renders bold elements like **bold** dynamically!)
                else -> {
                    var text = line
                    var isBold = false
                    if (text.startsWith("**") && text.endsWith("**") && text.length > 4) {
                        text = text.substring(2, text.length - 2)
                        isBold = true
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Normal,
                        fontStyle = if (line.startsWith("*") && line.endsWith("*")) FontStyle.Italic else FontStyle.Normal
                    )
                }
            }
        }
    }
}
