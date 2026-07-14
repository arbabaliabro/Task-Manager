package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onEditTask: (Task) -> Unit
) {
    val tasks by viewModel.allTasks.collectAsState()
    val foldersList by viewModel.folders.collectAsState()

    var calendarMode by remember { mutableStateOf(0) } // 0: Month, 1: Week, 2: Day, 3: Agenda
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val sdfDayFull = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

    // Filter tasks for the selected date
    val dayTasks = remember(tasks, selectedDate) {
        tasks.filter { t ->
            t.dueDate != null && isSameDayCalendar(Calendar.getInstance().apply { timeInMillis = t.dueDate }, selectedDate)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen")
    ) {
        // Mode selector TabRow
        TabRow(
            selectedTabIndex = calendarMode,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Month", "Week", "Day", "Agenda").forEachIndexed { index, mode ->
                Tab(
                    selected = calendarMode == index,
                    onClick = { calendarMode = index },
                    text = { Text(mode, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        // Navigation header (Prev Month/Next Month)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                selectedDate = (selectedDate.clone() as Calendar).apply {
                    add(
                        if (calendarMode == 0) Calendar.MONTH else if (calendarMode == 1) Calendar.WEEK_OF_YEAR else Calendar.DAY_OF_YEAR,
                        -1
                    )
                }
            }) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }

            Text(
                text = if (calendarMode == 0) sdfMonth.format(selectedDate.time) else sdfDayFull.format(selectedDate.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                selectedDate = (selectedDate.clone() as Calendar).apply {
                    add(
                        if (calendarMode == 0) Calendar.MONTH else if (calendarMode == 1) Calendar.WEEK_OF_YEAR else Calendar.DAY_OF_YEAR,
                        1
                    )
                }
            }) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }

        // Calendar views container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (calendarMode) {
                0 -> {
                    // Month Grid View
                    MonthGridView(selectedDate, tasks) { newDate ->
                        selectedDate = newDate
                    }
                }
                1 -> {
                    // Week perspective
                    WeekGridView(selectedDate, tasks) { newDate ->
                        selectedDate = newDate
                    }
                }
                2 -> {
                    // Day list perspective
                    DayAgendaView(dayTasks, onEditTask, viewModel)
                }
                3 -> {
                    // General agenda showing all scheduled tasks chronological
                    val scheduledTasks = remember(tasks) {
                        tasks.filter { it.dueDate != null }.sortedBy { it.dueDate }
                    }
                    AgendaListView(scheduledTasks, onEditTask, viewModel)
                }
            }
        }

        // Agenda listing at bottom for Month and Week modes
        if (calendarMode < 2) {
            Divider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(16.dp)
            ) {
                Text(
                    "Agenda for ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(selectedDate.time)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (dayTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks scheduled for today.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayTasks) { task ->
                            CalendarAgendaItem(task, onEditTask, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthGridView(
    currentCalendar: Calendar,
    tasks: List<Task>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = remember(currentCalendar) {
        val list = mutableListOf<Calendar?>()
        val tempCal = currentCalendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        for (i in 0 until firstDayOfWeek) {
            list.add(null) // Empty offsets
        }

        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDays) {
            val cellCal = tempCal.clone() as Calendar
            cellCal.set(Calendar.DAY_OF_MONTH, day)
            list.add(cellCal)
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week days headings
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(daysInMonth) { date ->
                if (date == null) {
                    Spacer(modifier = Modifier.size(40.dp))
                } else {
                    val isToday = isSameDayCalendar(date, Calendar.getInstance())
                    val tasksCount = tasks.count {
                        it.dueDate != null && isSameDayCalendar(Calendar.getInstance().apply { timeInMillis = it.dueDate }, date)
                    }

                    Card(
                        modifier = Modifier
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDateSelected(date) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            if (tasksCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekGridView(
    currentCalendar: Calendar,
    tasks: List<Task>,
    onDateSelected: (Calendar) -> Unit
) {
    val weekDays = remember(currentCalendar) {
        val list = mutableListOf<Calendar>()
        val tempCal = currentCalendar.clone() as Calendar
        // Move tempCal to Sunday of current selected week
        tempCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        for (i in 0 until 7) {
            list.add(tempCal.clone() as Calendar)
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { date ->
            val isToday = isSameDayCalendar(date, Calendar.getInstance())
            val isSelected = isSameDayCalendar(date, currentCalendar)
            val dayNum = date.get(Calendar.DAY_OF_MONTH).toString()
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date.time)

            val dayTasksCount = tasks.count {
                it.dueDate != null && isSameDayCalendar(Calendar.getInstance().apply { timeInMillis = it.dueDate }, date)
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateSelected(date) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dayNum,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    if (dayTasksCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DayAgendaView(dayTasks: List<Task>, onEditTask: (Task) -> Unit, viewModel: MainViewModel) {
    if (dayTasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tasks scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dayTasks) { task ->
                CalendarAgendaItem(task, onEditTask, viewModel)
            }
        }
    }
}

@Composable
fun AgendaListView(scheduledTasks: List<Task>, onEditTask: (Task) -> Unit, viewModel: MainViewModel) {
    if (scheduledTasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No scheduled tasks found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(scheduledTasks) { task ->
                CalendarAgendaItem(task, onEditTask, viewModel)
            }
        }
    }
}

@Composable
fun CalendarAgendaItem(
    task: Task,
    onEditTask: (Task) -> Unit,
    viewModel: MainViewModel
) {
    val priorityColor = getPriorityColor(task.priority)
    var expandedReschedule by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTask(task) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.size(12.dp).background(priorityColor, CircleShape))
                Column {
                    Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        task.dueTime?.let {
                            Text("at $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Quick rescheduling action
            Box {
                IconButton(onClick = { expandedReschedule = true }) {
                    Icon(Icons.Default.CalendarToday, "Reschedule", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = expandedReschedule,
                    onDismissRequest = { expandedReschedule = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Postpone to Today") },
                        onClick = {
                            viewModel.updateTaskFull(task.copy(dueDate = System.currentTimeMillis()))
                            expandedReschedule = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Postpone to Tomorrow") },
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                            viewModel.updateTaskFull(task.copy(dueDate = cal.timeInMillis))
                            expandedReschedule = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Postpone to Next Week") },
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
                            viewModel.updateTaskFull(task.copy(dueDate = cal.timeInMillis))
                            expandedReschedule = false
                        }
                    )
                }
            }
        }
    }
}

fun isSameDayCalendar(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
