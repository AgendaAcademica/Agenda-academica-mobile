package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.ui.components.*
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.CalendarViewMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AcademicViewModel,
    onAddActivity: () -> Unit
) {
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarViewMode by viewModel.calendarViewMode.collectAsState()
    val selectedDisciplineFilter by viewModel.calendarDisciplineFilter.collectAsState()
    val selectedTypeFilter by viewModel.calendarTypeFilter.collectAsState()
    val calendarYearMonth by viewModel.calendarYearMonth.collectAsState()

    val disciplinesMap = disciplines.associateBy { it.id }

    // Filter activities based on chip selections
    val filteredActivities = activities.filter { act ->
        (selectedDisciplineFilter == null || act.disciplineId == selectedDisciplineFilter) &&
        (selectedTypeFilter == null || act.activityType == selectedTypeFilter)
    }

    val (currentYear, currentMonth) = calendarYearMonth
    val monthName = getBrazilianMonthName(currentMonth)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header: View Mode Switcher and Discipline Filters
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                // View Mode Segmented Control (Mês / Semana / Dia) & Quick Today jump
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualização",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // View Mode Segmented Control (Mês / Semana / Dia)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        CalendarViewMode.values().forEach { mode ->
                            val isSelected = mode == calendarViewMode
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.setCalendarViewMode(mode) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mode.title,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Discipline filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedDisciplineFilter == null,
                            onClick = { viewModel.setCalendarDisciplineFilter(null) },
                            label = { Text("Todas as Matérias", fontSize = 11.sp) }
                        )
                    }

                    items(disciplines) { disc ->
                        val isSelected = selectedDisciplineFilter == disc.id
                        val color = parseColorHex(disc.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.setCalendarDisciplineFilter(if (isSelected) null else disc.id)
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(disc.name, fontSize = 11.sp)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Calendar Content based on Mode
        when (calendarViewMode) {
            CalendarViewMode.MONTH -> MonthlyCalendarView(
                year = currentYear,
                month = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = { date -> viewModel.setSelectedDate(date) },
                onPreviousMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                activities = filteredActivities,
                disciplinesMap = disciplinesMap,
                onActivityClick = { act -> viewModel.selectActivity(act) },
                onStatusChange = { id, s -> viewModel.updateActivityStatus(id, s) },
                onGradeChange = { activity, grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
                onDateChange = { activity, date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) },
                onAddActivity = onAddActivity
            )
            CalendarViewMode.WEEK -> WeeklyCalendarView(
                year = currentYear,
                month = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) },
                activities = filteredActivities,
                disciplinesMap = disciplinesMap,
                onActivityClick = { act -> viewModel.selectActivity(act) },
                onStatusChange = { id, s -> viewModel.updateActivityStatus(id, s) },
                onGradeChange = { activity, grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
                onDateChange = { activity, date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) }
            )
            CalendarViewMode.DAY -> DailyTimelineView(
                year = currentYear,
                month = currentMonth,
                selectedDate = selectedDate,
                onDateSelected = { viewModel.setSelectedDate(it) },
                activities = filteredActivities,
                disciplinesMap = disciplinesMap,
                onActivityClick = { act -> viewModel.selectActivity(act) },
                onStatusChange = { id, s -> viewModel.updateActivityStatus(id, s) },
                onGradeChange = { activity, grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
                onDateChange = { activity, date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) }
            )
        }
    }
}

@Composable
fun MonthlyCalendarView(
    year: Int,
    month: Int,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    activities: List<AcademicActivity>,
    disciplinesMap: Map<Long, Discipline>,
    onActivityClick: (AcademicActivity) -> Unit,
    onStatusChange: (Long, ActivityStatus) -> Unit,
    onGradeChange: (AcademicActivity, Double?) -> Unit,
    onDateChange: (AcademicActivity, String) -> Unit,
    onAddActivity: () -> Unit
) {
    val monthName = getBrazilianMonthName(month)

    val selectedDayActivities = activities
        .filter { it.date == selectedDate }
        .sortedBy { it.time }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Calendar Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header of the Calendar with <, Month Year Title, and >
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Arrow (<)
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("btn_card_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Mês anterior",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Month and Year as Calendar Title with smooth transition
                        AnimatedContent(
                            targetState = "$monthName $year",
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { 20 })
                                    .togetherWith(fadeOut(animationSpec = tween(220)) + slideOutHorizontally(animationSpec = tween(220)) { -20 })
                            },
                            label = "calendar_card_month_title"
                        ) { title ->
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Right Arrow (>)
                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("btn_card_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Próximo mês",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Weekday headers: DOM to SÁB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Month Matrix with Animated transition on month change
                    AnimatedContent(
                        targetState = Pair(year, month),
                        transitionSpec = {
                            val targetVal = targetState.first * 12 + targetState.second
                            val initVal = initialState.first * 12 + initialState.second
                            if (targetVal > initVal) {
                                (slideInHorizontally(animationSpec = tween(260)) { it / 2 } + fadeIn(animationSpec = tween(260)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260)) { -it / 2 } + fadeOut(animationSpec = tween(260)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(260)) { -it / 2 } + fadeIn(animationSpec = tween(260)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(260)) { it / 2 } + fadeOut(animationSpec = tween(260)))
                            }
                        },
                        label = "calendar_matrix_anim"
                    ) { (currYear, currMonth) ->
                        val cal = remember(currYear, currMonth) {
                            Calendar.getInstance().apply {
                                set(Calendar.YEAR, currYear)
                                set(Calendar.MONTH, currMonth - 1)
                                set(Calendar.DAY_OF_MONTH, 1)
                            }
                        }

                        val daysInMonth = remember(currYear, currMonth) { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
                        val startOffset = remember(currYear, currMonth) { cal.get(Calendar.DAY_OF_WEEK) - 1 } // 0=DOM, 1=SEG... 6=SÁB

                        val weeks = remember(currYear, currMonth, daysInMonth, startOffset) {
                            val list = mutableListOf<List<Int?>>()
                            var currentDay = 1
                            var offset = startOffset
                            while (currentDay <= daysInMonth) {
                                val week = mutableListOf<Int?>()
                                for (i in 0 until 7) {
                                    if (offset > 0) {
                                        week.add(null)
                                        offset--
                                    } else if (currentDay <= daysInMonth) {
                                        week.add(currentDay)
                                        currentDay++
                                    } else {
                                        week.add(null)
                                    }
                                }
                                list.add(week)
                            }
                            list
                        }

                        Column {
                            weeks.forEachIndexed { weekIndex, week ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    week.forEach { dayNumber ->
                                        if (dayNumber != null) {
                                            val dateString = String.format("%04d-%02d-%02d", currYear, currMonth, dayNumber)
                                            val isSelected = dateString == selectedDate
                                            val dayActs = activities.filter { it.date == dateString }
                                            val hasExams = dayActs.any {
                                                it.activityType == ActivityType.PROVA ||
                                                (it.activityType != ActivityType.AULA && (
                                                    it.title.contains("prova", ignoreCase = true) ||
                                                    it.title.contains("avaliação", ignoreCase = true) ||
                                                    it.title.contains("avaliacao", ignoreCase = true) ||
                                                    it.title.contains("exame", ignoreCase = true) ||
                                                    it.title.contains("teste", ignoreCase = true)
                                                ))
                                            }
                                            val hasOnlyClasses = dayActs.isNotEmpty() && dayActs.all { it.activityType == ActivityType.AULA }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 2.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else if (dayActs.isNotEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable { onDateSelected(dateString) }
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Indicator dot based on day type
                                                if (dayActs.isEmpty()) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                } else {
                                                    val dotColor = when {
                                                        hasExams -> Color(0xFFBA1A1A)
                                                        hasOnlyClasses -> Color(0xFF006E1C)
                                                        else -> Color(0xFF0288D1)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else dotColor)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                if (weekIndex < weeks.size - 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Marker Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF006E1C)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apenas Aulas", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFBA1A1A)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Provas / Avaliações", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF0288D1)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Trabalhos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Expanded Activities of the Selected Day Header
        item {
            val formattedDate = isoToBrazilianDate(selectedDate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atividades em $formattedDate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (selectedDayActivities.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${selectedDayActivities.size} " + if (selectedDayActivities.size == 1) "atividade" else "atividades",
                        color = if (selectedDayActivities.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Expanded list of activities for the selected day
        if (selectedDayActivities.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma atividade agendada para ${isoToBrazilianDate(selectedDate)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onAddActivity,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar Atividade", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(selectedDayActivities) { act ->
                val disc = act.disciplineId?.let { disciplinesMap[it] }
                DashboardActivityCard(
                    activity = act,
                    discipline = disc,
                    onStatusChange = { newStatus -> onStatusChange(act.id, newStatus) },
                    onGradeChange = { grade -> onGradeChange(act, grade) },
                    onDateChange = { date -> onDateChange(act, date) },
                    onClick = { onActivityClick(act) }
                )
            }
        }
    }
}

@Composable
fun WeeklyCalendarView(
    year: Int,
    month: Int,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    activities: List<AcademicActivity>,
    disciplinesMap: Map<Long, Discipline>,
    onActivityClick: (AcademicActivity) -> Unit,
    onStatusChange: (Long, ActivityStatus) -> Unit,
    onGradeChange: (AcademicActivity, Double?) -> Unit,
    onDateChange: (AcademicActivity, String) -> Unit
) {
    // Generate the 7 days of the week containing selectedDate
    val weekDays = remember(selectedDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply {
            time = try { sdf.parse(selectedDate) ?: Date() } catch (e: Exception) { Date() }
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val list = mutableListOf<Pair<String, String>>()
        for (i in 0 until 7) {
            val dStr = sdf.format(cal.time)
            val dayOfWeekName = getBrazilianDayOfWeekName(cal.get(Calendar.DAY_OF_WEEK))
            val brDate = isoToBrazilianDate(dStr)
            list.add(dStr to "$dayOfWeekName ($brDate)")
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        weekDays.forEach { (dateStr, dayLabel) ->
            val dayActs = activities.filter { it.date == dateStr }
            val isCurrentSelectedDay = dateStr == selectedDate

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentSelectedDay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isCurrentSelectedDay) 1.5.dp else 1.dp,
                        if (isCurrentSelectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.clickable { onDateSelected(dateStr) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCurrentSelectedDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (dayActs.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${dayActs.size} eventos",
                                    fontSize = 11.sp,
                                    color = if (dayActs.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (dayActs.isEmpty()) {
                            Text(
                                "Sem compromissos marcados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayActs.forEach { act ->
                                    val disc = act.disciplineId?.let { disciplinesMap[it] }
                                    DashboardActivityCard(
                                        activity = act,
                                        discipline = disc,
                                        onStatusChange = { onStatusChange(act.id, it) },
                                        onGradeChange = { grade -> onGradeChange(act, grade) },
                                        onDateChange = { date -> onDateChange(act, date) },
                                        onClick = { onActivityClick(act) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTimelineView(
    year: Int,
    month: Int,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    activities: List<AcademicActivity>,
    disciplinesMap: Map<Long, Discipline>,
    onActivityClick: (AcademicActivity) -> Unit,
    onStatusChange: (Long, ActivityStatus) -> Unit,
    onGradeChange: (AcademicActivity, Double?) -> Unit,
    onDateChange: (AcademicActivity, String) -> Unit
) {
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = remember(year, month) { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val monthShort = getBrazilianMonthName(month).take(3)

    val dayActivities = activities.filter { it.date == selectedDate }.sortedBy { it.time }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date quick selector for the current navigated month
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..daysInMonth).forEach { day ->
                val dateStr = String.format("%04d-%02d-%02d", year, month, day)
                val isSelected = dateStr == selectedDate
                val count = activities.count { it.date == dateStr }

                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (count > 0 && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.clickable { onDateSelected(dateStr) }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = monthShort,
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = day.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Timeline of the selected day
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (dayActivities.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Nenhuma aula ou atividade para ${isoToBrazilianDate(selectedDate)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(dayActivities) { act ->
                    val disc = act.disciplineId?.let { disciplinesMap[it] }
                    DashboardActivityCard(
                        activity = act,
                        discipline = disc,
                        onStatusChange = { onStatusChange(act.id, it) },
                        onGradeChange = { grade -> onGradeChange(act, grade) },
                        onDateChange = { date -> onDateChange(act, date) },
                        onClick = { onActivityClick(act) }
                    )
                }
            }
        }
    }
}
