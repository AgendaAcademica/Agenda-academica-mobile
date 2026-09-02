package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Início", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Calendar : Screen("calendar", "Calendário", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Disciplines : Screen("disciplines", "Disciplinas", Icons.Filled.School, Icons.Outlined.School)
    object ImportDocument : Screen("import", "Importar IA", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner)
    object WidgetStudio : Screen("widget", "Widget", Icons.Filled.Widgets, Icons.Outlined.Widgets)
    object Reminders : Screen("reminders", "Lembretes", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}
