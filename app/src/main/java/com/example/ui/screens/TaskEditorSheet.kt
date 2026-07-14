package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val task = viewModel.editingTask

    // --- State variables ---
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var notes by remember { mutableStateOf(task?.notes ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: viewModel.defaultPriority) }
    var folderId by remember { mutableStateOf(task?.folderId ?: viewModel.defaultFolderId) }
    var dueDate by remember { mutableStateOf(task?.dueDate) }
    var dueTime by remember { mutableStateOf(task?.dueTime) }
    var recurrence by remember { mutableStateOf(task?.recurrence ?: "NONE") }
    var tags by remember { mutableStateOf(task?.tags ?: "") }
    var estimatedDuration by remember { mutableStateOf(task?.estimatedDurationMinutes ?: 0) }
    var location by remember { mutableStateOf(task?.location ?: "") }

    // --- UI tab selection ---
    var selectedTab by remember { mutableStateOf(0) } // 0: Details, 1: Checklists, 2: Reminders, 3: Comments & Attach

    // --- Time tracking stopwatch state ---
    var timeSpent by remember { mutableStateOf(task?.timeSpentSeconds ?: 0L) }
    var isTimerActive by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            while (isTimerActive) {
                kotlinx.coroutines.delay(1000)
                timeSpent++
            }
        }
    }

    // --- Inline item creators ---
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newCommentText by remember { mutableStateOf("") }
    var newAttachmentName by remember { mutableStateOf("") }
    var newAttachmentUri by remember { mutableStateOf("") }
    var newAttachmentType by remember { mutableStateOf("LINK") } // LINK, PDF, IMAGE

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = {
        // Auto-save stopwatch on dismiss
        if (task != null && timeSpent != task.timeSpentSeconds) {
            viewModel.updateTaskFull(task.copy(timeSpentSeconds = timeSpent))
        }
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("task_editor_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (task == null) "Create Task" else "Edit Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // Tab Row for modular workspace
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Details", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Checklist", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("Reminders", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                        Text("Files", modifier = Modifier.padding(12.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // --- Tab 0: Core Details ---
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Task Title") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("task_title_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Short Description") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Markdown Notes") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Priority selector
                                        var priorityExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { priorityExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.PriorityHigh, "Priority", tint = getPriorityColor(priority))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(priority, style = MaterialTheme.typography.bodySmall)
                                            }
                                            DropdownMenu(
                                                expanded = priorityExpanded,
                                                onDismissRequest = { priorityExpanded = false }
                                            ) {
                                                listOf("CRITICAL", "HIGH", "MEDIUM", "NORMAL", "LOW").forEach { p ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(12.dp)
                                                                        .background(getPriorityColor(p), RoundedCornerShape(6.dp))
                                                                )
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(p)
                                                            }
                                                        },
                                                        onClick = {
                                                            priority = p
                                                            priorityExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Folder / Folder selector
                                        val foldersList by viewModel.folders.collectAsState()
                                        var folderExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { folderExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                val folderObj = foldersList.find { it.id == folderId }
                                                Icon(Icons.Default.Folder, "Folder", tint = Color(folderObj?.colorHex ?: 0xFF757575.toInt()))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(folderObj?.name ?: "Personal", style = MaterialTheme.typography.bodySmall)
                                            }
                                            DropdownMenu(
                                                expanded = folderExpanded,
                                                onDismissRequest = { folderExpanded = false }
                                            ) {
                                                foldersList.forEach { f ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Default.Folder, f.name, tint = Color(f.colorHex), modifier = Modifier.size(16.dp))
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(f.name)
                                                            }
                                                        },
                                                        onClick = {
                                                            folderId = f.id
                                                            folderExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Due Date Picker
                                        OutlinedButton(
                                            onClick = {
                                                val cal = Calendar.getInstance()
                                                dueDate?.let { cal.timeInMillis = it }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        val finalCal = Calendar.getInstance().apply {
                                                            set(Calendar.YEAR, y)
                                                            set(Calendar.MONTH, m)
                                                            set(Calendar.DAY_OF_MONTH, d)
                                                        }
                                                        dueDate = finalCal.timeInMillis
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.DateRange, "Date", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = dueDate?.let { sdf.format(Date(it)) } ?: "No Due Date",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        // Due Time Picker
                                        OutlinedButton(
                                            onClick = {
                                                TimePickerDialog(
                                                    context,
                                                    { _, h, m ->
                                                        dueTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                                                    },
                                                    12, 0, true
                                                ).show()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AccessTime, "Time", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = dueTime ?: "No Time",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Recurrence
                                        var recExpanded by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.weight(1.3f)) {
                                            OutlinedButton(
                                                onClick = { recExpanded = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Repeat, "Repeat", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Recur: $recurrence", style = MaterialTheme.typography.bodySmall)
                                            }
                                            DropdownMenu(
                                                expanded = recExpanded,
                                                onDismissRequest = { recExpanded = false }
                                            ) {
                                                listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { r ->
                                                    DropdownMenuItem(
                                                        text = { Text(r) },
                                                        onClick = {
                                                            recurrence = r
                                                            recExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Est. Duration input
                                        OutlinedTextField(
                                            value = if (estimatedDuration == 0) "" else estimatedDuration.toString(),
                                            onValueChange = { estimatedDuration = it.toIntOrNull() ?: 0 },
                                            label = { Text("Est. Min", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = tags,
                                        onValueChange = { tags = it },
                                        label = { Text("Tags (comma separated)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = location,
                                        onValueChange = { location = it },
                                        label = { Text("Location (optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }

                                // Active task tracking stopwatch
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Task Duration Tracking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "Spent: ${timeSpent / 3600}h ${(timeSpent % 3600) / 60}m ${timeSpent % 60}s",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            IconButton(
                                                onClick = { isTimerActive = !isTimerActive },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = if (isTimerActive) Color.Red else MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = if (isTimerActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Stopwatch Controls",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // --- Tab 1: Checklist / Subtasks ---
                            if (task == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Please save the task details first to configure subtasks.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                val subtasks by viewModel.editingSubtasks.collectAsState()
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newSubtaskTitle,
                                            onValueChange = { newSubtaskTitle = it },
                                            placeholder = { Text("Add rapid subtask...") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (newSubtaskTitle.isNotBlank()) {
                                                    viewModel.addSubtask(task.id, newSubtaskTitle)
                                                    newSubtaskTitle = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, "Add")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(subtasks) { sub ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = sub.isCompleted,
                                                    onCheckedChange = { viewModel.toggleSubtaskCompleted(sub) }
                                                )
                                                Text(
                                                    text = sub.title,
                                                    modifier = Modifier.weight(1f),
                                                    style = if (sub.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant) else MaterialTheme.typography.bodyMedium
                                                )
                                                IconButton(onClick = { viewModel.deleteSubtask(sub) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // --- Tab 2: Reminders ---
                            if (task == null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Please save the task details first to add reminders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                val reminders by viewModel.editingReminders.collectAsState()
                                Column(modifier = Modifier.fillMaxSize()) {
                                    OutlinedButton(
                                        onClick = {
                                            // Select dynamic reminder time
                                            DatePickerDialog(context, { _, y, m, d ->
                                                TimePickerDialog(context, { _, h, min ->
                                                    val finalCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, y)
                                                        set(Calendar.MONTH, m)
                                                        set(Calendar.DAY_OF_MONTH, d)
                                                        set(Calendar.HOUR_OF_DAY, h)
                                                        set(Calendar.MINUTE, min)
                                                    }
                                                    viewModel.addReminder(task.id, finalCal.timeInMillis, "Arbab Reminder")
                                                }, 12, 0, true).show()
                                            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, "Add Reminder")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Set Multi-reminders")
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(reminders) { r ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(r.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(r.triggerTime)),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.deleteReminder(r) }) {
                                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // --- Tab 3: Comments & Attachments ---
                            if (task == null) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Please save the task details first to add files/comments.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                val attachments by viewModel.editingAttachments.collectAsState()
                                val comments by viewModel.editingComments.collectAsState()

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text("Attachments / Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newAttachmentName,
                                            onValueChange = { newAttachmentName = it },
                                            placeholder = { Text("Name (e.g. Doc link)") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = newAttachmentUri,
                                            onValueChange = { newAttachmentUri = it },
                                            placeholder = { Text("URL / URI") },
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (newAttachmentName.isNotBlank() && newAttachmentUri.isNotBlank()) {
                                                    viewModel.addAttachment(task.id, newAttachmentName, newAttachmentType, newAttachmentUri)
                                                    newAttachmentName = ""
                                                    newAttachmentUri = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AttachFile, "Attach")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display attachments
                                    Box(modifier = Modifier.height(100.dp)) {
                                        if (attachments.isEmpty()) {
                                            Text("No attachments yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                                        } else {
                                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                items(attachments) { att ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Link, "Link", modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(att.fileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteAttachment(att) }) {
                                                            Icon(Icons.Default.Close, "Delete", modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.8f))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    Text("Comments log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newCommentText,
                                            onValueChange = { newCommentText = it },
                                            placeholder = { Text("Write comments...") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Button(
                                            onClick = {
                                                if (newCommentText.isNotBlank()) {
                                                    viewModel.addComment(task.id, newCommentText)
                                                    newCommentText = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Send, "Send")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(comments) { c ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(c.createdAt)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(onClick = { viewModel.deleteComment(c) }, modifier = Modifier.size(16.dp)) {
                                                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(14.dp), tint = Color.Red)
                                                    }
                                                }
                                                Text(c.content, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Save buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (task == null) {
                                    // Create new task
                                    viewModel.createTask(
                                        title = title,
                                        description = description,
                                        notes = notes,
                                        priority = priority,
                                        folderId = folderId,
                                        dueDate = dueDate,
                                        dueTime = dueTime,
                                        recurrence = recurrence,
                                        tags = tags,
                                        estimatedDuration = estimatedDuration
                                    )
                                } else {
                                    // Update existing task
                                    val updated = task.copy(
                                        title = title,
                                        description = description,
                                        notes = notes,
                                        priority = priority,
                                        folderId = folderId,
                                        dueDate = dueDate,
                                        dueTime = dueTime,
                                        recurrence = recurrence,
                                        tags = tags,
                                        estimatedDurationMinutes = estimatedDuration,
                                        location = if (location.isEmpty()) null else location,
                                        timeSpentSeconds = timeSpent
                                    )
                                    viewModel.updateTaskFull(updated)
                                }
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_task_button")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}

fun getPriorityColor(priority: String): Color {
    return when (priority) {
        "CRITICAL" -> PriorityCritical
        "HIGH" -> PriorityHigh
        "MEDIUM" -> PriorityMedium
        "NORMAL" -> PriorityNormal
        "LOW" -> PriorityLow
        else -> PriorityCompleted
    }
}
