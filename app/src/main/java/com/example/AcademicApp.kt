package com.example

import android.app.Application
import com.example.data.repository.AcademicRepository
import com.example.notification.AcademicNotificationManager

class AcademicApp : Application() {
    lateinit var repository: AcademicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        AcademicNotificationManager.createNotificationChannel(this)
        repository = AcademicRepository(this)
    }
}
