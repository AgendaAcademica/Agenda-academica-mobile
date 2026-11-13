package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.ai.ExtractedEvent
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
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

data class DisciplineWithStats(
    val discipline: Discipline,
    val totalActivities: Int,
    val completedActivities: Int,
    val nextActivity: AcademicActivity? = null
) {
    val progressFraction: Float
        get() = if (totalActivities > 0) completedActivities.toFloat() / totalActivities else 0f
}

internal fun generateTimetableEvents(
    syllabus: ExtractedSyllabus,
    createdDateIso: String
): List<ExtractedEvent> {
    val normalized = syllabus.schedule.lowercase(Locale.ROOT)
    val selectedDays = buildSet {
        if (Regex("\\b(seg|segunda)").containsMatchIn(normalized)) add(Calendar.MONDAY)
        if (Regex("\\b(ter|terça|terca)").containsMatchIn(normalized)) add(Calendar.TUESDAY)
        if (Regex("\\b(qua|quarta)").containsMatchIn(normalized)) add(Calendar.WEDNESDAY)
        if (Regex("\\b(qui|quinta)").containsMatchIn(normalized)) add(Calendar.THURSDAY)
        if (Regex("\\b(sex|sexta)").containsMatchIn(normalized)) add(Calendar.FRIDAY)
        if (Regex("\\b(sáb|sab|sábado|sabado)").containsMatchIn(normalized)) add(Calendar.SATURDAY)
        if (Regex("\\b(dom|domingo)").containsMatchIn(normalized)) add(Calendar.SUNDAY)
    }
    val times = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
        .findAll(syllabus.schedule)
        .map { it.value.padStart(5, '0') }
        .toList()
    if (selectedDays.isEmpty() || times.isEmpty()) return emptyList()

    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    val start = runCatching { format.parse(createdDateIso) }.getOrNull() ?: return emptyList()
    val endIso = academicTermEndDate(syllabus.term, createdDateIso)
    val end = runCatching { format.parse(endIso) }.getOrNull() ?: return emptyList()
    val current = Calendar.getInstance().apply { time = start }
    val finish = Calendar.getInstance().apply { time = end }
    val timeRange = if (times.size >= 2) "${times[0]} - ${times[1]}" else times[0]
    val result = mutableListOf<ExtractedEvent>()
    var number = 1

    while (!current.after(finish)) {
        if (current.get(Calendar.DAY_OF_WEEK) in selectedDays) {
            result += ExtractedEvent(
                title = "Aula #$number — ${syllabus.disciplineName}",
                date = format.format(current.time),
                time = times[0],
                activityType = ActivityType.AULA,
                priority = Priority.BAIXA,
                description = "Horário previsto: $timeRange. Aula criada sem base de plano acadêmico."
            )
            number++
        }
        current.add(Calendar.DAY_OF_MONTH, 1)
    }
    return result
}

internal fun academicTermEndDate(term: String, referenceDateIso: String): String {
    val referenceParts = referenceDateIso.split("-")
    val referenceYear = referenceParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
    val referenceMonth = referenceParts.getOrNull(1)?.toIntOrNull() ?: 1
    val match = Regex("(20\\d{2}).*?([12])(?:\\D|$)").find(term)
    val year = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: referenceYear
    val semester = match?.groupValues?.getOrNull(2)?.toIntOrNull() ?: if (referenceMonth <= 6) 1 else 2
    return if (semester == 1) "%04d-06-30".format(year) else "%04d-12-15".format(year)
}

internal fun normalizeDisciplineIdentity(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

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
    val mimeType: String,
    val sizeBytes: Long = 0
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

    suspend fun fingerprintDocuments(documents: List<DocumentUpload>): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val hashes = documents.map { document ->
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(document.uri)?.use { input ->
                val buffer = ByteArray(8192)
                var count = input.read(buffer)
                while (count != -1) {
                    digest.update(buffer, 0, count)
                    count = input.read(buffer)
                }
            } ?: error("Não foi possível abrir o documento.")
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        hashes.sorted().joinToString(":")
    }

    suspend fun isRepeatedImport(syllabus: ExtractedSyllabus, fingerprint: String): Boolean {
        val discipline = findImportedDiscipline(syllabus) ?: return false
        return documentDao.getDocumentsByDiscipline(discipline.id).first().any {
            if (it.fileFingerprint.isNotBlank() && fingerprint.isNotBlank()) it.fileFingerprint == fingerprint
            else normalizeDisciplineIdentity(it.title) == normalizeDisciplineIdentity(syllabus.documentTitle)
        }
    }

    suspend fun extractDocuments(documents: List<DocumentUpload>): List<ExtractedSyllabus> {
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
        return api.extractDocuments(parts).data.disciplines.map { it.toDomain() }
    }

    suspend fun saveImportedSyllabus(
        syllabus: ExtractedSyllabus,
        selectedEvents: List<com.example.ai.ExtractedEvent>,
        withoutAcademicPlan: Boolean = false,
        replaceExistingClassroom: Boolean = false,
        fileFingerprint: String = "",
        replaceExistingActivities: Boolean = false
    ): Long {
        var added = emptyList<AcademicActivity>()
        var ids = emptyList<Long>()
        var removed = emptyList<AcademicActivity>()
        val savedId = database.withTransaction {
            // Match schedules and later syllabus imports to the same discipline.
            val discipline = findImportedDiscipline(syllabus)
            val disciplineId = if (discipline != null) {
                val updated = discipline.copy(
                    name = syllabus.disciplineName.ifBlank { discipline.name },
                    code = syllabus.disciplineCode.ifBlank { discipline.code },
                    professor = syllabus.professor.ifBlank { discipline.professor },
                    // O comprovante de matrícula é a fonte mais confiável para a sala de aula.
                    // Planos podem trazer uma sala de atendimento, que não deve sobrescrevê-la.
                    classroom = if (withoutAcademicPlan) {
                        syllabus.classroom.ifBlank { discipline.classroom }
                    } else if (replaceExistingClassroom && syllabus.classroom.isNotBlank()) {
                        syllabus.classroom
                    } else {
                        discipline.classroom.ifBlank { syllabus.classroom }
                    },
                    schedule = syllabus.schedule.ifBlank { discipline.schedule },
                    term = syllabus.term.ifBlank { discipline.term },
                    syllabusNotes = if (withoutAcademicPlan) discipline.syllabusNotes else
                        "Informações atualizadas automaticamente a partir do plano de ensino."
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
                        syllabusNotes = if (withoutAcademicPlan) {
                            "Disciplina importada de um quadro de horário ou comprovante de matrícula, sem base de plano acadêmico."
                        } else {
                            "Importado automaticamente via análise inteligente de documento."
                        }
                    )
                )
            }

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
                    notes = if (withoutAcademicPlan) {
                        "Aula criada a partir do quadro de horário ou comprovante de matrícula, sem base de plano acadêmico."
                    } else {
                        "Identificado automaticamente do plano de ensino."
                    }
                )
            }

            val existing = activityDao.getActivitiesByDiscipline(disciplineId).first()
            removed = if (replaceExistingActivities) existing else if (!withoutAcademicPlan && selectedEvents.any { it.activityType == ActivityType.AULA }) {
                existing.filter { it.activityType == ActivityType.AULA &&
                    (it.sourceDocument == "Horário das Aulas" || it.notes.contains("sem base de plano acadêmico")) }
            } else emptyList()
            added = uniqueImportedActivities(existing.filter { item -> removed.none { it.id == item.id } }, activitiesToInsert)
            if (removed.isNotEmpty()) activityDao.deleteActivitiesByIds(removed.map { it.id })
            ids = if (added.isEmpty()) emptyList() else activityDao.insertActivities(added)
            if (added.isNotEmpty() || discipline == null || replaceExistingActivities) {
                // Insert document record
                documentDao.insertDocument(
                    AcademicDocument(
                        title = syllabus.documentTitle,
                        fileFingerprint = fileFingerprint,
                        fileName = "${syllabus.disciplineName.replace(" ", "_")}_2026.pdf",
                        disciplineId = disciplineId,
                        parsedSummary = if (withoutAcademicPlan) {
                            "${added.size} aulas criadas a partir dos horários do documento, sem base de plano acadêmico"
                        } else {
                            "${added.size} eventos e compromissos importados"
                        },
                        itemsExtractedCount = added.size
                    )
                )

            }
            disciplineId
        }
        removed.forEach { AcademicReminderScheduler.cancel(context, it.id) }
        added.zip(ids).forEach { (activity, id) ->
            AcademicReminderScheduler.schedule(context, activity.copy(id = id), disciplineDao.getDisciplineById(savedId))
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return savedId
    }

    private suspend fun findImportedDiscipline(syllabus: ExtractedSyllabus): Discipline? {
        if (syllabus.disciplineCode.isNotBlank()) {
            disciplineDao.getDisciplineByCode(syllabus.disciplineCode.trim())?.let { return it }
        }
        disciplineDao.getDisciplineByName(syllabus.disciplineName)?.let { return it }
        val normalizedName = normalizeDisciplineIdentity(syllabus.disciplineName)
        if (normalizedName.isBlank()) return null
        return disciplineDao.getAllDisciplines().first().firstOrNull {
            normalizeDisciplineIdentity(it.name) == normalizedName
        }
    }

    suspend fun saveImportedTimetable(syllabi: List<ExtractedSyllabus>, createdDateIso: String, fileFingerprint: String = "", replaceRepeated: Boolean = false): Int {
        var totalClasses = 0
        syllabi.forEach { syllabus ->
            val classes = generateTimetableEvents(syllabus, createdDateIso)
            saveImportedSyllabus(syllabus, classes, withoutAcademicPlan = true, fileFingerprint = fileFingerprint,
                replaceExistingActivities = replaceRepeated && isRepeatedImport(syllabus, fileFingerprint))
            totalClasses += classes.size
        }
        return totalClasses
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

    suspend fun clearAcademicData() {
        val removed = database.withTransaction {
            val activities = activityDao.getAllActivities().first()
            activityDao.deleteAll()
            documentDao.deleteAll()
            disciplineDao.deleteAll()
            activities
        }
        removed.forEach { AcademicReminderScheduler.cancel(context, it.id) }
        androidx.core.app.NotificationManagerCompat.from(context).cancelAll()
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
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

internal fun uniqueImportedActivities(existing: List<AcademicActivity>, incoming: List<AcademicActivity>): List<AcademicActivity> {
    fun key(a: AcademicActivity): List<Any?> = listOf(
        a.disciplineId, a.activityType, a.originalDate ?: a.date, a.time.trim().padStart(5, '0'),
        if (a.activityType == ActivityType.AULA) "" else normalizeDisciplineIdentity(a.title)
    )
    val seen = existing.map(::key).toMutableSet()
    return incoming.filter { seen.add(key(it)) }
}
