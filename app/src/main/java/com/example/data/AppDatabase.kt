package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        Folder::class,
        Task::class,
        Subtask::class,
        Attachment::class,
        Comment::class,
        Reminder::class,
        ActivityLog::class,
        Note::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun commentDao(): CommentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arbab_tasks_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultFolders(database.folderDao())
                    populateDefaultNotes(database.noteDao())
                }
            }
        }

        private suspend fun populateDefaultFolders(folderDao: FolderDao) {
            val defaults = listOf(
                Folder("office", "Office Work", 0xFFE53935.toInt(), "work", true),
                Folder("court", "Court Work", 0xFF5E35B1.toInt(), "gavel", true),
                Folder("prep", "Civil Judge Prep", 0xFF1E88E5.toInt(), "school", true),
                Folder("personal", "Personal", 0xFF43A047.toInt(), "person", true),
                Folder("fitness", "Fitness & Health", 0xFF00ACC1.toInt(), "fitness_center", true),
                Folder("finance", "Finance", 0xFFFFB300.toInt(), "attach_money", true),
                Folder("study", "Study", 0xFFD81B60.toInt(), "book", true),
                Folder("shopping", "Shopping", 0xFFF4511E.toInt(), "shopping_cart", true),
                Folder("projects", "Projects", 0xFF3949AB.toInt(), "assignment", true)
            )
            for (folder in defaults) {
                folderDao.insertFolder(folder)
            }
        }

        private suspend fun populateDefaultNotes(noteDao: NoteDao) {
            val welcomeNote = Note(
                id = UUID.randomUUID().toString(),
                title = "Welcome to Arbab Tasks! 🚀",
                content = """
                    # Arbab Tasks
                    
                    Your modern, secure, fast, and offline-capable personal productivity powerhouse!
                    
                    ## Key Features
                    - **Dashboard**: Track daily milestones, task progress, category distributions, and streaks.
                    - **Task Detail**: Add multiple subtasks, timers, multiple reminders, attachments, and comments.
                    - **Calendar**: Day, Week, Month, and Agenda perspectives.
                    - **Pomodoro Timer**: Build focus loops right within the app.
                    - **Rich Markdown Notes**: This note is a live markdown note! You can edit it.
                    
                    ## Markdown Sandbox
                    - Use `#` for headings
                    - Use `**bold**` or `*italic*` for styling
                    - Checklists are supported:
                      - [x] Create some tasks
                      - [ ] Set a focus timer
                      
                    Enjoy staying productive!
                """.trimIndent(),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                colorHex = 0xFF1E88E5.toInt()
            )
            noteDao.insertNote(welcomeNote)
        }
    }
}
