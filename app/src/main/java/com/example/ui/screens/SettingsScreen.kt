package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val foldersList by viewModel.folders.collectAsState()

    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var exportedJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("General Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }

        // 1. Theme Configuration Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                            val isSelected = viewModel.darkThemeSetting == mode
                            Button(
                                onClick = { viewModel.updateTheme(mode) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Accent Color Swatches Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Accent Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val accents = listOf(
                            Pair("Crimson", 0xFFE53935.toInt()),
                            Pair("Emerald", 0xFF43A047.toInt()),
                            Pair("Sapphire", 0xFF1E88E5.toInt()),
                            Pair("Amber", 0xFFFFB300.toInt()),
                            Pair("HotPink", 0xFFD81B60.toInt())
                        )

                        accents.forEach { (_, colorHex) ->
                            val isSelected = viewModel.accentColorHex == colorHex
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(colorHex), CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.updateAccentColor(colorHex) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Default Task Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Defaults configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // Default Priority Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Priority", style = MaterialTheme.typography.bodyMedium)
                        var priorityExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { priorityExpanded = true }) {
                                Text(viewModel.defaultPriority, style = MaterialTheme.typography.labelMedium)
                                Icon(Icons.Default.ArrowDropDown, "Open")
                            }
                            DropdownMenu(
                                expanded = priorityExpanded,
                                onDismissRequest = { priorityExpanded = false }
                            ) {
                                listOf("CRITICAL", "HIGH", "MEDIUM", "NORMAL", "LOW").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            viewModel.updateDefaultPriority(p)
                                            priorityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Default Folder Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Workspace", style = MaterialTheme.typography.bodyMedium)
                        var folderExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { folderExpanded = true }) {
                                val currentFolderObj = foldersList.find { it.id == viewModel.defaultFolderId }
                                Text(currentFolderObj?.name ?: "Personal", style = MaterialTheme.typography.labelMedium)
                                Icon(Icons.Default.ArrowDropDown, "Open")
                            }
                            DropdownMenu(
                                expanded = folderExpanded,
                                onDismissRequest = { folderExpanded = false }
                            ) {
                                foldersList.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.name) },
                                        onClick = {
                                            viewModel.updateDefaultFolder(f.id)
                                            folderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Backup & Restore section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Database Backup & Restore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Export your workspaces to JSON or restore past configurations instantly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

                    Button(
                        onClick = {
                            showBackupRestoreDialog = true
                            exportedJsonText = viewModel.exportBackup(context) ?: "Error creating backup"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Backup, "Backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Backup / Restore")
                    }
                }
            }
        }

        // About block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Arbab Tasks Personal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Version 1.0.0 Stable (Offline)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("© 2026 Arbab Inc. All rights reserved.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }

    // --- Backup & Restore Panel Dialog ---
    if (showBackupRestoreDialog) {
        Dialog(onDismissRequest = { showBackupRestoreDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Backup Engine", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showBackupRestoreDialog = false }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pasted JSON Input to Restore", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste valid backup JSON here to restore...") },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    val success = viewModel.importBackup(importJsonText)
                                    if (success) {
                                        Toast.makeText(context, "Database Restored Successfully!", Toast.LENGTH_SHORT).show()
                                        showBackupRestoreDialog = false
                                        importJsonText = ""
                                    } else {
                                        Toast.makeText(context, "Invalid Backup JSON structure", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restore JSON", style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                // Simple copy to clipboard or display
                                importJsonText = exportedJsonText
                                Toast.makeText(context, "Backup copied to input window!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Inspect Backup", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
