package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ActivityLog
import com.example.data.Folder
import com.example.data.Task
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onQuickAddTask: () -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val foldersList by viewModel.folders.collectAsState()
    val logs by viewModel.activityLogs.collectAsState()

    // Filtered lists for counts
    val todayTasksList by viewModel.todayTasks.collectAsState()
    val upcomingTasksList by viewModel.upcomingTasks.collectAsState()
    val overdueTasksList by viewModel.overdueTasks.collectAsState()

    val completedTodayList = todayTasksList.filter { it.status == "COMPLETED" }
    val completionPercentage = if (todayTasksList.isNotEmpty()) {
        (completedTodayList.size * 100) / todayTasksList.size
    } else 0

    // Streak tracker calculation
    val streakDays = remember(tasks) {
        // Simple logic: check activity logs or completed tasks over last few days
        val calendar = Calendar.getInstance()
        var streak = 0
        var checkDate = true
        while (checkDate) {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
            val completedOnDay = tasks.any { t ->
                t.status == "COMPLETED" && t.modifiedDate > 0 &&
                        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(t.modifiedDate)) == dateStr
            }
            if (completedOnDay) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                checkDate = false
            }
        }
        if (streak == 0 && completedTodayList.isNotEmpty()) 1 else streak
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Premium Welcome Header & Banner Image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner),
                        contentDescription = "Workspace Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Glassmorphic / Gradient Overlay for dark atmospheric depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                colorSchemeOverlayBrush()
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Arbab Tasks",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Master your workflow. Stay focus and offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 2. High-level metric summary cards (Today, Upcoming, Overdue, Streak)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Today's Tasks",
                    value = todayTasksList.size.toString(),
                    icon = Icons.Default.Today,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
                MetricCard(
                    title = "Overdue",
                    value = overdueTasksList.size.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Completed Today",
                    value = completedTodayList.size.toString(),
                    icon = Icons.Default.DoneAll,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
                MetricCard(
                    title = "Daily Streak",
                    value = "$streakDays Days",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToFocus
                )
            }
        }

        // 3. High Density Productivity Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Daily Productivity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$completionPercentage%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "LEVEL ${streakDays + 5}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${completedTodayList.size} of ${todayTasksList.size} tasks done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Linear progress bar
                    LinearProgressIndicator(
                        progress = if (todayTasksList.isNotEmpty()) completionPercentage / 100f else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Bottom info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Overdue indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFD32F2F), CircleShape)
                                )
                                Text(
                                    text = "${overdueTasksList.size} Overdue",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Upcoming indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF1976D2), CircleShape)
                                )
                                Text(
                                    text = "${upcomingTasksList.size} Upcoming",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Streak Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$streakDays Day Streak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Horizontal Weekly Calendar Strip
        item {
            Text(
                text = "Calendar Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            WeeklyStripRow(tasks)
        }

        // 5. Beautiful Category Distribution Pie Chart
        item {
            Text(
                text = "Workspace Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            CategoryDistributionCard(tasks, foldersList)
        }

        // 6. Recent Activity Timeline / logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearSelection() }) {
                    Text("Clear Log", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No recent activities. Actions appear here as you work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        } else {
            items(logs.take(5)) { log ->
                ActivityLogItem(log)
            }
        }

        // Bottom space to avoid overlap with buttons
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Open", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun WeeklyStripRow(tasks: List<Task>) {
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2)
        for (i in 0 until 7) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            val isCurrent = isSameDay(date, Date())
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            val dayNum = SimpleDateFormat("dd", Locale.getDefault()).format(date)

            val tasksCount = tasks.count {
                it.dueDate != null && isSameDay(Date(it.dueDate), date)
            }

            Card(
                modifier = Modifier
                    .width(60.dp)
                    .height(85.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isCurrent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (tasksCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isCurrent) Color.White else MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDistributionCard(tasks: List<Task>, folders: List<Folder>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val stats = remember(tasks, folders) {
                folders.map { f ->
                    val count = tasks.count { it.folderId == f.id && it.status != "COMPLETED" }
                    Pair(f, count)
                }.filter { it.second > 0 }
            }

            if (stats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active tasks in folders.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Beautiful custom Canvas ring segment drawer
                    Canvas(
                        modifier = Modifier
                            .size(130.dp)
                            .weight(1f)
                    ) {
                        val total = stats.sumOf { it.second }.toFloat()
                        var startAngle = -90f
                        stats.forEach { (folder, count) ->
                            val sweep = (count / total) * 360f
                            drawArc(
                                color = Color(folder.colorHex),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                                size = Size(size.width - 20.dp.toPx(), size.height - 20.dp.toPx()),
                                alpha = 1f
                            )
                            startAngle += sweep
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend column
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stats.take(4).forEach { (folder, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(folder.colorHex), CircleShape)
                                )
                                Text(
                                    text = "${folder.name} ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        if (stats.size > 4) {
                            Text("+ ${stats.size - 4} other folders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: ActivityLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (log.action) {
                    "TASK_CREATED" -> Icons.Default.Add
                    "TASK_UPDATED" -> Icons.Default.Edit
                    "TASK_DELETED" -> Icons.Default.DeleteForever
                    "TASK_SOFT_DELETED" -> Icons.Default.Delete
                    "TASK_RESTORED" -> Icons.Default.Restore
                    "FOCUS_COMPLETED" -> Icons.Default.LocalFireDepartment
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = "Event",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(log.details, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// Helper linear gradient overlay for atmospheric welcome banners
@Composable
fun colorSchemeOverlayBrush(): androidx.compose.ui.graphics.Brush {
    return androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.75f)
        )
    )
}

fun isSameDay(d1: Date, d2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = d1 }
    val cal2 = Calendar.getInstance().apply { time = d2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
