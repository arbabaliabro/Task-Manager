package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Folder
import com.example.data.Task
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksListScreen(
    viewModel: MainViewModel,
    onEditTask: (Task) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    val foldersList by viewModel.folders.collectAsState()
    val selectedIds by viewModel.selectedTaskIds

    // --- Bottom sheets/dialogs controllers ---
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var folderColorInput by remember { mutableStateOf(0xFF1E88E5.toInt()) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }

    // --- Search & Filtering Logic ---
    val filteredTasks = remember(
        tasks,
        viewModel.searchQuery,
        viewModel.filterFolderId,
        viewModel.filterPriority,
        viewModel.filterStatus,
        viewModel.filterDateRange,
        viewModel.filterPinnedOnly,
        viewModel.filterFavoriteOnly,
        foldersList
    ) {
        tasks.filter { t ->
            // Search Match
            val matchSearch = if (viewModel.searchQuery.isBlank()) true else {
                t.title.contains(viewModel.searchQuery, ignoreCase = true) ||
                        t.description.contains(viewModel.searchQuery, ignoreCase = true) ||
                        t.tags.contains(viewModel.searchQuery, ignoreCase = true) ||
                        t.notes.contains(viewModel.searchQuery, ignoreCase = true)
            }

            // Folder Match
            val matchFolder = if (viewModel.filterFolderId == null) true else t.folderId == viewModel.filterFolderId

            // Priority Match
            val matchPriority = if (viewModel.filterPriority == null) true else t.priority == viewModel.filterPriority

            // Status Match
            val matchStatus = if (viewModel.filterStatus == null) true else t.status == viewModel.filterStatus

            // Pinned Match
            val matchPinned = if (viewModel.filterPinnedOnly) t.isPinned else true

            // Favorite Match
            val matchFavorite = if (viewModel.filterFavoriteOnly) t.isFavorite else true

            // Date Match
            val matchDate = when (viewModel.filterDateRange) {
                MainViewModel.DateRangeFilter.ALL -> true
                MainViewModel.DateRangeFilter.TODAY -> isToday(t.dueDate)
                MainViewModel.DateRangeFilter.TOMORROW -> isTomorrow(t.dueDate)
                MainViewModel.DateRangeFilter.THIS_WEEK -> isThisWeek(t.dueDate)
                MainViewModel.DateRangeFilter.THIS_MONTH -> isThisMonth(t.dueDate)
                MainViewModel.DateRangeFilter.OVERDUE -> isOverdue(t.dueDate, t.status)
            }

            matchSearch && matchFolder && matchPriority && matchStatus && matchDate && matchPinned && matchFavorite
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasks_list_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Search bar & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Search everywhere...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_bar_input"),
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )

                // Recycle Bin Icon Indicator
                IconButton(
                    onClick = { showRecycleBinDialog = true },
                    modifier = Modifier.testTag("recycle_bin_button")
                ) {
                    BadgedBox(badge = {
                        if (deletedTasks.isNotEmpty()) {
                            Badge { Text(deletedTasks.size.toString()) }
                        }
                    }) {
                        Icon(Icons.Default.DeleteSweep, "Recycle Bin")
                    }
                }
            }

            // 2. Folder List Selector horizontally
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Folder 'ALL' pill
                item {
                    FilterChip(
                        selected = viewModel.filterFolderId == null,
                        onClick = { viewModel.filterFolderId = null },
                        label = { Text("All Folders") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Custom/System folders
                items(foldersList) { folder ->
                    FilterChip(
                        selected = viewModel.filterFolderId == folder.id,
                        onClick = { viewModel.filterFolderId = folder.id },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(folder.colorHex), CircleShape)
                                )
                                Text(folder.name)
                            }
                        },
                        trailingIcon = {
                            if (!folder.isSystem && viewModel.filterFolderId == folder.id) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete Folder",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.deleteFolder(folder) },
                                    tint = Color.Red
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Add Folder Card button
                item {
                    IconButton(
                        onClick = { showAddFolderDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.CreateNewFolder, "Add Folder", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 3. Multi-criteria Time Filters Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pin toggle
                item {
                    FilterChip(
                        selected = viewModel.filterPinnedOnly,
                        onClick = { viewModel.filterPinnedOnly = !viewModel.filterPinnedOnly },
                        label = { Text("Pinned") },
                        leadingIcon = { Icon(Icons.Default.PushPin, "Pinned", modifier = Modifier.size(14.dp)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Date ranges
                MainViewModel.DateRangeFilter.values().forEach { filter ->
                    if (filter != MainViewModel.DateRangeFilter.ALL) {
                        item {
                            FilterChip(
                                selected = viewModel.filterDateRange == filter,
                                onClick = {
                                    viewModel.filterDateRange = if (viewModel.filterDateRange == filter) MainViewModel.DateRangeFilter.ALL else filter
                                },
                                label = { Text(filter.name.lowercase().capitalize(Locale.ROOT).replace("_", " ")) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // 4. Tasks list view
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Inbox, "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("No tasks found matching current filters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("tasks_lazy_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val isSelected = selectedIds.contains(task.id)
                        TaskListItem(
                            task = task,
                            folder = foldersList.find { it.id == task.folderId },
                            isSelected = isSelected,
                            onToggleSelection = { viewModel.toggleSelection(task.id) },
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleSelection(task.id)
                                } else {
                                    viewModel.loadEditingTaskDetails(task)
                                    onEditTask(task)
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(task.id) },
                            onCheckChanged = { progress -> viewModel.updateTaskProgress(task, progress) },
                            onDuplicate = { viewModel.duplicateTask(task) },
                            onPin = { viewModel.toggleTaskPin(task) },
                            onFavorite = { viewModel.toggleTaskFavorite(task) },
                            onDelete = { viewModel.softDeleteTask(task.id) }
                        )
                    }
                }
            }
        }

        // 5. Bulk action floating bar at the bottom
        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_actions_bar"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedIds.size} Selected",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Bulk high priority override
                        IconButton(onClick = { viewModel.bulkEditPriority("HIGH") }) {
                            Icon(Icons.Default.PriorityHigh, "Bulk High Priority")
                        }
                        // Bulk Move Folder Dialog launcher
                        var showBulkFolderExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showBulkFolderExpanded = true }) {
                                Icon(Icons.Default.Folder, "Bulk Move Folder")
                            }
                            DropdownMenu(
                                expanded = showBulkFolderExpanded,
                                onDismissRequest = { showBulkFolderExpanded = false }
                            ) {
                                foldersList.forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f.name) },
                                        onClick = {
                                            viewModel.bulkMoveSelected(f.id)
                                            showBulkFolderExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Bulk Delete action
                        IconButton(onClick = { viewModel.bulkDeleteSelected() }) {
                            Icon(Icons.Default.Delete, "Bulk Delete", tint = Color.Red)
                        }

                        // Cancel Bulk Selection
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, "Cancel Selection")
                        }
                    }
                }
            }
        }

        // --- Dialog 1: Add custom folder ---
        if (showAddFolderDialog) {
            AlertDialog(
                onDismissRequest = { showAddFolderDialog = false },
                title = { Text("Create Custom Folder") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text("Folder Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Simple grid of color label options
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colors = listOf(0xFFD32F2F.toInt(), 0xFF388E3C.toInt(), 0xFF1976D2.toInt(), 0xFFF57C00.toInt(), 0xFFD81B60.toInt(), 0xFF7B1FA2.toInt())
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(c), CircleShape)
                                        .border(
                                            width = if (folderColorInput == c) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable { folderColorInput = c }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (folderNameInput.isNotBlank()) {
                                viewModel.createFolder(folderNameInput, folderColorInput, "folder")
                                folderNameInput = ""
                                showAddFolderDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Dialog 2: Recycle Bin dialog ---
        if (showRecycleBinDialog) {
            AlertDialog(
                onDismissRequest = { showRecycleBinDialog = false },
                title = { Text("Recycle Bin") },
                text = {
                    Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                        if (deletedTasks.isEmpty()) {
                            Text("Recycle bin is empty.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(deletedTasks) { dt ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dt.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                            Text("Overdue / Deleted", style = MaterialTheme.typography.labelSmall, color = Color.Red.copy(alpha = 0.8f))
                                        }
                                        Row {
                                            IconButton(onClick = { viewModel.restoreTask(dt.id) }) {
                                                Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.permanentlyDeleteTask(dt) }) {
                                                Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showRecycleBinDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskListItem(
    task: Task,
    folder: Folder?,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckChanged: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val priorityColor = getPriorityColor(task.priority)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left priority border indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox for completion percentage
                Checkbox(
                    checked = task.status == "COMPLETED" || task.completionPercentage >= 100,
                    onCheckedChange = { checked ->
                        onCheckChanged(if (checked) 100 else 0)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = priorityColor, uncheckedColor = priorityColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (task.isPinned) {
                            Icon(Icons.Default.PushPin, "Pinned", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.status == "COMPLETED") TextDecoration.LineThrough else null,
                            color = if (task.status == "COMPLETED") MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }

                    // IN PROGRESS Progress Bar from high density design
                    if (task.completionPercentage in 1..99) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = task.completionPercentage / 100f,
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = priorityColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "IN PROGRESS • ${task.completionPercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = priorityColor,
                                fontSize = 8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom badge items
                    Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority Indicator Badge
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(task.priority, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = priorityColor, fontSize = 9.sp)
                    }

                    // Folder Indicator
                    folder?.let { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color(f.colorHex), CircleShape))
                            Text(f.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }

                    // Due Date
                    task.dueDate?.let { due ->
                        val dateString = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(due))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.AccessTime, "Due", modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // More Menu dropdown icon launcher
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            onDuplicate()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, "Duplicate", modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (task.isPinned) "Unpin" else "Pin") },
                        onClick = {
                            onPin()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.PushPin, "Pin", modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (task.isFavorite) "Remove Fav" else "Favorite") },
                        onClick = {
                            onFavorite()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Favorite, "Favorite", modifier = Modifier.size(16.dp)) }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = {
                            onDelete()
                            expandedMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

// --- Filtering Utility Checks ---
fun isToday(time: Long?): Boolean {
    if (time == null) return false
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = time }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isTomorrow(time: Long?): Boolean {
    if (time == null) return false
    val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun isThisWeek(time: Long?): Boolean {
    if (time == null) return false
    val cal = Calendar.getInstance()
    val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
    val currentYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = time
    return cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek
}

fun isThisMonth(time: Long?): Boolean {
    if (time == null) return false
    val cal = Calendar.getInstance()
    val currentMonth = cal.get(Calendar.MONTH)
    val currentYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = time
    return cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
}

fun isOverdue(time: Long?, status: String): Boolean {
    if (time == null) return false
    if (status == "COMPLETED" || status == "CANCELLED" || status == "ARCHIVED") return false
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return time < startOfToday
}
