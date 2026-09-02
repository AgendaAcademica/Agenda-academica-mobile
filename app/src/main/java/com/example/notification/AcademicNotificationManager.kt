package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AcademicActivity
import com.example.data.model.Discipline
import com.example.data.model.Priority

object AcademicNotificationManager {
    private const val CHANNEL_ID = "academic_reminders_channel"
    private const val CHANNEL_NAME = "Lembretes Acadêmicos"
    private const val CHANNEL_DESC = "Notificações de aulas, provas, trabalhos e prazos acadêmicos"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTestReminder(context: Context, activity: AcademicActivity, discipline: Discipline, reminderType: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            activity.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityText = when (activity.priority) {
            Priority.URGENTE -> "🚨 URGENTE"
            Priority.ALTA -> "⚠️ Alta Prioridade"
            Priority.MEDIA -> "📌 Importante"
            Priority.BAIXA -> "ℹ️ Lembrete"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$priorityText: ${discipline.name}")
            .setContentText("[$reminderType] ${activity.activityType.displayName}: ${activity.title} (${activity.date} às ${activity.time})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("[$reminderType] ${activity.title}\nDisciplina: ${discipline.name} (${discipline.professor})\nData: ${activity.date} às ${activity.time}\n${activity.description}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        runCatching {
            NotificationManagerCompat.from(context).notify(activity.id.toInt() + 1000, builder.build())
        }
    }
}
