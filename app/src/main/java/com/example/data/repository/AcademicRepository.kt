package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.ai.ExtractedSyllabus
import com.example.data.local.AppDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority
import com.example.data.remote.AcademicApi
import com.example.data.remote.AcademicApiFactory
import com.example.notification.AcademicReminderScheduler
import com.example.widget.AcademicDayAppWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class DisciplineWithStats(
    val discipline: Discipline,
    val totalActivities: Int,
    val completedActivities: Int,
    val nextActivity: AcademicActivity? = null
) {
    val progressFraction: Float
        get() = if (totalActivities > 0) completedActivities.toFloat() / totalActivities else 0f
}

data class DashboardOverview(
    val todayActivities: List<ActivityWithDiscipline>,
    val upcomingExams: List<ActivityWithDiscipline>,
    val urgentDeadlines: List<ActivityWithDiscipline>,
    val delayedActivities: List<ActivityWithDiscipline>,
    val weeklyPendingCount: Int,
    val totalCompletedCount: Int,
    val disciplineStats: List<DisciplineWithStats>
)

data class ActivityWithDiscipline(
    val activity: AcademicActivity,
    val discipline: Discipline
)

data class DocumentUpload(
    val uri: Uri,
    val fileName: String,
    val mimeType: String
)

class AcademicRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val api: AcademicApi = AcademicApiFactory.create()
) {
    private val disciplineColorPalette = listOf(
        "#2E7D32", "#C62828", "#6A1B9A", "#EF6C00",
        "#00838F", "#AD1457", "#4527A0", "#558B2F",
        "#D84315", "#0277BD", "#5D4037", "#1565C0"
    )
    private val disciplineDao = database.disciplineDao()
    private val activityDao = database.activityDao()
    private val documentDao = database.documentDao()

    val allDisciplines: Flow<List<Discipline>> = disciplineDao.getAllDisciplines()
    val allActivities: Flow<List<AcademicActivity>> = activityDao.getAllActivities()
    val allDocuments: Flow<List<AcademicDocument>> = documentDao.getAllDocuments()

    suspend fun extractDocuments(documents: List<DocumentUpload>): ExtractedSyllabus {
        require(documents.isNotEmpty()) { "Selecione pelo menos um arquivo." }
        require(documents.size <= 10) { "Selecione no máximo 10 imagens." }
        require(documents.size == 1 || documents.all { it.mimeType.startsWith("image/") }) {
            "O envio múltiplo aceita somente imagens."
        }
        val parts = documents.map { document ->
            val bytes = context.contentResolver.openInputStream(document.uri)?.use { it.readBytes() }
                ?: error("Não foi possível abrir ${document.fileName}.")
            val valid = when (document.mimeType) {
            "application/pdf" -> bytes.size >= 5 && bytes.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray())
            "image/jpeg" -> bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            "image/png" -> bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            "image/webp" -> bytes.size >= 12 && String(bytes.copyOfRange(0, 4)) == "RIFF" && String(bytes.copyOfRange(8, 12)) == "WEBP"
            else -> false
            }
            require(valid) { "${document.fileName} não é um PDF, JPEG, PNG ou WebP válido." }
            val safeName = document.fileName.replace("\"", "").ifBlank { "documento" }
            val body = bytes.toRequestBody(document.mimeType.toMediaType())
            MultipartBody.Part.createFormData(if (documents.size == 1) "file" else "files", safeName, body)
        }
        return api.extractDocuments(parts).data.toDomain()
    }

    suspend fun saveImportedSyllabus(
        syllabus: ExtractedSyllabus,
        selectedEvents: List<com.example.ai.ExtractedEvent>
    ): Long {
        // Find existing discipline or create new one
        var discipline = disciplineDao.getDisciplineByName(syllabus.disciplineName)
        val disciplineId = if (discipline != null) {
            // Update details if missing
            val updated = discipline.copy(
                professor = if (discipline.professor.isBlank()) syllabus.professor else discipline.professor,
                classroom = if (discipline.classroom.isBlank()) syllabus.classroom else discipline.classroom,
                schedule = if (discipline.schedule.isBlank()) syllabus.schedule else discipline.schedule,
                term = syllabus.term
            )
            disciplineDao.updateDiscipline(updated)
            discipline.id
        } else {
            val color = nextAvailableDisciplineColor(syllabus.recommendedColorHex)
            disciplineDao.insertDiscipline(
                Discipline(
                    name = syllabus.disciplineName,
                    code = syllabus.disciplineCode,
                    professor = syllabus.professor,
                    classroom = syllabus.classroom,
                    schedule = syllabus.schedule,
                    term = syllabus.term,
                    colorHex = color,
                    syllabusNotes = "Importado automaticamente via análise inteligente de documento."
                )
            )
        }

        // Insert document record
        val docId = documentDao.insertDocument(
            AcademicDocument(
                title = syllabus.documentTitle,
                fileName = "${syllabus.disciplineName.replace(" ", "_")}_2026.pdf",
                disciplineId = disciplineId,
                parsedSummary = "${selectedEvents.size} eventos e compromissos importados",
                itemsExtractedCount = selectedEvents.size
            )
        )

        // Insert activities
        val scheduleStartTime = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
            .find(syllabus.schedule)
            ?.value
            ?.padStart(5, '0')
            .orEmpty()
        val activitiesToInsert = selectedEvents.map { event ->
            AcademicActivity(
                disciplineId = disciplineId,
                title = event.title,
                description = event.description,
                activityType = event.activityType,
                date = event.date,
                time = event.time.ifBlank { scheduleStartTime },
                dueDate = event.dueDate ?: if (event.activityType == ActivityType.TRABALHO || event.activityType == ActivityType.PROJETO) event.date else null,
                priority = event.priority,
                status = ActivityStatus.PENDENTE,
                sourceDocument = syllabus.documentTitle,
                notes = "Identificado automaticamente do plano de ensino."
            )
        }

        val insertedIds = activityDao.insertActivities(activitiesToInsert)
        activitiesToInsert.zip(insertedIds).forEach { (activity, id) ->
            AcademicReminderScheduler.schedule(context, activity.copy(id = id), disciplineDao.getDisciplineById(disciplineId))
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return disciplineId
    }

    suspend fun insertDiscipline(discipline: Discipline): Long {
        val id = disciplineDao.insertDiscipline(discipline.withAvailableColor())
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    suspend fun insertDisciplineWithClasses(discipline: Discipline, classes: List<AcademicActivity>): Long {
        val id = database.withTransaction {
            val savedId = if (discipline.id == 0L) {
                disciplineDao.insertDiscipline(discipline.withAvailableColor())
            } else {
                disciplineDao.updateDiscipline(discipline.withAvailableColor(excludingId = discipline.id))
                discipline.id
            }

            if (classes.isNotEmpty()) {
                if (discipline.id != 0L) {
                    activityDao.updateDisciplineActivityTimes(savedId, classes.first().time)
                }
                val generatedClassesAlreadySaved = if (discipline.id != 0L) {
                    activityDao.getGeneratedClasses(savedId)
                } else {
                    emptyList()
                }
                if (generatedClassesAlreadySaved.isNotEmpty()) {
                    activityDao.deleteGeneratedClasses(savedId)
                    activityDao.insertActivities(classes.map { it.copy(disciplineId = savedId) })
                } else if (discipline.id != 0L && activityDao.countClassesByDiscipline(savedId) > 0) {
                    // Imported occurrences retain their dates and content; their times were updated above.
                } else {
                    activityDao.insertActivities(classes.map { it.copy(disciplineId = savedId) })
                }
            } else if (discipline.id != 0L) {
                removeDuplicateGeneratedClasses(savedId)
            }
            savedId
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    private suspend fun removeDuplicateGeneratedClasses(disciplineId: Long) {
        val duplicates = activityDao.getGeneratedClasses(disciplineId)
            .groupBy { "${it.date}|${it.time}" }
            .values
            .flatMap { occurrences ->
                if (occurrences.size <= 1) {
                    emptyList()
                } else {
                    val occurrenceToKeep = occurrences.firstOrNull { it.status != ActivityStatus.PENDENTE }
                        ?: occurrences.first()
                    occurrences.filter { it.id != occurrenceToKeep.id }.map { it.id }
                }
            }
        if (duplicates.isNotEmpty()) {
            activityDao.deleteActivitiesByIds(duplicates)
        }
    }

    suspend fun insertActivities(activities: List<AcademicActivity>): List<Long> {
        val ids = activityDao.insertActivities(activities)
        activities.zip(ids).forEach { (activity, id) ->
            AcademicReminderScheduler.schedule(context, activity.copy(id = id), activity.disciplineId?.let { disciplineDao.getDisciplineById(it) })
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return ids
    }

    suspend fun updateDiscipline(discipline: Discipline) {
        disciplineDao.updateDiscipline(discipline.withAvailableColor(excludingId = discipline.id))
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun ensureDistinctDisciplineColors() {
        val disciplines = disciplineDao.getAllDisciplines().first()
        val usedColors = mutableSetOf<String>()
        disciplines.forEachIndexed { index, discipline ->
            val current = discipline.colorHex.uppercase()
            val color = if (current !in usedColors) {
                current
            } else {
                disciplineColorPalette.firstOrNull { it.uppercase() !in usedColors }
                    ?: disciplineColorPalette[index % disciplineColorPalette.size]
            }
            usedColors += color.uppercase()
            if (!color.equals(discipline.colorHex, ignoreCase = true)) {
                disciplineDao.updateDiscipline(discipline.copy(colorHex = color))
            }
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    private suspend fun Discipline.withAvailableColor(excludingId: Long? = null): Discipline {
        val color = nextAvailableDisciplineColor(colorHex, excludingId)
        return if (color.equals(colorHex, ignoreCase = true)) this else copy(colorHex = color)
    }

    private suspend fun nextAvailableDisciplineColor(preferred: String, excludingId: Long? = null): String {
        val usedColors = disciplineDao.getAllDisciplines().first()
            .filter { it.id != excludingId }
            .map { it.colorHex.uppercase() }
            .toSet()
        if (preferred.uppercase() !in usedColors) return preferred
        return disciplineColorPalette.firstOrNull { it.uppercase() !in usedColors }
            ?: disciplineColorPalette[usedColors.size % disciplineColorPalette.size]
    }

    suspend fun deleteDiscipline(disciplineId: Long) {
        disciplineDao.deleteDisciplineById(disciplineId)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun insertActivity(activity: AcademicActivity): Long {
        val id = activityDao.insertActivity(activity)
        AcademicReminderScheduler.schedule(context, activity.copy(id = id), activity.disciplineId?.let { disciplineDao.getDisciplineById(it) })
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    suspend fun updateActivity(activity: AcademicActivity) {
        activityDao.updateActivity(activity)
        AcademicReminderScheduler.schedule(context, activity, activity.disciplineId?.let { disciplineDao.getDisciplineById(it) })
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun updateActivityStatus(id: Long, status: ActivityStatus) {
        activityDao.updateActivityStatus(id, status)
        activityDao.getActivityById(id)?.let { activity ->
            AcademicReminderScheduler.schedule(context, activity, activity.disciplineId?.let { disciplineDao.getDisciplineById(it) })
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun deleteActivity(id: Long) {
        AcademicReminderScheduler.cancel(context, id)
        activityDao.deleteActivityById(id)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    fun getActivitiesByDate(date: String): Flow<List<AcademicActivity>> = activityDao.getActivitiesByDate(date)

    fun getActivitiesByDiscipline(disciplineId: Long): Flow<List<AcademicActivity>> = activityDao.getActivitiesByDiscipline(disciplineId)

    fun getDocumentsByDiscipline(disciplineId: Long): Flow<List<AcademicDocument>> = documentDao.getDocumentsByDiscipline(disciplineId)

    suspend fun deleteDocument(id: Long) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun rescheduleAllNotifications() {
        val disciplineMap = disciplineDao.getAllDisciplines().first().associateBy { it.id }
        activityDao.getAllActivities().first().forEach { activity ->
            AcademicReminderScheduler.schedule(context, activity, activity.disciplineId?.let(disciplineMap::get))
        }
    }
}
