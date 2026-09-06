package com.example

import android.app.Application
import com.example.data.repository.AcademicRepository
import com.example.notification.AcademicNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AcademicApp : Application() {
    lateinit var repository: AcademicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AcademicNotificationManager.createNotificationChannel(this)
        repository = AcademicRepository(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.rescheduleAllNotifications()
        }
    }
}
