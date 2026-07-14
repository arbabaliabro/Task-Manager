package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Database and Repository Initialization
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = TaskRepository(database)
        val factory = MainViewModelFactory(application, repository)

        setContent {
            // 2. ViewModel Initialization
            val viewModel: MainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

            val isDarkTheme = when (viewModel.darkThemeSetting) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                accentColor = Color(viewModel.accentColorHex)
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

                // 3. Task Editor Trigger State
                var showEditorDialog by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                if (currentRoute == "dashboard") {
                                    Column {
                                        Text(
                                            text = "Arbab Tasks",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "PWA • OFFLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 10.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        text = when (currentRoute) {
                                            "tasks" -> "Workspace Tasks"
                                            "ai" -> "AI Workspace Agent"
                                            "calendar" -> "Schedule Agenda"
                                            "focus" -> "Deep Focus Loops"
                                            "notes" -> "Notebook Workspace"
                                            "settings" -> "App Preferences"
                                            else -> "Arbab Tasks"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            },
                            actions = {
                                if (currentRoute != "settings") {
                                    IconButton(
                                        onClick = { navController.navigate("settings") },
                                        modifier = Modifier.testTag("app_settings_button")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        // Custom bottom navigation bar respecting system navigation safe zones
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation_bar"),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "dashboard",
                                onClick = { navController.navigate("dashboard") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                                label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "tasks",
                                onClick = { navController.navigate("tasks") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.CheckCircle, "Tasks") },
                                label = { Text("Tasks", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "ai",
                                onClick = { navController.navigate("ai") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.AutoAwesome, "AI Agent") },
                                label = { Text("AI Agent", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "calendar",
                                onClick = { navController.navigate("calendar") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.CalendarToday, "Calendar") },
                                label = { Text("Calendar", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "focus",
                                onClick = { navController.navigate("focus") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.Timer, "Focus") },
                                label = { Text("Focus", style = MaterialTheme.typography.labelSmall) }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "notes",
                                onClick = { navController.navigate("notes") { popUpTo("dashboard") { saveState = true } } },
                                icon = { Icon(Icons.Default.Book, "Notes") },
                                label = { Text("Notes", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    },
                    floatingActionButton = {
                        // Quick Add FAB on Tasks & Dashboard Screen
                        if (currentRoute == "tasks" || currentRoute == "dashboard") {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.editingTask = null
                                    showEditorDialog = true
                                },
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .testTag("floating_quick_add_button"),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Quick Add Task")
                            }
                        }
                    }
                ) { innerPadding ->
                    // 4. Main NavHost Routing
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTasks = { navController.navigate("tasks") },
                                onNavigateToFocus = { navController.navigate("focus") },
                                onQuickAddTask = {
                                    viewModel.editingTask = null
                                    showEditorDialog = true
                                }
                            )
                        }

                        composable("tasks") {
                            TasksListScreen(
                                viewModel = viewModel,
                                onEditTask = { task ->
                                    showEditorDialog = true
                                }
                            )
                        }

                        composable("ai") {
                            AiAgentScreen(viewModel = viewModel)
                        }

                        composable("calendar") {
                            CalendarScreen(
                                viewModel = viewModel,
                                onEditTask = { task ->
                                    viewModel.loadEditingTaskDetails(task)
                                    showEditorDialog = true
                                }
                            )
                        }

                        composable("focus") {
                            FocusTimerScreen(viewModel = viewModel)
                        }

                        composable("notes") {
                            NotesScreen(viewModel = viewModel)
                        }

                        composable("settings") {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }

                    // 5. Shared Task Editor workspace dialog overlays
                    if (showEditorDialog) {
                        TaskEditorSheet(
                            viewModel = viewModel,
                            onDismiss = { showEditorDialog = false }
                        )
                    }
                }
            }
        }
    }
}
