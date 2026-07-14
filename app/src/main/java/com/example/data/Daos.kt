package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isSystem DESC, name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): Folder?
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isPinned DESC, dueDate ASC, createdDate DESC")
    fun getAllTasksReactive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTasksReactive(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTask(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 0, deletedAt = null WHERE id = :id")
    suspend fun restoreTask(id: String)
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId")
    fun getSubtasksForTask(taskId: String): Flow<List<Subtask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: String)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getAttachmentsForTask(taskId: String): Flow<List<Attachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getCommentsForTask(taskId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Delete
    suspend fun deleteComment(comment: Comment)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY triggerTime ASC")
    fun getRemindersForTask(taskId: String): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isTriggered = 0 ORDER BY triggerTime ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isTriggered = 1 WHERE id = :id")
    suspend fun markTriggered(id: String)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getActivityLogsReactive(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY modifiedAt DESC")
    fun getAllNotesReactive(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?
}
