package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: Int,
    val iconName: String,
    val isSystem: Boolean = false
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val notes: String = "", // Supports markdown/rich content
    val priority: String, // CRITICAL, HIGH, MEDIUM, NORMAL, LOW
    val folderId: String,
    val dueDate: Long? = null, // Epoch timestamp (ms)
    val dueTime: String? = null, // "HH:mm" format
    val startDate: Long? = null,
    val estimatedDurationMinutes: Int = 0,
    val completionPercentage: Int = 0, // 0 - 100
    val status: String, // PENDING, IN_PROGRESS, ON_HOLD, WAITING, COMPLETED, CANCELLED, ARCHIVED
    val tags: String = "", // Comma-separated list of tags
    val colorLabel: Int = 0, // Visual color override
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false, // Soft-delete recycle bin support
    val deletedAt: Long? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val recurrence: String = "NONE", // DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM, NONE
    val timeSpentSeconds: Long = 0,
    val location: String? = null
)

@Entity(tableName = "subtasks")
data class Subtask(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "NORMAL",
    val dueDate: Long? = null,
    val progress: Int = 0 // 0 or 100, or gradual progress
)

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey val id: String,
    val taskId: String,
    val fileName: String,
    val fileType: String, // "PDF", "IMAGE", "LINK", etc.
    val fileUri: String, // URL, File path, or Drive link
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val taskId: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String,
    val taskId: String,
    val triggerTime: Long, // Epoch timestamp (ms)
    val isTriggered: Boolean = false,
    val isSnoozed: Boolean = false,
    val label: String = "Reminder"
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val taskId: String?,
    val action: String, // e.g. "TASK_CREATED", "STATUS_UPDATED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val title: String,
    val content: String, // Markdown text
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val colorHex: Int = 0
)
