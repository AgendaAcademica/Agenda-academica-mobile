package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class AcademicDayAppWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        WidgetRefreshWorker.synchronize(context)
    }

    override fun onDisabled(context: Context) {
        WidgetRefreshWorker.synchronize(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            updateAllWidgets(context)
            WidgetRefreshWorker.synchronize(context)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        WidgetRefreshWorker.synchronize(context)
        ids.forEach { updateWidget(context, manager, it) }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_items_list)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, AcademicDayAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(provider)
            ids.forEach { updateWidget(context, manager, it) }
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_items_list)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.academic_widget_layout)
            val calendar = java.util.Calendar.getInstance()
            val months = arrayOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
            views.setTextViewText(
                R.id.widget_title,
                "Hoje — ${String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))} de ${months[calendar.get(java.util.Calendar.MONTH)]}"
            )

            val systemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val isDark = context.getSharedPreferences("appearance_preferences", Context.MODE_PRIVATE)
                .getBoolean("dark_theme", systemDark)
            views.setInt(R.id.widget_root, "setBackgroundResource", if (isDark) R.drawable.widget_bg else R.drawable.widget_bg_light)
            views.setTextColor(R.id.widget_title, if (isDark) Color.WHITE else Color.parseColor("#0F172A"))
            views.setTextColor(R.id.widget_empty, if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569"))

            val serviceIntent = Intent(context, AcademicWidgetListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                // A unique data URI forces launchers to discard cached collection rows.
                // Without it, a ListView can keep showing activities deleted from Room.
                data = android.net.Uri.Builder()
                    .scheme("academic-widget")
                    .authority("activities")
                    .appendPath(widgetId.toString())
                    .appendQueryParameter("refresh", System.currentTimeMillis().toString())
                    .build()
            }
            views.setRemoteAdapter(R.id.widget_items_list, serviceIntent)

            val openApp = PendingIntent.getActivity(
                context,
                widgetId,
                Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            views.setPendingIntentTemplate(R.id.widget_items_list, openApp)
            views.setEmptyView(R.id.widget_items_list, R.id.widget_empty)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
