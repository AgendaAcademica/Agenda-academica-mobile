package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AcademicApp
import com.example.ai.ExtractedEvent
import com.example.ai.ExtractedSyllabus
import com.example.data.auth.FirebaseUsageTracker
import com.example.data.model.AcademicActivity
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.repository.DisciplineWithStats
import com.example.data.repository.DocumentUpload
import com.example.widget.WidgetPreferences
import com.example.widget.WidgetSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val title: String) {
    MONTH("Mensal"),
    WEEK("Semanal"),
    DAY("Diária")
}

class AcademicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as AcademicApp).repository
    private val usageTracker = FirebaseUsageTracker(application)
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val todayParts = today.split("-")

    init {
        viewModelScope.launch {
            repository.ensureDistinctDisciplineColors()
        }
    }

    val disciplines: StateFlow<List<Discipline>> = repository.allDisciplines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activities: StateFlow<List<AcademicActivity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<AcademicDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _currentDate = MutableStateFlow(today)
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _calendarViewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val calendarViewMode: StateFlow<CalendarViewMode> = _calendarViewMode.asStateFlow()

    private val _expandedWeekIndex = MutableStateFlow<Int?>(0)
    val expandedWeekIndex: StateFlow<Int?> = _expandedWeekIndex.asStateFlow()

    private val _calendarDisciplineFilter = MutableStateFlow<Long?>(null)
    val calendarDisciplineFilter: StateFlow<Long?> = _calendarDisciplineFilter.asStateFlow()

    private val _calendarTypeFilter = MutableStateFlow<ActivityType?>(null)
    val calendarTypeFilter: StateFlow<ActivityType?> = _calendarTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Import / AI states
    private val _isAnalyzingDocument = MutableStateFlow(false)
    val isAnalyzingDocument: StateFlow<Boolean> = _isAnalyzingDocument.asStateFlow()

    private val _extractedSyllabus = MutableStateFlow<ExtractedSyllabus?>(null)
    val extractedSyllabus: StateFlow<ExtractedSyllabus?> = _extractedSyllabus.asStateFlow()

    private val _extractedSyllabi = MutableStateFlow<List<ExtractedSyllabus>>(emptyList())
    val extractedSyllabi: StateFlow<List<ExtractedSyllabus>> = _extractedSyllabi.asStateFlow()

    private val _pendingImportDisciplineCount = MutableStateFlow(0)
    val pendingImportDisciplineCount: StateFlow<Int> = _pendingImportDisciplineCount.asStateFlow()
    private var pendingImportedSyllabi: List<ExtractedSyllabus> = emptyList()
    private var importedDisciplinesSaved = 0

    private val _importSuccessMessage = MutableStateFlow<String?>(null)
    val importSuccessMessage: StateFlow<String?> = _importSuccessMessage.asStateFlow()

    private val _documentAnalysisError = MutableStateFlow<String?>(null)
    val documentAnalysisError: StateFlow<String?> = _documentAnalysisError.asStateFlow()
    private var documentAnalysisJob: Job? = null

    // Widget Settings
    private val _widgetSettings = MutableStateFlow(WidgetPreferences.getSettings(application))
    val widgetSettings: StateFlow<WidgetSettings> = _widgetSettings.asStateFlow()

    // Active detail item
    private val _selectedActivity = MutableStateFlow<AcademicActivity?>(null)
    val selectedActivity: StateFlow<AcademicActivity?> = _selectedActivity.asStateFlow()

    private val _selectedDiscipline = MutableStateFlow<Discipline?>(null)
    val selectedDiscipline: StateFlow<Discipline?> = _selectedDiscipline.asStateFlow()

    // Calculated discipline stats
    val disciplineStats: StateFlow<List<DisciplineWithStats>> = combine(disciplines, activities) { discList, actList ->
        discList.map { disc ->
            val discActs = actList.filter { it.disciplineId == disc.id }
            val countableActivities = discActs.filter { it.activityType != ActivityType.AULA }
            val completed = countableActivities.count { it.status == ActivityStatus.CONCLUIDA }
            val nextAct = discActs
                .filter { it.status != ActivityStatus.CONCLUIDA && it.date >= _currentDate.value }
                .minByOrNull { "${it.date} ${it.time}" }
            DisciplineWithStats(
                discipline = disc,
                totalActivities = countableActivities.size,
                completedActivities = completed,
                nextActivity = nextAct
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar navigation (Year, Month 1..12)
    private val _calendarYearMonth = MutableStateFlow(
        Pair(todayParts[0].toInt(), todayParts[1].toInt())
    )
    val calendarYearMonth: StateFlow<Pair<Int, Int>> = _calendarYearMonth.asStateFlow()

    fun setCalendarYearMonth(year: Int, month: Int) {
        _calendarYearMonth.value = Pair(year, month)
    }

    fun nextMonth() {
        val (y, m) = _calendarYearMonth.value
        if (m == 12) {
            _calendarYearMonth.value = Pair(y + 1, 1)
        } else {
            _calendarYearMonth.value = Pair(y, m + 1)
        }
    }

    fun previousMonth() {
        val (y, m) = _calendarYearMonth.value
        if (m == 1) {
            _calendarYearMonth.value = Pair(y - 1, 12)
        } else {
            _calendarYearMonth.value = Pair(y, m - 1)
        }
    }

    fun goToCurrentMonth() {
        _calendarYearMonth.value = Pair(todayParts[0].toInt(), todayParts[1].toInt())
        _selectedDate.value = today
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        val parts = date.split("-")
        if (parts.size >= 2) {
            val y = parts[0].toIntOrNull() ?: 2026
            val m = parts[1].toIntOrNull() ?: 9
            if (_calendarYearMonth.value != Pair(y, m)) {
                _calendarYearMonth.value = Pair(y, m)
            }
        }
    }

    fun setCalendarViewMode(mode: CalendarViewMode) {
        _calendarViewMode.value = mode
    }

    fun toggleWeekExpansion(weekIndex: Int) {
        _expandedWeekIndex.value = if (_expandedWeekIndex.value == weekIndex) null else weekIndex
    }

    fun setCalendarDisciplineFilter(disciplineId: Long?) {
        _calendarDisciplineFilter.value = disciplineId
    }

    fun setCalendarTypeFilter(type: ActivityType?) {
        _calendarTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectActivity(activity: AcademicActivity?) {
        _selectedActivity.value = activity
    }

    fun selectDiscipline(discipline: Discipline?) {
        _selectedDiscipline.value = discipline
    }

    fun updateActivityStatus(id: Long, newStatus: ActivityStatus) {
        viewModelScope.launch {
            repository.updateActivityStatus(id, newStatus)
            if (_selectedActivity.value?.id == id) {
                _selectedActivity.value = _selectedActivity.value?.copy(status = newStatus)
            }
        }
    }

    fun saveActivity(activity: AcademicActivity) {
        viewModelScope.launch {
            if (activity.id == 0L) {
                repository.insertActivity(activity)
            } else {
                repository.updateActivity(activity)
            }
            _selectedActivity.value = null
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            repository.deleteActivity(id)
            if (_selectedActivity.value?.id == id) {
                _selectedActivity.value = null
            }
        }
    }

    fun saveDiscipline(discipline: Discipline) {
        viewModelScope.launch {
            if (discipline.id == 0L) {
                repository.insertDiscipline(discipline)
            } else {
                repository.updateDiscipline(discipline)
            }
        }
    }

    fun saveDisciplineWithClasses(discipline: Discipline, classes: List<AcademicActivity>) {
        viewModelScope.launch {
            repository.insertDisciplineWithClasses(discipline, classes)
        }
    }

    fun deleteDiscipline(id: Long) {
        viewModelScope.launch {
            repository.deleteDiscipline(id)
            if (_selectedDiscipline.value?.id == id) {
                _selectedDiscipline.value = null
            }
        }
    }

    private var pendingFileFingerprint = ""
    private val _repeatedImportNames = MutableStateFlow<List<String>>(emptyList())
    val repeatedImportNames = _repeatedImportNames.asStateFlow()
    private var pendingReplacement: (() -> Unit)? = null

    fun cancelRepeatedImport() {
        pendingReplacement = null
        _repeatedImportNames.value = emptyList()
    }

    fun confirmRepeatedImport() {
        val action = pendingReplacement ?: return
        cancelRepeatedImport()
        action()
    }

    fun analyzeDocuments(documents: List<DocumentUpload>) {
        if (_isClearing.value || _isSavingImport.value || _isAnalyzingDocument.value) return
        cancelRepeatedImport()
        documentAnalysisJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            _isAnalyzingDocument.value = true
            _importSuccessMessage.value = null
            _documentAnalysisError.value = null
            try {
                pendingFileFingerprint = repository.fingerprintDocuments(documents)
                pendingImportedSyllabi = repository.extractDocuments(documents)
                importedDisciplinesSaved = 0
                require(pendingImportedSyllabi.isNotEmpty()) { "Nenhuma disciplina foi identificada no documento." }
                _extractedSyllabi.value = pendingImportedSyllabi
                _extractedSyllabus.value = pendingImportedSyllabi.first()
                _pendingImportDisciplineCount.value = pendingImportedSyllabi.size
                usageTracker.recordDocumentAnalysis(
                    fileCount = documents.size,
                    totalBytes = documents.sumOf { it.sizeBytes },
                    durationMs = System.currentTimeMillis() - startedAt,
                    success = true,
                    resultCount = pendingImportedSyllabi.size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Preserve diagnostics in development logs without exposing URLs,
                // endpoints or third-party error details in the user interface.
                Log.e("AcademicViewModel", "Document analysis failed", e)
                _documentAnalysisError.value = documentAnalysisErrorMessage(e)
                usageTracker.recordDocumentAnalysis(
                    fileCount = documents.size,
                    totalBytes = documents.sumOf { it.sizeBytes },
                    durationMs = System.currentTimeMillis() - startedAt,
                    success = false,
                    errorCategory = documentAnalysisErrorCategory(e)
                )
            } finally {
                _isAnalyzingDocument.value = false
                documentAnalysisJob = null
            }
        }
    }

    fun cancelDocumentAnalysis() {
        if (!_isAnalyzingDocument.value) return
        documentAnalysisJob?.cancel()
        documentAnalysisJob = null
        _isAnalyzingDocument.value = false
        pendingFileFingerprint = ""
        _extractedSyllabus.value = null
        _extractedSyllabi.value = emptyList()
        pendingImportedSyllabi = emptyList()
        importedDisciplinesSaved = 0
        _pendingImportDisciplineCount.value = 0
        _importSuccessMessage.value = null
        _documentAnalysisError.value = null
    }

    fun clearExtractedSyllabus() {
        cancelRepeatedImport()
        pendingFileFingerprint = ""
        _extractedSyllabus.value = null
        _extractedSyllabi.value = emptyList()
        pendingImportedSyllabi = emptyList()
        importedDisciplinesSaved = 0
        _pendingImportDisciplineCount.value = 0
        _importSuccessMessage.value = null
        _documentAnalysisError.value = null
    }

    fun confirmAndSaveImportedEvents(
        syllabus: ExtractedSyllabus,
        events: List<ExtractedEvent>,
        replaceExistingClassroom: Boolean = false,
        replaceExistingActivities: Boolean = false
    ) {
        if (_extractedSyllabus.value == null || _isSavingImport.value || _isClearing.value) return
        _isSavingImport.value = true
        viewModelScope.launch {
            try {
                if (!replaceExistingActivities && repository.isRepeatedImport(syllabus, pendingFileFingerprint)) {
                    _repeatedImportNames.value = listOf(syllabus.disciplineName)
                    pendingReplacement = { confirmAndSaveImportedEvents(syllabus, events, replaceExistingClassroom, true) }
                    return@launch
                }
                repository.saveImportedSyllabus(
                    syllabus,
                    events,
                    replaceExistingClassroom = replaceExistingClassroom,
                    fileFingerprint = pendingFileFingerprint,
                    replaceExistingActivities = replaceExistingActivities
                )
                importedDisciplinesSaved++
                pendingImportedSyllabi = pendingImportedSyllabi.drop(1)
                _pendingImportDisciplineCount.value = pendingImportedSyllabi.size
                _extractedSyllabus.value = pendingImportedSyllabi.firstOrNull()
                _extractedSyllabi.value = pendingImportedSyllabi
                if (pendingImportedSyllabi.isEmpty()) {
                    _importSuccessMessage.value = if (importedDisciplinesSaved == 1) {
                        "Importação concluída! A disciplina ${syllabus.disciplineName} foi salva."
                    } else {
                        "Importação concluída! $importedDisciplinesSaved disciplinas foram salvas."
                    }
                }
            } catch (e: Exception) {
                _documentAnalysisError.value = "Não foi possível salvar a importação. Tente novamente."
            } finally {
                _isSavingImport.value = false
            }
        }
    }

    fun confirmAndSaveImportedTimetable(syllabi: List<ExtractedSyllabus>, replaceRepeated: Boolean = false) {
        if (syllabi.isEmpty() || _isSavingImport.value || _isClearing.value) return
        _isSavingImport.value = true
        viewModelScope.launch {
            try {
                val repeated = syllabi.filter { repository.isRepeatedImport(it, pendingFileFingerprint) }
                if (!replaceRepeated && repeated.isNotEmpty()) {
                    _repeatedImportNames.value = repeated.map { it.disciplineName }
                    pendingReplacement = { confirmAndSaveImportedTimetable(syllabi, true) }
                    return@launch
                }
                repository.saveImportedTimetable(syllabi, _currentDate.value, pendingFileFingerprint, replaceRepeated)
                _extractedSyllabus.value = null
                _extractedSyllabi.value = emptyList()
                pendingImportedSyllabi = emptyList()
                _pendingImportDisciplineCount.value = 0
                _importSuccessMessage.value = if (replaceRepeated) {
                    "Quadro de horário salvo. As atividades das disciplinas repetidas foram recriadas."
                } else {
                    "Quadro de horário salvo. Aulas já cadastradas foram mantidas."
                }
            } catch (e: Exception) {
                _documentAnalysisError.value = "Não foi possível salvar os horários. Tente novamente."
            } finally {
                _isSavingImport.value = false
            }
        }
    }

    private val _isSavingImport = MutableStateFlow(false)
    val isSavingImport = _isSavingImport.asStateFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing = _isClearing.asStateFlow()
    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage = _settingsMessage.asStateFlow()

    fun clearAcademicData() {
        if (_isClearing.value || _isAnalyzingDocument.value || _isSavingImport.value || pendingReplacement != null) return
        _isClearing.value = true
        viewModelScope.launch {
            try {
                repository.clearAcademicData()
                clearExtractedSyllabus()
                _selectedActivity.value = null
                _selectedDiscipline.value = null
                _calendarDisciplineFilter.value = null
                _calendarTypeFilter.value = null
                _searchQuery.value = ""
                goToCurrentMonth()
                _settingsMessage.value = "Disciplinas, atividades e documentos removidos."
            } catch (e: Exception) {
                _settingsMessage.value = "Não foi possível limpar os dados. Tente novamente."
            } finally {
                _isClearing.value = false
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    fun updateWidgetSettings(settings: WidgetSettings) {
        _widgetSettings.value = settings
        WidgetPreferences.saveSettings(getApplication(), settings)
        com.example.widget.AcademicDayAppWidgetProvider.updateAllWidgets(getApplication())
    }

}

internal fun documentAnalysisErrorMessage(error: Throwable): String = when (error) {
    is com.example.data.remote.SessionAuthenticationException ->
        "Não foi possível verificar sua sessão. Saia e entre novamente na sua conta."
    is java.net.SocketTimeoutException ->
        "O processamento demorou demais. Seus arquivos continuam selecionados; tente novamente."
    is java.net.ConnectException,
    is java.net.UnknownHostException,
    is javax.net.ssl.SSLException,
    is java.io.IOException ->
        "Não foi possível conectar ao serviço de análise. Verifique sua conexão e tente novamente."
    is retrofit2.HttpException -> when (error.code()) {
        401 -> "Sua sessão expirou. Saia e entre novamente na sua conta."
        403 -> "Esta conta ainda não está autorizada para os testes de análise. Contate o administrador."
        429 -> documentAnalysisLimitMessage(error)
        503 -> "O serviço de análise está temporariamente indisponível. Seus arquivos continuam selecionados; tente novamente mais tarde."
        413 -> "Os arquivos excedem o tamanho máximo permitido. Envie menos arquivos ou reduza o tamanho."
        415 -> "Um dos arquivos selecionados não está em um formato aceito."
        else -> "Não foi possível processar o documento. Verifique o arquivo e tente novamente."
    }
    is IllegalArgumentException -> if (error.message == "Nenhuma disciplina foi identificada no documento.") {
        "Nenhuma disciplina foi identificada no documento."
    } else {
        "Não foi possível analisar o documento. Verifique o arquivo e tente novamente."
    }
    else -> "Não foi possível analisar o documento. Verifique o arquivo e tente novamente."
}

private fun documentAnalysisLimitMessage(error: retrofit2.HttpException): String {
    val code = runCatching {
        val body = error.response()?.errorBody()?.string() ?: return@runCatching null
        val json = com.squareup.moshi.Moshi.Builder().build().adapter(Any::class.java).fromJson(body)
        (json as? Map<*, *>)?.get("code") as? String
    }.getOrNull()
    val message = when (code) {
        "USER_RATE_LIMIT" -> "Você fez vários envios em pouco tempo. Aguarde até 1 minuto e tente novamente."
        "USER_DAILY_QUOTA", "USER_AI_QUOTA" -> "Você atingiu seu limite diário de novas análises. Aguarde a renovação da cota à meia-noite UTC ou contate o administrador."
        "GLOBAL_DAILY_QUOTA", "GLOBAL_AI_QUOTA" -> "O limite diário de análises do serviço foi atingido. Aguarde a renovação da cota à meia-noite UTC ou contate o administrador."
        "USER_COST_LIMIT" -> "Você atingiu seu limite diário de custo das análises. Aguarde a renovação da cota à meia-noite UTC ou contate o administrador."
        "GLOBAL_COST_LIMIT" -> "O limite diário de custo do serviço foi atingido. Aguarde a renovação da cota à meia-noite UTC ou contate o administrador."
        "SERVER_BUSY" -> "Há outras análises em andamento. Tente novamente em instantes."
        "PROVIDER_RATE_LIMIT" -> "O serviço de inteligência artificial atingiu um limite temporário. Tente novamente em instantes."
        else -> "O serviço de análise atingiu um limite de uso. Tente novamente mais tarde ou contate o administrador."
    }
    return "$message Seus arquivos continuam selecionados."
}

internal fun documentAnalysisErrorCategory(error: Throwable): String = when (error) {
    is com.example.data.remote.SessionAuthenticationException -> "authentication"
    is java.net.SocketTimeoutException -> "timeout"
    is java.net.ConnectException,
    is java.net.UnknownHostException,
    is javax.net.ssl.SSLException,
    is java.io.IOException -> "connection"
    is retrofit2.HttpException -> "http_${error.code()}"
    is IllegalArgumentException -> "invalid_document"
    else -> "unexpected"
}
