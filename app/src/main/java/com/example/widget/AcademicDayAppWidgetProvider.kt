package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AcademicDayAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AcademicDayAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.academic_widget_layout)

            // Setup click to open main app directly on home screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_item_1, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_item_2, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_item_3, pendingIntent)

            val currentDateStr = runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            }.getOrDefault("2026-09-01")

            val headerDisplayDate = runCatching {
                val cal = java.util.Calendar.getInstance()
                val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val monthNames = arrayOf(
                    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
                    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
                )
                val month = monthNames[cal.get(java.util.Calendar.MONTH)]
                "Hoje — ${String.format("%02d", day)} de $month"
            }.getOrDefault("Hoje — Agenda Acadêmica")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val settings = WidgetPreferences.getSettings(context)
                    val allDisciplines = db.disciplineDao().getAllDisciplines().first().associateBy { it.id }
                    var activities = db.activityDao().getAllActivities().first()

                    // Filter for today or upcoming
                    var todayActivities = activities.filter { act ->
                        act.date == currentDateStr || act.dueDate == currentDateStr
                    }
                    if (todayActivities.isEmpty()) {
                        // Fallback to active semester date if today has none
                        todayActivities = activities.filter { act ->
                            act.date == "2026-09-01" || act.dueDate == "2026-09-01"
                        }
                    }

                    // Apply widget settings
                    if (settings.showOnlyPending) {
                        todayActivities = todayActivities.filter { it.status != ActivityStatus.CONCLUIDA }
                    }
                    if (!settings.showClasses) {
                        todayActivities = todayActivities.filter { it.activityType != ActivityType.AULA }
                    }
                    if (settings.selectedDisciplineIds.isNotEmpty()) {
                        todayActivities = todayActivities.filter { settings.selectedDisciplineIds.contains(it.disciplineId) }
                    }

                    val pendingCount = todayActivities.count { it.status != ActivityStatus.CONCLUIDA }
                    views.setTextViewText(R.id.widget_title, headerDisplayDate)
                    views.setTextViewText(R.id.widget_count, "$pendingCount pendentes")

                    // Populate slot 1
                    val item1 = todayActivities.getOrNull(0)
                    if (item1 != null) {
                        views.setViewVisibility(R.id.widget_item_1, View.VISIBLE)
                        val disc = allDisciplines[item1.disciplineId]
                        val discName = disc?.name ?: "Geral"
                        val discColor = runCatching { Color.parseColor(disc?.colorHex ?: "#2563EB") }.getOrDefault(Color.BLUE)

                        val timePrefix = if (settings.showTimes) "${item1.time} — " else ""
                        views.setTextViewText(R.id.widget_item_1_subject_time, "$timePrefix$discName")
                        views.setTextColor(R.id.widget_item_1_subject_time, discColor)
                        views.setTextViewText(R.id.widget_item_1_title, "${item1.activityType.displayName}: ${item1.title}")
                        views.setInt(R.id.widget_item_1_color, "setBackgroundColor", discColor)
                    } else {
                        views.setViewVisibility(R.id.widget_item_1, View.GONE)
                    }

                    // Populate slot 2
                    val item2 = todayActivities.getOrNull(1)
                    if (item2 != null && settings.maxItems >= 2) {
                        views.setViewVisibility(R.id.widget_item_2, View.VISIBLE)
                        val disc = allDisciplines[item2.disciplineId]
                        val discName = disc?.name ?: "Geral"
                        val discColor = runCatching { Color.parseColor(disc?.colorHex ?: "#16A34A") }.getOrDefault(Color.GREEN)

                        val timePrefix = if (settings.showTimes) "${item2.time} — " else ""
                        views.setTextViewText(R.id.widget_item_2_subject_time, "$timePrefix$discName")
                        views.setTextColor(R.id.widget_item_2_subject_time, discColor)
                        views.setTextViewText(R.id.widget_item_2_title, "${item2.activityType.displayName}: ${item2.title}")
                        views.setInt(R.id.widget_item_2_color, "setBackgroundColor", discColor)
                    } else {
                        views.setViewVisibility(R.id.widget_item_2, View.GONE)
                    }

                    // Populate slot 3
                    val item3 = todayActivities.getOrNull(2)
                    if (item3 != null && settings.maxItems >= 3) {
                        views.setViewVisibility(R.id.widget_item_3, View.VISIBLE)
                        val disc = allDisciplines[item3.disciplineId]
                        val discName = disc?.name ?: "Geral"
                        val discColor = runCatching { Color.parseColor(disc?.colorHex ?: "#EA580C") }.getOrDefault(Color.parseColor("#EA580C"))

                        val timePrefix = if (settings.showTimes) "${item3.time} — " else ""
                        views.setTextViewText(R.id.widget_item_3_subject_time, "$timePrefix$discName")
                        views.setTextColor(R.id.widget_item_3_subject_time, discColor)
                        views.setTextViewText(R.id.widget_item_3_title, "${item3.activityType.displayName}: ${item3.title}")
                        views.setInt(R.id.widget_item_3_color, "setBackgroundColor", discColor)
                    } else {
                        views.setViewVisibility(R.id.widget_item_3, View.GONE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
