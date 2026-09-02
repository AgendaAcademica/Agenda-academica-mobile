package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["disciplineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("disciplineId"), Index("date")]
)
data class AcademicActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val disciplineId: Long? = null,
    val title: String,
    val description: String = "",
    val activityType: ActivityType = ActivityType.TRABALHO,
    val date: String, // YYYY-MM-DD
    val time: String = "08:00", // HH:MM
    val dueDate: String? = null, // YYYY-MM-DD
    val priority: Priority = Priority.MEDIA,
    val status: ActivityStatus = ActivityStatus.PENDENTE,
    val sourceDocument: String = "",
    val notes: String = "",
    val remindersEnabled: Boolean = true,
    val reminder7Days: Boolean = true,
    val reminder3Days: Boolean = true,
    val reminder1Day: Boolean = true,
    val reminderDayOf: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "academic_documents")
data class AcademicDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val disciplineId: Long? = null,
    val rawText: String = "",
    val parsedSummary: String = "",
    val itemsExtractedCount: Int = 0,
    val importedAt: Long = System.currentTimeMillis()
)
