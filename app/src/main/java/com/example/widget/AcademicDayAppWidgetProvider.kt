package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AcademicDayAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { appWidgetId ->
                    updateAppWidgetNow(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val itemContainers = intArrayOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3, R.id.widget_item_4, R.id.widget_item_5)
        private val itemColors = intArrayOf(R.id.widget_item_1_color, R.id.widget_item_2_color, R.id.widget_item_3_color, R.id.widget_item_4_color, R.id.widget_item_5_color)
        private val itemSubjects = intArrayOf(R.id.widget_item_1_subject_time, R.id.widget_item_2_subject_time, R.id.widget_item_3_subject_time, R.id.widget_item_4_subject_time, R.id.widget_item_5_subject_time)
        private val itemTitles = intArrayOf(R.id.widget_item_1_title, R.id.widget_item_2_title, R.id.widget_item_3_title, R.id.widget_item_4_title, R.id.widget_item_5_title)
        private val itemLocations = intArrayOf(R.id.widget_item_1_location, R.id.widget_item_2_location, R.id.widget_item_3_location, R.id.widget_item_4_location, R.id.widget_item_5_location)
        private val itemDeadlines = intArrayOf(R.id.widget_item_1_deadline, R.id.widget_item_2_deadline, R.id.widget_item_3_deadline, R.id.widget_item_4_deadline, R.id.widget_item_5_deadline)

        private data class WidgetPalette(
            val background: Int,
            val itemBackground: Int,
            val primaryText: Int,
            val secondaryText: Int,
            val deadlineText: Int
        )

        fun updateAllWidgets(context: Context) {
            val applicationContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val manager = AppWidgetManager.getInstance(applicationContext)
                val provider = ComponentName(applicationContext, AcademicDayAppWidgetProvider::class.java)
                manager.getAppWidgetIds(provider).forEach { appWidgetId ->
                    updateAppWidgetNow(applicationContext, manager, appWidgetId)
                }
            }
        }

        private suspend fun updateAppWidgetNow(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.academic_widget_layout)
            val palette = getPalette(context)
            applyTheme(views, palette)
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
            itemContainers.forEach { views.setOnClickPendingIntent(it, openAppIntent) }

            try {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val calendar = java.util.Calendar.getInstance()
                val monthNames = arrayOf(
                    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
                    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
                )
                val header = "Hoje — ${String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))} de ${monthNames[calendar.get(java.util.Calendar.MONTH)]}"

                val database = AppDatabase.getDatabase(context)
                val settings = WidgetPreferences.getSettings(context)
                val disciplines = database.disciplineDao().getAllDisciplines().first().associateBy { it.id }
                var activities = database.activityDao().getAllActivities().first()
                    .filter { it.date == today || it.dueDate == today }

                if (settings.showOnlyPending) activities = activities.filter { it.status != ActivityStatus.CONCLUIDA }
                if (!settings.showClasses) activities = activities.filter { it.activityType != ActivityType.AULA }
                if (settings.selectedDisciplineIds.isNotEmpty()) {
                    activities = activities.filter { it.disciplineId in settings.selectedDisciplineIds }
                }

                views.setTextViewText(R.id.widget_title, header)
                val visibleActivities = activities.take(settings.maxItems.coerceIn(1, itemContainers.size))

                itemContainers.indices.forEach { index ->
                    val activity = visibleActivities.getOrNull(index)
                    val discipline = activity?.let { disciplines[it.disciplineId] }
                    bindActivity(
                        views = views,
                        index = index,
                        activity = activity,
                        disciplineName = discipline?.name,
                        disciplineLocation = discipline?.classroom,
                        disciplineColor = activity?.let {
                            runCatching { Color.parseColor(discipline?.colorHex ?: "#2563EB") }.getOrDefault(Color.BLUE)
                        },
                        showTime = settings.showTimes,
                        showDeadline = settings.showDeadlines && activity?.dueDate == today,
                        palette = palette
                    )
                }
            } catch (_: Exception) {
                views.setTextViewText(R.id.widget_title, "Hoje — Agenda Acadêmica")
                itemContainers.forEach { views.setViewVisibility(it, View.GONE) }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun bindActivity(
            views: RemoteViews,
            index: Int,
            activity: AcademicActivity?,
            disciplineName: String?,
            disciplineLocation: String?,
            disciplineColor: Int?,
            showTime: Boolean,
            showDeadline: Boolean,
            palette: WidgetPalette
        ) {
            val containerId = itemContainers[index]
            if (activity == null) {
                views.setViewVisibility(containerId, View.GONE)
                return
            }

            val color = disciplineColor ?: Color.BLUE
            val timePrefix = if (showTime && activity.time.isNotBlank()) "${activity.time} — " else ""
            views.setViewVisibility(containerId, View.VISIBLE)
            views.setInt(containerId, "setBackgroundResource", palette.itemBackground)
            views.setTextViewText(itemSubjects[index], "$timePrefix${disciplineName ?: "Geral"}")
            views.setTextColor(itemSubjects[index], color)
            views.setTextViewText(itemTitles[index], "${activity.activityType.displayName}: ${activity.title}")
            views.setInt(itemColors[index], "setBackgroundColor", color)
            val hasLocation = !disciplineLocation.isNullOrBlank()
            views.setTextViewText(itemLocations[index], if (hasLocation) "📍 $disciplineLocation" else "")
            views.setViewVisibility(itemLocations[index], if (hasLocation) View.VISIBLE else View.GONE)
            views.setViewVisibility(itemDeadlines[index], if (showDeadline) View.VISIBLE else View.GONE)
        }

        private fun getPalette(context: Context): WidgetPalette {
            val appearancePreferences = context.getSharedPreferences("appearance_preferences", Context.MODE_PRIVATE)
            val systemIsDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val isDark = appearancePreferences.getBoolean("dark_theme", systemIsDark)
            return if (isDark) {
                WidgetPalette(
                    background = R.drawable.widget_bg,
                    itemBackground = R.drawable.widget_item_bg,
                    primaryText = Color.WHITE,
                    secondaryText = Color.parseColor("#CBD5E1"),
                    deadlineText = Color.parseColor("#FCA5A5")
                )
            } else {
                WidgetPalette(
                    background = R.drawable.widget_bg_light,
                    itemBackground = R.drawable.widget_item_bg_light,
                    primaryText = Color.parseColor("#0F172A"),
                    secondaryText = Color.parseColor("#475569"),
                    deadlineText = Color.parseColor("#B91C1C")
                )
            }
        }

        private fun applyTheme(views: RemoteViews, palette: WidgetPalette) {
            views.setInt(R.id.widget_root, "setBackgroundResource", palette.background)
            views.setTextColor(R.id.widget_title, palette.primaryText)
            itemTitles.forEach { views.setTextColor(it, palette.primaryText) }
            itemLocations.forEach { views.setTextColor(it, palette.secondaryText) }
            itemDeadlines.forEach { views.setTextColor(it, palette.deadlineText) }
        }
    }
}
