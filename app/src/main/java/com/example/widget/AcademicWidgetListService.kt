package com.example.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.util.Log
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AcademicWidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = AcademicWidgetListFactory(applicationContext)
}

private class AcademicWidgetListFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private data class Row(val activity: AcademicActivity, val discipline: Discipline?)
    private var rows: List<Row> = emptyList()
    private var today: String = ""

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        rows = emptyList()
        runCatching {
            runBlocking {
                withTimeout(8_000) {
                    val database = AppDatabase.getDatabase(context)
                    val settings = WidgetPreferences.getSettings(context)
                    val disciplines = database.disciplineDao().getAllDisciplines().first().associateBy { it.id }
                    if (disciplines.isEmpty()) {
                        rows = emptyList()
                        return@withTimeout
                    }
                    var activities = database.activityDao().getAllActivities().first()
                        .filter { it.date == today || it.dueDate == today }
                        .filter { it.disciplineId == null || disciplines.containsKey(it.disciplineId) }
                    if (settings.showOnlyPending) activities = activities.filter { it.status != ActivityStatus.CONCLUIDA }
                    if (!settings.showClasses) activities = activities.filter { it.activityType != ActivityType.AULA }
                    if (settings.selectedDisciplineIds.isNotEmpty()) {
                        activities = activities.filter { it.disciplineId in settings.selectedDisciplineIds }
                    }
                    rows = activities.take(settings.maxItems.coerceAtLeast(1))
                        .map { Row(it, disciplines[it.disciplineId]) }
                }
            }
        }.onFailure { error ->
            Log.e("AcademicWidget", "Não foi possível carregar as atividades do widget", error)
        }
    }

    override fun onDestroy() { rows = emptyList() }
    override fun getCount(): Int = rows.size
    override fun getViewTypeCount(): Int = 1
    override fun hasStableIds(): Boolean = true
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.activity?.id ?: position.toLong()
    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews? {
        val row = rows.getOrNull(position) ?: return null
        return runCatching { createItemView(row) }.onFailure { error ->
            Log.e("AcademicWidget", "Não foi possível montar um item do widget", error)
        }.getOrNull()
    }

    private fun createItemView(row: Row): RemoteViews {
        val settings = WidgetPreferences.getSettings(context)
        val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val isDark = context.getSharedPreferences("appearance_preferences", Context.MODE_PRIVATE)
            .getBoolean("dark_theme", systemDark)
        val views = RemoteViews(context.packageName, R.layout.academic_widget_item)
        val color = runCatching { Color.parseColor(row.discipline?.colorHex ?: "#2563EB") }.getOrDefault(Color.BLUE)
        val timePrefix = if (settings.showTimes && row.activity.time.isNotBlank()) "${row.activity.time} — " else ""
        val itemAlpha = if (row.activity.status == ActivityStatus.CANCELADA) 0.55f else 1f
        // Always set the value because collection item views can be recycled by the launcher.
        views.setFloat(R.id.widget_item_root, "setAlpha", itemAlpha)
        views.setInt(R.id.widget_item_root, "setBackgroundResource", if (isDark) R.drawable.widget_item_bg else R.drawable.widget_item_bg_light)
        views.setInt(R.id.widget_item_color, "setBackgroundColor", color)
        views.setTextViewText(R.id.widget_item_subject_time, "$timePrefix${row.discipline?.name ?: "Geral"}")
        views.setTextColor(R.id.widget_item_subject_time, color)
        views.setTextViewText(R.id.widget_item_title, "${row.activity.activityType.displayName}: ${row.activity.title}")
        views.setTextColor(R.id.widget_item_title, if (isDark) Color.WHITE else Color.parseColor("#0F172A"))
        val location = row.discipline?.classroom.orEmpty()
        views.setTextViewText(R.id.widget_item_location, if (location.isBlank()) "" else "📍 $location")
        views.setTextColor(R.id.widget_item_location, if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569"))
        views.setViewVisibility(R.id.widget_item_location, if (location.isBlank()) View.GONE else View.VISIBLE)
        val deadline = settings.showDeadlines && row.activity.dueDate == today
        views.setViewVisibility(R.id.widget_item_deadline, if (deadline) View.VISIBLE else View.GONE)
        views.setTextColor(R.id.widget_item_deadline, if (isDark) Color.parseColor("#FCA5A5") else Color.parseColor("#B91C1C"))
        views.setOnClickFillInIntent(R.id.widget_item_root, Intent())
        return views
    }
}
