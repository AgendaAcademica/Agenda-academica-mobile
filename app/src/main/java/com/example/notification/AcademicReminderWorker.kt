package com.example.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AcademicReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        AcademicNotificationManager.createNotificationChannel(applicationContext)
        val activityId = inputData.getLong(KEY_ACTIVITY_ID, 0L)
        val offsetDays = inputData.getInt(KEY_OFFSET_DAYS, 0)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (activityId.toString() + offsetDays).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val whenText = when (offsetDays) {
            7 -> "Faltam 7 dias"
            3 -> "Faltam 3 dias"
            1 -> "É amanhã"
            else -> "É hoje"
        }
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val discipline = inputData.getString(KEY_DISCIPLINE).orEmpty()
        val dateTime = inputData.getString(KEY_DATE_TIME).orEmpty()
        val notification = NotificationCompat.Builder(applicationContext, AcademicNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$whenText: $title")
            .setContentText("$discipline • $dateTime")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$whenText para $title\nDisciplina: $discipline\n$dateTime"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext)
            .notify((activityId.toString() + ":" + offsetDays).hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_ACTIVITY_ID = "activity_id"
        const val KEY_OFFSET_DAYS = "offset_days"
        const val KEY_TITLE = "title"
        const val KEY_DISCIPLINE = "discipline"
        const val KEY_DATE_TIME = "date_time"
    }
}

object AcademicReminderScheduler {
    private fun workName(activityId: Long, offsetDays: Int) = "academic-reminder-$activityId-$offsetDays"

    fun schedule(context: Context, activity: AcademicActivity, discipline: Discipline?) {
        cancel(context, activity.id)
        if (
            activity.id <= 0 || !activity.remindersEnabled ||
            activity.activityType == ActivityType.AULA ||
            activity.status == ActivityStatus.CONCLUIDA || activity.status == ActivityStatus.CANCELADA
        ) return

        val eventTime = activity.time.takeIf { Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(it) } ?: "09:00"
        val eventMillis = runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${activity.date} $eventTime")?.time
        }.getOrNull() ?: return
        val offsets = buildList {
            if (activity.reminder7Days) add(7)
            if (activity.reminder3Days) add(3)
            if (activity.reminder1Day) add(1)
            if (activity.reminderDayOf) add(0)
        }
        offsets.forEach { offset ->
            val triggerAt = eventMillis - TimeUnit.DAYS.toMillis(offset.toLong())
            val delay = triggerAt - System.currentTimeMillis()
            if (delay <= 0) return@forEach
            val data = Data.Builder()
                .putLong(AcademicReminderWorker.KEY_ACTIVITY_ID, activity.id)
                .putInt(AcademicReminderWorker.KEY_OFFSET_DAYS, offset)
                .putString(AcademicReminderWorker.KEY_TITLE, activity.title)
                .putString(AcademicReminderWorker.KEY_DISCIPLINE, discipline?.name ?: "Atividade acadêmica")
                .putString(AcademicReminderWorker.KEY_DATE_TIME, "${activity.date} às $eventTime")
                .build()
            val request = OneTimeWorkRequestBuilder<AcademicReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("academic-reminder-${activity.id}")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(activity.id, offset), ExistingWorkPolicy.REPLACE, request
            )
        }
    }

    fun cancel(context: Context, activityId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("academic-reminder-$activityId")
    }
}
