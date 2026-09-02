package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority
import com.example.data.repository.DisciplineWithStats
import com.example.ui.components.*
import com.example.ui.viewmodel.AcademicViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: AcademicViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToDisciplines: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToWidget: () -> Unit,
    onOpenAddActivity: () -> Unit
) {
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val disciplineStats by viewModel.disciplineStats.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var activeModal by remember { mutableStateOf<DashboardModalType?>(null) }

    val disciplinesMap = disciplines.associateBy { it.id }

    // Categorized lists for Dashboard
    val todayActivities = activities.filter { it.date == selectedDate }
    val pendingToday = todayActivities.filter { it.status != ActivityStatus.CONCLUIDA }

    val upcomingExams = activities
        .filter { it.activityType == ActivityType.PROVA && it.status != ActivityStatus.CONCLUIDA }
        .sortedBy { "${it.date} ${it.time}" }

    val urgentDeadlines = activities
        .filter {
            it.activityType != ActivityType.AULA &&
            it.activityType != ActivityType.PROVA &&
            it.status != ActivityStatus.CONCLUIDA &&
            (it.priority == Priority.URGENTE || it.priority == Priority.ALTA)
        }
        .sortedBy { "${it.date} ${it.time}" }

    val allAssignments = activities
        .filter { it.activityType != ActivityType.AULA && it.activityType != ActivityType.PROVA }
        .sortedBy { "${it.date} ${it.time}" }

    val delayedActivities = activities
        .filter { it.date < currentDate && it.status != ActivityStatus.CONCLUIDA }
        .sortedBy { it.date }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Professional Polish Top Header & Calendar Strip
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar with Monogram Badge, App Title & Quick Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "A",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Agenda Acadêmica",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "Semestre 2026/2 • Terça, 01 de Setembro",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onNavigateToWidget,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    Icons.Outlined.Widgets,
                                    contentDescription = "Widget da Tela Inicial",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onNavigateToReminders,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Lembretes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Date Subtitle & Quick Today Jump
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Semestre 2026/2 • ${formatDisplayDate(selectedDate)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (selectedDate != currentDate) {
                            TextButton(
                                onClick = { viewModel.setSelectedDate(currentDate) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hoje", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Lateral Scrollable Calendar Strip
                    val calendarDays = remember {
                        generateCalendarDays(
                            startYear = 2026,
                            startMonth = Calendar.AUGUST,
                            startDay = 1,
                            endYear = 2027,
                            endMonth = Calendar.JANUARY,
                            endDay = 31
                        )
                    }

                    val listState = rememberLazyListState()
                    val coroutineScope = rememberCoroutineScope()

                    // Auto scroll to center the selected date on load or when selectedDate changes
                    LaunchedEffect(selectedDate) {
                        val index = calendarDays.indexOfFirst { it.dateStr == selectedDate }
                        if (index >= 0) {
                            val targetIndex = (index - 2).coerceAtLeast(0)
                            listState.animateScrollToItem(targetIndex)
                        }
                    }

                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(calendarDays) { dayItem ->
                            val isSelected = dayItem.dateStr == selectedDate
                            val isToday = dayItem.dateStr == currentDate
                            val dayActs = activities.filter { it.date == dayItem.dateStr }
                            val hasExams = dayActs.any {
                                it.activityType == ActivityType.PROVA ||
                                it.title.contains("prova", ignoreCase = true) ||
                                it.title.contains("avaliação", ignoreCase = true) ||
                                it.title.contains("avaliacao", ignoreCase = true) ||
                                it.title.contains("exame", ignoreCase = true) ||
                                it.title.contains("teste", ignoreCase = true)
                            }
                            val hasOnlyClasses = dayActs.isNotEmpty() && dayActs.all { it.activityType == ActivityType.AULA }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else Color.Transparent
                                        )
                                        .clickable { viewModel.setSelectedDate(dayItem.dateStr) }
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dayItem.dayLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(5.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayItem.dayNum,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White
                                            else if (isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Dot indicator: vazio para dias livres, verde para apenas aulas, vermelho para provas/avaliações
                                    if (dayActs.isEmpty()) {
                                        Spacer(modifier = Modifier.height(5.dp))
                                    } else {
                                        val dotColor = when {
                                            hasExams -> Color(0xFFBA1A1A) // Ponto vermelho para provas e avaliações
                                            hasOnlyClasses -> Color(0xFF006E1C) // Ponto verde para dias apenas com aulas
                                            else -> Color(0xFF0288D1) // Ponto azul para trabalhos/entregas
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                }

                                // Visual demarcation at the end of each month
                                if (dayItem.isLastDayOfMonth) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    MonthEndDemarcation(
                                        currentMonth = dayItem.monthName,
                                        nextMonth = dayItem.nextMonthName
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Summary KPIs (Compromissos, Provas, Matérias) - Clickable to open Modals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card Compromissos (Clickable Modal Trigger)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeModal = DashboardModalType.COMPROMISSOS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Compromissos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${todayActivities.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = if (pendingToday.isNotEmpty()) "${pendingToday.size} pendentes" else "Tudo feito",
                            fontSize = 11.sp,
                            color = if (pendingToday.isNotEmpty()) Color(0xFF8B5000) else Color(0xFF006E1C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card Provas (Clickable Modal Trigger)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeModal = DashboardModalType.PROVAS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFBA1A1A)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Provas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${upcomingExams.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val nextExam = upcomingExams.firstOrNull()
                        Text(
                            text = if (nextExam != null) "Próx: ${formatDdMm(nextExam.date)}" else "Nenhuma",
                            fontSize = 11.sp,
                            color = Color(0xFFBA1A1A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Card Matérias (Clickable Modal Trigger)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeModal = DashboardModalType.MATERIAS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF006E1C)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Matérias", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(11.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${disciplines.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Progresso →",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Delayed Activities Warning (if any)
        if (delayedActivities.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, Color(0xFFFFB4AB))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Atenção: ${delayedActivities.size} atividade(s) pendente(s) do passado", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(delayedActivities.first().title, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section: Atividades do Dia Selecionado
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDate == currentDate) "Atividades de Hoje" else "Atividades de $selectedDate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenAddActivity,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = onNavigateToCalendar,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Calendário", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (todayActivities.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma atividade para este dia.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            items(todayActivities) { activity ->
                val disc = disciplinesMap[activity.disciplineId]
                DashboardActivityCard(
                    activity = activity,
                    discipline = disc,
                    onStatusChange = { newStatus -> viewModel.updateActivityStatus(activity.id, newStatus) },
                    onClick = { viewModel.selectActivity(activity) }
                )
            }
        }

        // Section: Mais Informações do Semestre (Módulos em Modais para Reduzir Poluição Visual)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mais Informações do Semestre",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Compact Expandable Cards triggering Modals
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Card 1: Próximas Provas Modal Trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeModal = DashboardModalType.PROVAS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFBA1A1A).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Próximas Provas & Avaliações", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${upcomingExams.size} provas agendadas no semestre", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFBA1A1A).copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expandir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A))
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Card 2: Trabalhos e Prazos Modal Trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeModal = DashboardModalType.TRABALHOS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF8B5000).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF8B5000), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trabalhos & Prazos Próximos", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${allAssignments.size} trabalhos e entregas pendentes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF8B5000).copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expandir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5000))
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8B5000), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Card 3: Progresso nas Disciplinas Modal Trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeModal = DashboardModalType.MATERIAS },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Progresso nas Disciplinas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("${disciplines.size} matérias cadastradas no semestre", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Expandir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Card 4: Widget da Tela Inicial (Ativar e Fixar)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToWidget),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Widget da Tela Inicial", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Personalizar e fixar widget no seu celular", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Abrir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Triggered by Cards
    if (activeModal != null) {
        DashboardModalBottomSheet(
            modalType = activeModal!!,
            onDismiss = { activeModal = null },
            activities = activities,
            disciplines = disciplines,
            disciplinesMap = disciplinesMap,
            disciplineStats = disciplineStats,
            selectedDate = selectedDate,
            onSelectActivity = { act ->
                viewModel.selectActivity(act)
                activeModal = null
            },
            onStatusChange = { id, s -> viewModel.updateActivityStatus(id, s) },
            onNavigateToDisciplines = {
                activeModal = null
                onNavigateToDisciplines()
            },
            onOpenAddActivity = {
                activeModal = null
                onOpenAddActivity()
            },
            onSelectDiscipline = { disc ->
                viewModel.selectDiscipline(disc)
                activeModal = null
                onNavigateToDisciplines()
            }
        )
    }
}

@Composable
fun DashboardActivityCard(
    activity: AcademicActivity,
    discipline: Discipline?,
    onStatusChange: (ActivityStatus) -> Unit,
    onClick: () -> Unit
) {
    val discColor = parseColorHex(discipline?.colorHex ?: "#0061A4")
    val isCompleted = activity.status == ActivityStatus.CONCLUIDA
    val isCancelled = activity.status == ActivityStatus.CANCELADA
    val isFalta = activity.status == ActivityStatus.FALTA
    val isUrgent = activity.priority == Priority.URGENTE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCancelled) 0.55f else 1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCancelled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            if (isFalta) 1.5.dp else 1.dp,
            when {
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.5f)
                isCancelled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Time and Period
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(58.dp)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = activity.time,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isCompleted || isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = getTimePeriod(activity.time),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.4.sp
                )
            }

            // Vertical subtle divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Center Column: Discipline & Title & Location
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isCancelled) Color(0xFF5F6368) else discColor)
                    )
                    Text(
                        text = (discipline?.name ?: "GERAL").uppercase(),
                        color = if (isCancelled) Color(0xFF5F6368) else discColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                    ActivityTypeBadge(type = activity.activityType)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isCompleted || isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCancelled || isUrgent || discipline?.classroom?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCancelled) {
                            Surface(
                                color = Color(0xFF5F6368).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color(0xFF5F6368),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "CANCELADA",
                                        color = Color(0xFF5F6368),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (isUrgent && !isCancelled) {
                            Surface(
                                color = Color(0xFFFFDAD6),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "URGENTE",
                                    color = Color(0xFF410002),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (discipline?.classroom?.isNotBlank() == true) {
                            Text(
                                text = discipline.classroom,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Status Action Icon
            IconButton(
                onClick = {
                    val nextStatus = when (activity.status) {
                        ActivityStatus.PENDENTE -> ActivityStatus.EM_ANDAMENTO
                        ActivityStatus.EM_ANDAMENTO -> ActivityStatus.CONCLUIDA
                        ActivityStatus.CONCLUIDA -> ActivityStatus.PENDENTE
                        ActivityStatus.FALTA -> ActivityStatus.PENDENTE
                        ActivityStatus.CANCELADA -> ActivityStatus.PENDENTE
                    }
                    onStatusChange(nextStatus)
                },
                modifier = Modifier.size(36.dp)
            ) {
                when (activity.status) {
                    ActivityStatus.CONCLUIDA -> Icon(Icons.Default.CheckCircle, contentDescription = "Concluída", tint = Color(0xFF006E1C))
                    ActivityStatus.EM_ANDAMENTO -> Icon(Icons.Default.PlayCircle, contentDescription = "Em andamento", tint = Color(0xFF005AC1))
                    ActivityStatus.FALTA -> Icon(Icons.Default.Cancel, contentDescription = "Falta", tint = Color(0xFFBA1A1A))
                    ActivityStatus.CANCELADA -> Icon(Icons.Default.EventBusy, contentDescription = "Cancelada", tint = Color(0xFF5F6368))
                    ActivityStatus.PENDENTE -> Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = "Pendente", tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/**
 * Model representing a single day in the lateral scrollable calendar strip.
 */
data class CalendarDayItem(
    val dateStr: String,
    val dayLabel: String,
    val dayNum: String,
    val monthName: String,
    val nextMonthName: String,
    val isLastDayOfMonth: Boolean
)

/**
 * Generates continuous days across the requested date range.
 */
fun generateCalendarDays(
    startYear: Int,
    startMonth: Int,
    startDay: Int,
    endYear: Int,
    endMonth: Int,
    endDay: Int
): List<CalendarDayItem> {
    val list = mutableListOf<CalendarDayItem>()
    val cal = Calendar.getInstance().apply {
        set(startYear, startMonth, startDay, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
        set(endYear, endMonth, endDay, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dayNames = arrayOf("DOM", "SEG", "TER", "QUA", "QUI", "SEX", "SÁB")
    val monthNames = arrayOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    while (!cal.after(endCal)) {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed DOM..SÁB

        val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
        val dayLabel = dayNames.getOrElse(dayOfWeek) { "DIA" }
        val dayNum = String.format("%02d", day)
        val maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isLastDay = day == maxDayOfMonth
        val nextMonthIdx = if (month == 11) 0 else month + 1

        list.add(
            CalendarDayItem(
                dateStr = dateStr,
                dayLabel = dayLabel,
                dayNum = dayNum,
                monthName = monthNames.getOrElse(month) { "" },
                nextMonthName = monthNames.getOrElse(nextMonthIdx) { "" },
                isLastDayOfMonth = isLastDay
            )
        )
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return list
}

/**
 * Formats a "YYYY-MM-DD" string into a Portuguese display title.
 */
fun formatDisplayDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val monthNames = arrayOf(
                "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
            )
            val monthName = monthNames.getOrElse(month - 1) { "Mês $month" }
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            val dayOfWeekNames = arrayOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
            val dayOfWeek = dayOfWeekNames.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "" }
            "$dayOfWeek, ${String.format("%02d", day)} de $monthName"
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * Visual demarcation card marking the transition between months in the lateral calendar strip.
 */
@Composable
fun MonthEndDemarcation(
    currentMonth: String,
    nextMonth: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left accent line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Fim de $currentMonth",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Próximo: $nextMonth",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right accent line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
    }
}

enum class DashboardModalType {
    COMPROMISSOS,
    PROVAS,
    MATERIAS,
    TRABALHOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardModalBottomSheet(
    modalType: DashboardModalType,
    onDismiss: () -> Unit,
    activities: List<AcademicActivity>,
    disciplines: List<Discipline>,
    disciplinesMap: Map<Long, Discipline>,
    disciplineStats: List<DisciplineWithStats>,
    selectedDate: String,
    onSelectActivity: (AcademicActivity) -> Unit,
    onStatusChange: (Long, ActivityStatus) -> Unit,
    onNavigateToDisciplines: () -> Unit,
    onOpenAddActivity: () -> Unit,
    onSelectDiscipline: (Discipline) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            when (modalType) {
                DashboardModalType.COMPROMISSOS -> {
                    var filterIndex by remember { mutableStateOf(0) }
                    val filteredList = when (filterIndex) {
                        0 -> activities.filter { it.date == selectedDate }
                        1 -> activities.filter { it.status != ActivityStatus.CONCLUIDA }.sortedBy { "${it.date} ${it.time}" }
                        else -> activities.sortedBy { "${it.date} ${it.time}" }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Compromissos & Atividades",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${filteredList.size} compromissos encontrados",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Dia ($selectedDate)", "Pendentes", "Todos").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = filterIndex == idx,
                                onClick = { filterIndex = idx },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum compromisso para este filtro.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 450.dp)
                        ) {
                            items(filteredList) { act ->
                                val disc = disciplinesMap[act.disciplineId]
                                DashboardActivityCard(
                                    activity = act,
                                    discipline = disc,
                                    onStatusChange = { newS -> onStatusChange(act.id, newS) },
                                    onClick = { onSelectActivity(act) }
                                )
                            }
                        }
                    }
                }

                DashboardModalType.PROVAS -> {
                    val exams = activities.filter { it.activityType == ActivityType.PROVA }.sortedBy { "${it.date} ${it.time}" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFBA1A1A).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Próximas Provas & Avaliações",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${exams.size} avaliações cadastradas no semestre",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (exams.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma prova agendada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 450.dp)
                        ) {
                            items(exams) { exam ->
                                val disc = disciplinesMap[exam.disciplineId]
                                DashboardActivityCard(
                                    activity = exam,
                                    discipline = disc,
                                    onStatusChange = { newS -> onStatusChange(exam.id, newS) },
                                    onClick = { onSelectActivity(exam) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onOpenAddActivity,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agendar Nova Prova / Avaliação", fontWeight = FontWeight.Bold)
                    }
                }

                DashboardModalType.MATERIAS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Disciplinas & Progresso",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${disciplines.size} matérias do Semestre 2026/2",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 450.dp)
                    ) {
                        items(disciplineStats) { stat ->
                            val disc = stat.discipline
                            val discColor = parseColorHex(disc.colorHex)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDiscipline(disc) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(discColor))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(disc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = "${(stat.progressFraction * 100).toInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = discColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { stat.progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = discColor,
                                        trackColor = discColor.copy(alpha = 0.15f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${stat.completedActivities}/${stat.totalActivities} concluídas • ${disc.professor}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Ver detalhes →",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onNavigateToDisciplines,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ir para Tela de Disciplinas", fontWeight = FontWeight.Bold)
                    }
                }

                DashboardModalType.TRABALHOS -> {
                    val assignments = activities
                        .filter { it.activityType != ActivityType.AULA && it.activityType != ActivityType.PROVA }
                        .sortedBy { "${it.date} ${it.time}" }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5000).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF8B5000), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Trabalhos & Prazos Próximos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${assignments.size} trabalhos e prazos registrados",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (assignments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhum trabalho pendente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 450.dp)
                        ) {
                            items(assignments) { item ->
                                val disc = disciplinesMap[item.disciplineId]
                                DashboardActivityCard(
                                    activity = item,
                                    discipline = disc,
                                    onStatusChange = { newS -> onStatusChange(item.id, newS) },
                                    onClick = { onSelectActivity(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDdMm(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size >= 3) {
        "${parts[2]}-${parts[1]}"
    } else {
        isoDate
    }
}
