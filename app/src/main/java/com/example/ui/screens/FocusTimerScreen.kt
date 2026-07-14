package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    viewModel: MainViewModel
) {
    val tasks by viewModel.allTasks.collectAsState()
    val focusTask = viewModel.focusTask
    val totalTimeSeconds = viewModel.focusTimerTotalSeconds
    val leftSeconds = viewModel.focusTimerLeftSeconds
    val isRunning = viewModel.isFocusTimerRunning
    val mode = viewModel.pomodoroMode
    val cyclesCompleted = viewModel.pomodoroCyclesCompleted

    val minutes = leftSeconds / 60
    val seconds = leftSeconds % 60
    val progress = if (totalTimeSeconds > 0) leftSeconds.toFloat() / totalTimeSeconds else 0f

    var showTaskSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("focus_timer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Mode Header Indicator (Work, Short Break, Long Break)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (mode) {
                    MainViewModel.PomodoroMode.WORK -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    MainViewModel.PomodoroMode.SHORT_BREAK -> Color(0xFF388E3C).copy(alpha = 0.15f)
                    MainViewModel.PomodoroMode.LONG_BREAK -> Color(0xFF1976D2).copy(alpha = 0.15f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (mode) {
                        MainViewModel.PomodoroMode.WORK -> Icons.Default.Timer
                        else -> Icons.Default.Spa
                    },
                    contentDescription = "Session Mode",
                    tint = when (mode) {
                        MainViewModel.PomodoroMode.WORK -> MaterialTheme.colorScheme.primary
                        MainViewModel.PomodoroMode.SHORT_BREAK -> Color(0xFF388E3C)
                        MainViewModel.PomodoroMode.LONG_BREAK -> Color(0xFF1976D2)
                    }
                )
                Text(
                    text = when (mode) {
                        MainViewModel.PomodoroMode.WORK -> "Focus Work Session"
                        MainViewModel.PomodoroMode.SHORT_BREAK -> "Short Relax Break"
                        MainViewModel.PomodoroMode.LONG_BREAK -> "Long Relax Break"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Circular Timer Visual Canvas
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            val fillAccent = when (mode) {
                MainViewModel.PomodoroMode.WORK -> MaterialTheme.colorScheme.primary
                MainViewModel.PomodoroMode.SHORT_BREAK -> Color(0xFF388E3C)
                MainViewModel.PomodoroMode.LONG_BREAK -> Color(0xFF1976D2)
            }

            Canvas(modifier = Modifier.size(220.dp)) {
                // Background Track
                drawCircle(color = trackColor, style = Stroke(width = 12.dp.toPx()))
                // Countdown Arc progress
                drawArc(
                    color = fillAccent,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 52.sp),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${cyclesCompleted} Loops Completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Preset Time Blocks selector
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Select Focus Block", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60).forEach { mins ->
                    item {
                        FilterChip(
                            selected = totalTimeSeconds == mins * 60,
                            onClick = { viewModel.setPomodoroTimerPreset(mins) },
                            label = { Text("$mins min") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Associated Task Picker Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Focusing on Task", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { showTaskSelector = true },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Assignment, "Task")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = focusTask?.title ?: "Select Task to Bind Time",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showTaskSelector) {
                DropdownMenu(
                    expanded = showTaskSelector,
                    onDismissRequest = { showTaskSelector = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    tasks.filter { it.status != "COMPLETED" }.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.title, maxLines = 1) },
                            onClick = {
                                viewModel.focusTask = t
                                showTaskSelector = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("None / Ambient Timer", color = Color.Red) },
                        onClick = {
                            viewModel.focusTask = null
                            showTaskSelector = false
                        }
                    )
                }
            }
        }

        // Action Buttons (Play/Pause, Reset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause Action
            Button(
                onClick = {
                    if (isRunning) viewModel.pauseFocusTimer() else viewModel.startFocusTimer(focusTask)
                },
                modifier = Modifier
                    .size(width = 160.dp, height = 56.dp)
                    .testTag("focus_toggle_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, "Toggle")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Pause" else "Start focus")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Reset Action
            IconButton(
                onClick = { viewModel.resetFocusTimer() },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.Refresh, "Reset Timer")
            }
        }
    }
}
