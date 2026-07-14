package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

class MainViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    // --- Core Reactive Streams ---
    val folders = repository.allFolders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deletedTasks = repository.deletedTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activityLogs = repository.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filtering States ---
    var searchQuery by mutableStateOf("")
    var filterFolderId by mutableStateOf<String?>(null)
    var filterPriority by mutableStateOf<String?>(null)
    var filterStatus by mutableStateOf<String?>(null)
    var filterDateRange by mutableStateOf<DateRangeFilter>(DateRangeFilter.ALL)
    var filterPinnedOnly by mutableStateOf(false)
    var filterFavoriteOnly by mutableStateOf(false)

    enum class DateRangeFilter { ALL, TODAY, TOMORROW, THIS_WEEK, THIS_MONTH, OVERDUE }

    // --- Multi-Select / Bulk Actions ---
    var selectedTaskIds = mutableStateOf<Set<String>>(emptySet())

    // --- Theme & Customization Settings ---
    private val prefs = application.getSharedPreferences("arbab_tasks_prefs", Context.MODE_PRIVATE)
    var darkThemeSetting by mutableStateOf(prefs.getString("theme", "SYSTEM") ?: "SYSTEM")
    var accentColorHex by mutableStateOf(prefs.getInt("accent_color", 0xFF6750A4.toInt())) // Purple default
    var defaultPriority by mutableStateOf(prefs.getString("default_priority", "NORMAL") ?: "NORMAL")
    var defaultFolderId by mutableStateOf(prefs.getString("default_folder", "personal") ?: "personal")

    // --- Pomodoro Timer / Focus Mode States ---
    var focusTask by mutableStateOf<Task?>(null)
    var focusTimerLeftSeconds by mutableStateOf(25 * 60)
    var focusTimerTotalSeconds by mutableStateOf(25 * 60)
    var isFocusTimerRunning by mutableStateOf(false)
    var pomodoroMode by mutableStateOf(PomodoroMode.WORK) // WORK, SHORT_BREAK, LONG_BREAK
    var pomodoroCyclesCompleted by mutableStateOf(0)
    private var timerJob: Job? = null

    enum class PomodoroMode { WORK, SHORT_BREAK, LONG_BREAK }

    // --- Selected Task for Editing Detail Sheet ---
    var editingTask by mutableStateOf<Task?>(null)
    val editingSubtasks = MutableStateFlow<List<Subtask>>(emptyList())
    val editingAttachments = MutableStateFlow<List<Attachment>>(emptyList())
    val editingComments = MutableStateFlow<List<Comment>>(emptyList())
    val editingReminders = MutableStateFlow<List<Reminder>>(emptyList())

    // --- Local statistics calculated from reactive tasks ---
    val todayTasks = allTasks.map { list ->
        list.filter { isToday(it.dueDate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingTasks = allTasks.map { list ->
        list.filter { isUpcoming(it.dueDate) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overdueTasks = allTasks.map { list ->
        list.filter { isOverdue(it.dueDate, it.status) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Log launch
        viewModelScope.launch {
            repository.logActivity(null, "APP_LAUNCHED", "Arbab Tasks launch successful.")
        }
    }

    // --- Settings updates ---
    fun updateTheme(theme: String) {
        darkThemeSetting = theme
        prefs.edit().putString("theme", theme).apply()
    }

    fun updateAccentColor(color: Int) {
        accentColorHex = color
        prefs.edit().putInt("accent_color", color).apply()
    }

    fun updateDefaultPriority(p: String) {
        defaultPriority = p
        prefs.edit().putString("default_priority", p).apply()
    }

    fun updateDefaultFolder(folderId: String) {
        defaultFolderId = folderId
        prefs.edit().putString("default_folder", folderId).apply()
    }

    // --- Folder Operations ---
    fun createFolder(name: String, color: Int, icon: String) {
        viewModelScope.launch {
            val folder = Folder(id = UUID.randomUUID().toString(), name = name, colorHex = color, iconName = icon)
            repository.insertFolder(folder)
            repository.logActivity(null, "FOLDER_CREATED", "Created folder \"$name\"")
        }
    }

    fun renameFolder(id: String, newName: String, color: Int, icon: String) {
        viewModelScope.launch {
            val folder = Folder(id = id, name = newName, colorHex = color, iconName = icon)
            repository.insertFolder(folder)
            repository.logActivity(null, "FOLDER_RENAMED", "Renamed folder to \"$newName\"")
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            repository.logActivity(null, "FOLDER_DELETED", "Deleted folder \"${folder.name}\"")
        }
    }

    // --- Task CRUD & Helpers ---
    fun createTask(
        title: String,
        description: String,
        notes: String = "",
        priority: String = defaultPriority,
        folderId: String = defaultFolderId,
        dueDate: Long? = null,
        dueTime: String? = null,
        recurrence: String = "NONE",
        tags: String = "",
        estimatedDuration: Int = 0
    ) {
        viewModelScope.launch {
            val task = Task(
                id = UUID.randomUUID().toString(),
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
                status = "PENDING"
            )
            repository.insertTask(task)
        }
    }

    fun updateTaskFull(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(modifiedDate = System.currentTimeMillis()))
        }
    }

    fun toggleTaskPin(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned, modifiedDate = System.currentTimeMillis()), logAction = false)
        }
    }

    fun toggleTaskFavorite(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isFavorite = !task.isFavorite, modifiedDate = System.currentTimeMillis()), logAction = false)
        }
    }

    fun updateTaskProgress(task: Task, progress: Int) {
        viewModelScope.launch {
            val newStatus = if (progress >= 100) "COMPLETED" else if (progress > 0) "IN_PROGRESS" else "PENDING"
            val updated = task.copy(
                completionPercentage = progress.coerceIn(0, 100),
                status = newStatus,
                modifiedDate = System.currentTimeMillis()
            )
            repository.updateTask(updated)
        }
    }

    fun updateTaskStatus(task: Task, status: String) {
        viewModelScope.launch {
            val progress = if (status == "COMPLETED") 100 else task.completionPercentage
            val updated = task.copy(
                status = status,
                completionPercentage = progress,
                modifiedDate = System.currentTimeMillis()
            )
            repository.updateTask(updated)
        }
    }

    fun softDeleteTask(taskId: String) {
        viewModelScope.launch {
            repository.softDeleteTask(taskId)
        }
    }

    fun restoreTask(taskId: String) {
        viewModelScope.launch {
            repository.restoreTask(taskId)
        }
    }

    fun permanentlyDeleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun duplicateTask(task: Task) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val duplicate = task.copy(
                id = newId,
                title = "${task.title} (Copy)",
                createdDate = System.currentTimeMillis(),
                modifiedDate = System.currentTimeMillis(),
                status = "PENDING",
                completionPercentage = 0,
                timeSpentSeconds = 0
            )
            repository.insertTask(duplicate)
        }
    }

    // --- Bulk Operations ---
    fun clearSelection() {
        selectedTaskIds.value = emptySet()
    }

    fun toggleSelection(taskId: String) {
        val current = selectedTaskIds.value
        if (current.contains(taskId)) {
            selectedTaskIds.value = current - taskId
        } else {
            selectedTaskIds.value = current + taskId
        }
    }

    fun bulkDeleteSelected() {
        viewModelScope.launch {
            val ids = selectedTaskIds.value
            for (id in ids) {
                repository.softDeleteTask(id)
            }
            clearSelection()
        }
    }

    fun bulkMoveSelected(targetFolderId: String) {
        viewModelScope.launch {
            val ids = selectedTaskIds.value
            for (id in ids) {
                val task = repository.getTaskById(id)
                if (task != null) {
                    repository.updateTask(task.copy(folderId = targetFolderId, modifiedDate = System.currentTimeMillis()))
                }
            }
            clearSelection()
        }
    }

    fun bulkEditPriority(priority: String) {
        viewModelScope.launch {
            val ids = selectedTaskIds.value
            for (id in ids) {
                val task = repository.getTaskById(id)
                if (task != null) {
                    repository.updateTask(task.copy(priority = priority, modifiedDate = System.currentTimeMillis()))
                }
            }
            clearSelection()
        }
    }

    // --- Subtask Operations ---
    fun loadEditingTaskDetails(task: Task) {
        editingTask = task
        viewModelScope.launch {
            repository.getSubtasksForTask(task.id).collectLatest { editingSubtasks.value = it }
        }
        viewModelScope.launch {
            repository.getAttachmentsForTask(task.id).collectLatest { editingAttachments.value = it }
        }
        viewModelScope.launch {
            repository.getCommentsForTask(task.id).collectLatest { editingComments.value = it }
        }
        viewModelScope.launch {
            repository.getRemindersForTask(task.id).collectLatest { editingReminders.value = it }
        }
    }

    fun addSubtask(taskId: String, title: String, priority: String = "NORMAL", dueDate: Long? = null) {
        viewModelScope.launch {
            val sub = Subtask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = title,
                isCompleted = false,
                priority = priority,
                dueDate = dueDate
            )
            repository.insertSubtask(sub)
            recalculateTaskProgressFromSubtasks(taskId)
        }
    }

    fun toggleSubtaskCompleted(subtask: Subtask) {
        viewModelScope.launch {
            val updated = subtask.copy(isCompleted = !subtask.isCompleted, progress = if (!subtask.isCompleted) 100 else 0)
            repository.insertSubtask(updated)
            recalculateTaskProgressFromSubtasks(subtask.taskId)
        }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
            recalculateTaskProgressFromSubtasks(subtask.taskId)
        }
    }

    private suspend fun recalculateTaskProgressFromSubtasks(taskId: String) {
        val subs = repository.getSubtasksForTask(taskId).firstOrNull() ?: repository.getSubtasksForTask(taskId).first()
        if (subs.isNotEmpty()) {
            val completed = subs.count { it.isCompleted }
            val progress = (completed * 100) / subs.size
            val task = repository.getTaskById(taskId)
            if (task != null) {
                val newStatus = if (progress >= 100) "COMPLETED" else if (progress > 0) "IN_PROGRESS" else "PENDING"
                repository.updateTask(task.copy(completionPercentage = progress, status = newStatus, modifiedDate = System.currentTimeMillis()), logAction = false)
            }
        }
    }

    // --- Comments Operations ---
    fun addComment(taskId: String, text: String) {
        viewModelScope.launch {
            val comment = Comment(id = UUID.randomUUID().toString(), taskId = taskId, content = text)
            repository.insertComment(comment)
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch {
            repository.deleteComment(comment)
        }
    }

    // --- Attachments Operations ---
    fun addAttachment(taskId: String, name: String, type: String, uri: String) {
        viewModelScope.launch {
            val attach = Attachment(id = UUID.randomUUID().toString(), taskId = taskId, fileName = name, fileType = type, fileUri = uri)
            repository.insertAttachment(attach)
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            repository.deleteAttachment(attachment)
        }
    }

    // --- Reminders Operations ---
    fun addReminder(taskId: String, triggerTime: Long, label: String = "Reminder") {
        viewModelScope.launch {
            val rem = Reminder(id = UUID.randomUUID().toString(), taskId = taskId, triggerTime = triggerTime, label = label)
            repository.insertReminder(rem)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- Notes Operations ---
    fun createNote(title: String, content: String, color: Int = 0) {
        viewModelScope.launch {
            val note = Note(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                colorHex = color
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note.copy(modifiedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- Focus Mode & Pomodoro Timer Logic ---
    fun startFocusTimer(task: Task?) {
        focusTask = task
        isFocusTimerRunning = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isFocusTimerRunning && focusTimerLeftSeconds > 0) {
                delay(1000)
                focusTimerLeftSeconds--
                if (focusTask != null) {
                    // Log focus seconds
                    val currentTask = focusTask!!
                    focusTask = currentTask.copy(timeSpentSeconds = currentTask.timeSpentSeconds + 1)
                    repository.updateTask(focusTask!!, logAction = false)
                }
            }
            if (focusTimerLeftSeconds == 0) {
                onFocusTimerFinished()
            }
        }
    }

    fun pauseFocusTimer() {
        isFocusTimerRunning = false
        timerJob?.cancel()
    }

    fun resetFocusTimer() {
        pauseFocusTimer()
        focusTimerLeftSeconds = when (pomodoroMode) {
            PomodoroMode.WORK -> 25 * 60
            PomodoroMode.SHORT_BREAK -> 5 * 60
            PomodoroMode.LONG_BREAK -> 15 * 60
        }
        focusTimerTotalSeconds = focusTimerLeftSeconds
    }

    private fun onFocusTimerFinished() {
        isFocusTimerRunning = false
        viewModelScope.launch {
            if (pomodoroMode == PomodoroMode.WORK) {
                pomodoroCyclesCompleted++
                repository.logActivity(focusTask?.id, "FOCUS_COMPLETED", "Completed Pomodoro work block.")
                if (pomodoroCyclesCompleted % 4 == 0) {
                    pomodoroMode = PomodoroMode.LONG_BREAK
                    focusTimerLeftSeconds = 15 * 60
                } else {
                    pomodoroMode = PomodoroMode.SHORT_BREAK
                    focusTimerLeftSeconds = 5 * 60
                }
            } else {
                repository.logActivity(null, "BREAK_COMPLETED", "Completed relaxing break block.")
                pomodoroMode = PomodoroMode.WORK
                focusTimerLeftSeconds = 25 * 60
            }
            focusTimerTotalSeconds = focusTimerLeftSeconds
        }
    }

    fun setPomodoroTimerPreset(minutes: Int) {
        pauseFocusTimer()
        focusTimerLeftSeconds = minutes * 60
        focusTimerTotalSeconds = focusTimerLeftSeconds
    }

    // --- Import & Export (Backup & Restore) ---
    fun exportBackup(context: Context): String? {
        try {
            val tasksList = allTasks.value
            val foldersList = folders.value
            val notesList = notes.value

            val json = JSONObject()
            val tasksArray = JSONArray()
            for (t in tasksList) {
                val tObj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("notes", t.notes)
                    put("priority", t.priority)
                    put("folderId", t.folderId)
                    put("dueDate", t.dueDate ?: 0L)
                    put("dueTime", t.dueTime ?: "")
                    put("completionPercentage", t.completionPercentage)
                    put("status", t.status)
                    put("tags", t.tags)
                    put("isPinned", t.isPinned)
                    put("isFavorite", t.isFavorite)
                    put("isDeleted", t.isDeleted)
                    put("timeSpentSeconds", t.timeSpentSeconds)
                }
                tasksArray.put(tObj)
            }
            json.put("tasks", tasksArray)

            val foldersArray = JSONArray()
            for (f in foldersList) {
                val fObj = JSONObject().apply {
                    put("id", f.id)
                    put("name", f.name)
                    put("colorHex", f.colorHex)
                    put("iconName", f.iconName)
                    put("isSystem", f.isSystem)
                }
                foldersArray.put(fObj)
            }
            json.put("folders", foldersArray)

            val notesArray = JSONArray()
            for (n in notesList) {
                val nObj = JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("colorHex", n.colorHex)
                }
                notesArray.put(nObj)
            }
            json.put("notes", notesArray)

            val backupString = json.toString(4)
            val file = File(context.filesDir, "arbab_tasks_backup.json")
            file.writeText(backupString)
            return backupString
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun importBackup(backupJsonString: String): Boolean {
        return try {
            val json = JSONObject(backupJsonString)

            if (json.has("folders")) {
                val foldersArray = json.getJSONArray("folders")
                viewModelScope.launch {
                    for (i in 0 until foldersArray.length()) {
                        val f = foldersArray.getJSONObject(i)
                        repository.insertFolder(
                            Folder(
                                id = f.getString("id"),
                                name = f.getString("name"),
                                colorHex = f.getInt("colorHex"),
                                iconName = f.getString("iconName"),
                                isSystem = f.optBoolean("isSystem", false)
                            )
                        )
                    }
                }
            }

            if (json.has("tasks")) {
                val tasksArray = json.getJSONArray("tasks")
                viewModelScope.launch {
                    for (i in 0 until tasksArray.length()) {
                        val t = tasksArray.getJSONObject(i)
                        val dueDateRaw = t.optLong("dueDate", 0L)
                        repository.insertTask(
                            Task(
                                id = t.getString("id"),
                                title = t.getString("title"),
                                description = t.optString("description", ""),
                                notes = t.optString("notes", ""),
                                priority = t.getString("priority"),
                                folderId = t.getString("folderId"),
                                dueDate = if (dueDateRaw == 0L) null else dueDateRaw,
                                dueTime = t.optString("dueTime", null).let { if (it.isNullOrEmpty()) null else it },
                                completionPercentage = t.optInt("completionPercentage", 0),
                                status = t.getString("status"),
                                tags = t.optString("tags", ""),
                                isPinned = t.optBoolean("isPinned", false),
                                isFavorite = t.optBoolean("isFavorite", false),
                                isDeleted = t.optBoolean("isDeleted", false),
                                timeSpentSeconds = t.optLong("timeSpentSeconds", 0L)
                            ),
                            logAction = false
                        )
                    }
                }
            }

            if (json.has("notes")) {
                val notesArray = json.getJSONArray("notes")
                viewModelScope.launch {
                    for (i in 0 until notesArray.length()) {
                        val n = notesArray.getJSONObject(i)
                        repository.insertNote(
                            Note(
                                id = n.getString("id"),
                                title = n.getString("title"),
                                content = n.getString("content"),
                                colorHex = n.optInt("colorHex", 0)
                            )
                        )
                    }
                }
            }

            viewModelScope.launch {
                repository.logActivity(null, "BACKUP_RESTORED", "Restored database from JSON backup successfully.")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- Helper Utilities ---
    private fun isToday(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDay = cal.get(Calendar.DAY_OF_YEAR)

        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay
    }

    private fun isUpcoming(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        return timestamp > today
    }

    private fun isOverdue(timestamp: Long?, status: String): Boolean {
        if (timestamp == null) return false
        if (status == "COMPLETED" || status == "CANCELLED" || status == "ARCHIVED") return false
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return timestamp < startOfToday
    }

    // --- AI Agent Chat States ---
    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val sender: Sender,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class Sender { USER, AGENT }

    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = Sender.AGENT,
                text = "Hello! I am your Arbab Tasks AI Agent. I can help you manage your tasks, create subtasks, build schedules, and summarize notes in real-time. Try asking me to 'Create a high priority task to review court files tomorrow'!"
            )
        )
    )
    val aiChatMessages: StateFlow<List<ChatMessage>> = _aiChatMessages.asStateFlow()

    var isAiThinking by mutableStateOf(false)
        private set

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        
        // Add user message to history
        val userMsg = ChatMessage(sender = Sender.USER, text = prompt)
        _aiChatMessages.value = _aiChatMessages.value + userMsg
        
        isAiThinking = true
        
        viewModelScope.launch {
            try {
                // Get fresh context to pass to Gemini
                val activeTasks = allTasks.value
                val foldersList = folders.value
                val notesList = notes.value
                
                val foldersContext = foldersList.joinToString("\n") { "- Folder: ${it.name} (ID: ${it.id})" }
                val tasksContext = activeTasks.filter { !it.isDeleted && it.status != "COMPLETED" }.joinToString("\n") { 
                    "- [ID: ${it.id}] \"${it.title}\" (Priority: ${it.priority}, Status: ${it.status}, FolderID: ${it.folderId}, Due: ${it.dueTime ?: "No Time"})"
                }
                val notesContext = notesList.joinToString("\n") { "- Note: \"${it.title}\"" }
                
                val systemInstruction = """
                    You are Arbab Tasks AI Agent, a helpful, highly integrated, elite productivity companion.
                    The current date and time is: 2026-07-14.
                    
                    Here are the user's current folders:
                    $foldersContext
                    
                    Here are the user's active/pending tasks:
                    $tasksContext
                    
                    Here are the user's notes:
                    $notesContext
                    
                    You MUST respond strictly in the following JSON format:
                    {
                      "reply": "friendly, conversational text reply explaining what you've done or answering the user",
                      "action": {
                        "type": "CREATE_TASK" | "CREATE_NOTE" | "COMPLETE_TASK" | null,
                        "title": "required if type is CREATE_TASK or CREATE_NOTE",
                        "description": "optional task description",
                        "priority": "CRITICAL" | "HIGH" | "MEDIUM" | "NORMAL" | "LOW" (optional for task, default is NORMAL),
                        "folderId": "optional, default is 'personal'",
                        "content": "required if type is CREATE_NOTE",
                        "taskId": "required if type is COMPLETE_TASK"
                      }
                    }
                    
                    If the user wants you to do something (like create a task, note, or complete a task), fulfill it by setting the "action" field and explaining it in the "reply" field.
                    If the user is just chatting or asking a question, keep "action" null.
                    
                    RULES:
                    1. ALWAYS respond in valid JSON format.
                    2. Do NOT wrap your JSON response in markdown code blocks like ```json ... ```. Just return the raw JSON string so we can parse it easily.
                    3. Ensure the reply is written in a friendly, conversational, yet concise tone.
                """.trimIndent()
                
                // Construct history Pair list
                val historyList = mutableListOf<Pair<String, String>>()
                // Only use the last 6 messages to keep context window manageable
                val recentMessages = _aiChatMessages.value.takeLast(7).dropLast(1) // exclude current user message
                var i = 0
                while (i < recentMessages.size - 1) {
                    val turn1 = recentMessages[i]
                    val turn2 = recentMessages[i+1]
                    if (turn1.sender == Sender.USER && turn2.sender == Sender.AGENT) {
                        historyList.add(Pair(turn1.text, turn2.text))
                    }
                    i++
                }

                val rawResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    GeminiService.generateContent(systemInstruction, prompt, historyList)
                }
                
                val cleanedJson = cleanJsonResponse(rawResponse)
                
                val json = JSONObject(cleanedJson)
                val replyText = json.optString("reply", "I processed your request, but couldn't formulate a text response.")
                val actionObj = json.optJSONObject("action")
                
                // Add agent reply to chat first
                _aiChatMessages.value = _aiChatMessages.value + ChatMessage(sender = Sender.AGENT, text = replyText)
                
                // Process Action if exists
                if (actionObj != null && !actionObj.isNull("type")) {
                    val type = actionObj.optString("type")
                    when (type) {
                        "CREATE_TASK" -> {
                            val title = actionObj.optString("title", "AI Task")
                            val description = actionObj.optString("description", "")
                            val priority = actionObj.optString("priority", "NORMAL")
                            val folderId = actionObj.optString("folderId", defaultFolderId)
                            createTask(
                                title = title,
                                description = description,
                                priority = priority,
                                folderId = folderId
                            )
                        }
                        "CREATE_NOTE" -> {
                            val title = actionObj.optString("title", "AI Note")
                            val content = actionObj.optString("content", "")
                            createNote(
                                title = title,
                                content = content
                            )
                        }
                        "COMPLETE_TASK" -> {
                            val taskId = actionObj.optString("taskId")
                            if (!taskId.isNullOrBlank()) {
                                val taskToComplete = activeTasks.find { it.id == taskId }
                                if (taskToComplete != null) {
                                    updateTaskStatus(taskToComplete, "COMPLETED")
                                }
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                _aiChatMessages.value = _aiChatMessages.value + ChatMessage(sender = Sender.AGENT, text = "I encountered an error while processing: ${e.message ?: "Unknown error"}")
            } finally {
                isAiThinking = false
            }
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
