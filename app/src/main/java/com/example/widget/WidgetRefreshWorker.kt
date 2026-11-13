package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        AcademicDayAppWidgetProvider.updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "academic-widget-refresh"

        fun synchronize(context: Context) {
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, AcademicDayAppWidgetProvider::class.java)
            )
            val manager = WorkManager.getInstance(context)
            if (ids.isEmpty()) {
                manager.cancelUniqueWork(WORK_NAME)
            } else {
                manager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES).build()
                )
            }
        }
    }
}
