package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(private val db: AppDatabase) {
    private val folderDao = db.folderDao()
    private val taskDao = db.taskDao()
    private val subtaskDao = db.subtaskDao()
    private val attachmentDao = db.attachmentDao()
    private val commentDao = db.commentDao()
    private val reminderDao = db.reminderDao()
    private val activityLogDao = db.activityLogDao()
    private val noteDao = db.noteDao()

    // --- Folders ---
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    suspend fun insertFolder(folder: Folder) = folderDao.insertFolder(folder)
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
    suspend fun getFolderById(id: String) = folderDao.getFolderById(id)

    // --- Tasks ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasksReactive()
    val deletedTasks: Flow<List<Task>> = taskDao.getDeletedTasksReactive()
    suspend fun getTaskById(id: String) = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task, logAction: Boolean = true) {
        taskDao.insertTask(task)
        if (logAction) {
            logActivity(task.id, "TASK_CREATED", "Created task: \"${task.title}\"")
        }
    }

    suspend fun updateTask(task: Task, logAction: Boolean = true) {
        taskDao.updateTask(task)
        if (logAction) {
            logActivity(task.id, "TASK_UPDATED", "Updated task details for: \"${task.title}\"")
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        logActivity(task.id, "TASK_DELETED", "Permanently deleted task: \"${task.title}\"")
    }

    suspend fun softDeleteTask(id: String) {
        val task = taskDao.getTaskById(id)
        taskDao.softDeleteTask(id)
        task?.let {
            logActivity(id, "TASK_SOFT_DELETED", "Moved \"${it.title}\" to recycle bin.")
        }
    }

    suspend fun restoreTask(id: String) {
        val task = taskDao.getTaskById(id)
        taskDao.restoreTask(id)
        task?.let {
            logActivity(id, "TASK_RESTORED", "Restored \"${it.title}\" from recycle bin.")
        }
    }

    // --- Subtasks ---
    fun getSubtasksForTask(taskId: String): Flow<List<Subtask>> = subtaskDao.getSubtasksForTask(taskId)
    suspend fun insertSubtask(subtask: Subtask) = subtaskDao.insertSubtask(subtask)
    suspend fun deleteSubtask(subtask: Subtask) = subtaskDao.deleteSubtask(subtask)
    suspend fun deleteSubtasksForTask(taskId: String) = subtaskDao.deleteSubtasksForTask(taskId)

    // --- Attachments ---
    fun getAttachmentsForTask(taskId: String): Flow<List<Attachment>> = attachmentDao.getAttachmentsForTask(taskId)
    suspend fun insertAttachment(attachment: Attachment) = attachmentDao.insertAttachment(attachment)
    suspend fun deleteAttachment(attachment: Attachment) = attachmentDao.deleteAttachment(attachment)

    // --- Comments ---
    fun getCommentsForTask(taskId: String): Flow<List<Comment>> = commentDao.getCommentsForTask(taskId)
    suspend fun insertComment(comment: Comment) = commentDao.insertComment(comment)
    suspend fun deleteComment(comment: Comment) = commentDao.deleteComment(comment)

    // --- Reminders ---
    fun getRemindersForTask(taskId: String): Flow<List<Reminder>> = reminderDao.getRemindersForTask(taskId)
    val activeReminders: Flow<List<Reminder>> = reminderDao.getActiveReminders()
    suspend fun insertReminder(reminder: Reminder) = reminderDao.insertReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
    suspend fun markReminderTriggered(id: String) = reminderDao.markTriggered(id)

    // --- Activity Logs ---
    val allLogs: Flow<List<ActivityLog>> = activityLogDao.getActivityLogsReactive()
    suspend fun logActivity(taskId: String?, action: String, details: String) {
        activityLogDao.insertActivityLog(
            ActivityLog(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                action = action,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    suspend fun clearAllLogs() = activityLogDao.clearAllLogs()

    // --- Notes ---
    val allNotes: Flow<List<Note>> = noteDao.getAllNotesReactive()
    suspend fun getNoteById(id: String) = noteDao.getNoteById(id)
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
}
