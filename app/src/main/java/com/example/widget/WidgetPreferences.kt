package com.example.widget

import android.content.Context
import android.content.SharedPreferences

data class WidgetSettings(
    val maxItems: Int = 3,
    val showOnlyPending: Boolean = false,
    val showClasses: Boolean = true,
    val showTimes: Boolean = true,
    val showDeadlines: Boolean = true,
    val selectedDisciplineIds: Set<Long> = emptySet()
)

object WidgetPreferences {
    private const val PREFS_NAME = "academic_widget_prefs"
    private const val KEY_MAX_ITEMS = "max_items"
    private const val KEY_SHOW_ONLY_PENDING = "show_only_pending"
    private const val KEY_SHOW_CLASSES = "show_classes"
    private const val KEY_SHOW_TIMES = "show_times"
    private const val KEY_SHOW_DEADLINES = "show_deadlines"
    private const val KEY_DISCIPLINES = "selected_disciplines"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSettings(context: Context): WidgetSettings {
        val prefs = getPrefs(context)
        val discSet = prefs.getStringSet(KEY_DISCIPLINES, emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        return WidgetSettings(
            maxItems = prefs.getInt(KEY_MAX_ITEMS, 3),
            showOnlyPending = prefs.getBoolean(KEY_SHOW_ONLY_PENDING, false),
            showClasses = prefs.getBoolean(KEY_SHOW_CLASSES, true),
            showTimes = prefs.getBoolean(KEY_SHOW_TIMES, true),
            showDeadlines = prefs.getBoolean(KEY_SHOW_DEADLINES, true),
            selectedDisciplineIds = discSet
        )
    }

    fun saveSettings(context: Context, settings: WidgetSettings) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putInt(KEY_MAX_ITEMS, settings.maxItems)
            .putBoolean(KEY_SHOW_ONLY_PENDING, settings.showOnlyPending)
            .putBoolean(KEY_SHOW_CLASSES, settings.showClasses)
            .putBoolean(KEY_SHOW_TIMES, settings.showTimes)
            .putBoolean(KEY_SHOW_DEADLINES, settings.showDeadlines)
            .putStringSet(KEY_DISCIPLINES, settings.selectedDisciplineIds.map { it.toString() }.toSet())
            .apply()
    }
}
