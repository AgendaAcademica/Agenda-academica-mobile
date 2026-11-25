package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.AcademicActivity
import com.example.data.model.Discipline
import com.example.data.auth.AuthState
import com.example.ui.dialogs.ActivityDetailDialog
import com.example.ui.dialogs.AddEditActivityDialog
import com.example.ui.dialogs.AddEditDisciplineDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.widget.AcademicDayAppWidgetProvider

class MainActivity : ComponentActivity() {
    private val viewModel: AcademicViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { getSharedPreferences("appearance_preferences", MODE_PRIVATE) }
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by remember {
                mutableStateOf(preferences.getBoolean("dark_theme", systemDarkTheme))
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                val navigationBarColor = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect {
                    window.navigationBarColor = navigationBarColor
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !darkTheme
                }
                AcademicAgendaRoot(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    isDarkTheme = darkTheme,
                    onToggleTheme = {
                            darkTheme = !darkTheme
                            preferences.edit().putBoolean("dark_theme", darkTheme).apply()
                            AcademicDayAppWidgetProvider.updateAllWidgets(this@MainActivity)
                        }
                    )
            }
        }
    }
}

@Composable
private fun AcademicAgendaRoot(
    viewModel: AcademicViewModel,
    authViewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val authState by authViewModel.state.collectAsState()
    val isSubmitting by authViewModel.isSubmitting.collectAsState()
    val authMessage by authViewModel.message.collectAsState()

    when (authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        AuthState.SignedOut, AuthState.ConfigurationRequired -> LoginScreen(
            isSubmitting = isSubmitting,
            message = authMessage,
            configurationRequired = authState == AuthState.ConfigurationRequired,
            onContinue = authViewModel::continueWithEmail,
            onResetPassword = authViewModel::resetPassword,
            onClearMessage = authViewModel::clearMessage
        )
        is AuthState.SignedIn -> MainApp(
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSignOut = authViewModel::signOut,
            accountEmail = (authState as AuthState.SignedIn).user.email
        )
    }
}

@Composable
fun MainApp(
    viewModel: AcademicViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit = {},
    accountEmail: String? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val selectedActivity by viewModel.selectedActivity.collectAsState()

    var showAddActivityDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<AcademicActivity?>(null) }
    var preselectedDisciplineIdForNewActivity by remember { mutableStateOf<Long?>(null) }

    var showAddDisciplineDialog by remember { mutableStateOf(false) }
    var disciplineToEdit by remember { mutableStateOf<Discipline?>(null) }

    // Request notification permission if Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Disciplines,
        Screen.ImportDocument,
        Screen.Settings
    )

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_app_scaffold"),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 0.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route ||
                            (screen == Screen.Settings && currentRoute in listOf(Screen.WidgetStudio.route, Screen.Reminders.route))
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            selected = isSelected,
                            onClick = {
                                navigateToTab(screen.route)
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToImport = { navigateToTab(Screen.ImportDocument.route) },
                    onNavigateToDisciplines = { navigateToTab(Screen.Disciplines.route) },
                    onNavigateToReminders = { navigateToTab(Screen.Reminders.route) },
                    onNavigateToWidget = { navigateToTab(Screen.WidgetStudio.route) },
                    onNavigateToSettings = { navigateToTab(Screen.Settings.route) },
                    onOpenAddActivity = {
                        preselectedDisciplineIdForNewActivity = null
                        activityToEdit = null
                        showAddActivityDialog = true
                    }
                )
            }

            composable(Screen.Disciplines.route) {
                DisciplinesScreen(
                    viewModel = viewModel,
                    onOpenAddDiscipline = {
                        disciplineToEdit = null
                        showAddDisciplineDialog = true
                    },
                    onOpenEditDiscipline = { disc ->
                        disciplineToEdit = disc
                        showAddDisciplineDialog = true
                    },
                    onOpenAddActivityForDiscipline = { discId ->
                        preselectedDisciplineIdForNewActivity = discId
                        activityToEdit = null
                        showAddActivityDialog = true
                    }
                )
            }

            composable(Screen.ImportDocument.route) {
                DocumentImportScreen(
                    viewModel = viewModel,
                    onFinishedImport = {
                        navigateToTab(Screen.Dashboard.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel, isDarkTheme, onToggleTheme,
                    onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                    onNavigateToWidget = { navController.navigate(Screen.WidgetStudio.route) },
                    onSignOut = onSignOut, accountEmail = accountEmail)
            }
            composable(Screen.WidgetStudio.route) {
                WidgetStudioScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onNavigateToHome = { navigateToTab(Screen.Settings.route) }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersCenterScreen(viewModel = viewModel,
                    onNavigateToSettings = { navigateToTab(Screen.Settings.route) })
            }
        }
    }

    // Detail dialog for clicked activity
    if (selectedActivity != null) {
        val disc = disciplines.find { it.id == selectedActivity!!.disciplineId }
        ActivityDetailDialog(
            activity = selectedActivity!!,
            discipline = disc,
            onDismiss = { viewModel.selectActivity(null) },
            onStatusChange = { newStatus -> viewModel.updateActivityStatus(selectedActivity!!.id, newStatus) },
            onUpdate = { updated ->
                viewModel.saveActivity(updated)
                viewModel.selectActivity(updated)
            },
            onEdit = {
                activityToEdit = selectedActivity
                viewModel.selectActivity(null)
                showAddActivityDialog = true
            },
            onDelete = { viewModel.deleteActivity(selectedActivity!!.id) }
        )
    }

    // Add / Edit Activity Dialog
    if (showAddActivityDialog) {
        AddEditActivityDialog(
            initialActivity = activityToEdit,
            disciplines = disciplines,
            preselectedDisciplineId = preselectedDisciplineIdForNewActivity,
            onDismiss = {
                showAddActivityDialog = false
                activityToEdit = null
            },
            onSave = { saved ->
                viewModel.saveActivity(saved)
                showAddActivityDialog = false
                activityToEdit = null
            }
        )
    }

    // Add / Edit Discipline Dialog
    if (showAddDisciplineDialog) {
        AddEditDisciplineDialog(
            initialDiscipline = disciplineToEdit,
            existingDisciplines = disciplines,
            existingActivities = activities,
            onDismiss = {
                showAddDisciplineDialog = false
                disciplineToEdit = null
            },
            onSave = { saved, generatedClasses ->
                viewModel.saveDisciplineWithClasses(saved, generatedClasses)
                showAddDisciplineDialog = false
                disciplineToEdit = null
            }
        )
    }
}
