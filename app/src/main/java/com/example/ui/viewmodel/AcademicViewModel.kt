package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AcademicApp
import com.example.ai.ExtractedEvent
import com.example.ai.ExtractedSyllabus
import com.example.data.model.AcademicActivity
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.repository.DisciplineWithStats
import com.example.data.repository.DocumentUpload
import com.example.widget.WidgetPreferences
import com.example.widget.WidgetSettings
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

    private val _importSuccessMessage = MutableStateFlow<String?>(null)
    val importSuccessMessage: StateFlow<String?> = _importSuccessMessage.asStateFlow()

    private val _documentAnalysisError = MutableStateFlow<String?>(null)
    val documentAnalysisError: StateFlow<String?> = _documentAnalysisError.asStateFlow()

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

    fun analyzeDocuments(documents: List<DocumentUpload>) {
        viewModelScope.launch {
            _isAnalyzingDocument.value = true
            _importSuccessMessage.value = null
            _documentAnalysisError.value = null
            try {
                _extractedSyllabus.value = repository.extractDocuments(documents)
            } catch (e: Exception) {
                _documentAnalysisError.value = when (e) {
                    is java.net.ConnectException -> "Não foi possível conectar ao serviço de análise. Verifique sua conexão e tente novamente."
                    is java.net.SocketTimeoutException -> "O processamento demorou demais. Seus arquivos continuam selecionados; tente novamente."
                    is retrofit2.HttpException -> when (e.code()) {
                        429 -> "O limite temporário de análises foi atingido. Seus arquivos continuam selecionados; aguarde um pouco e tente novamente."
                        503 -> "A inteligência artificial está temporariamente ocupada. Seus arquivos continuam selecionados; aguarde alguns instantes e tente novamente."
                        413 -> "O arquivo excede o tamanho máximo permitido."
                        415 -> "Um dos arquivos selecionados não está em um formato aceito."
                        else -> "Não foi possível processar o documento. Verifique o arquivo e tente novamente."
                    }
                    else -> e.message ?: "Não foi possível analisar o documento."
                }
            } finally {
                _isAnalyzingDocument.value = false
            }
        }
    }

    fun clearExtractedSyllabus() {
        _extractedSyllabus.value = null
        _importSuccessMessage.value = null
        _documentAnalysisError.value = null
    }

    fun confirmAndSaveImportedEvents(syllabus: ExtractedSyllabus, events: List<ExtractedEvent>) {
        if (_extractedSyllabus.value == null) return
        viewModelScope.launch {
            val discId = repository.saveImportedSyllabus(syllabus, events)
            _extractedSyllabus.value = null
            _importSuccessMessage.value = "${events.size} compromissos adicionados com sucesso à disciplina ${syllabus.disciplineName}!"
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
