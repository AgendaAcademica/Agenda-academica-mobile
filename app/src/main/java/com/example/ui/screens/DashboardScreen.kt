package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
import java.util.Calendar

@Composable
fun DashboardScreen(
    viewModel: AcademicViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToDisciplines: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToWidget: () -> Unit,
    onOpenAddActivity: () -> Unit
) {
    val context = LocalContext.current
    val profilePreferences = remember { context.getSharedPreferences("academic_profile", android.content.Context.MODE_PRIVATE) }
    val tutorialPreferences = remember { context.getSharedPreferences("app_tutorial", android.content.Context.MODE_PRIVATE) }
    var userName by remember { mutableStateOf(profilePreferences.getString("user_name", "Estudante") ?: "Estudante") }
    var userNameDraft by remember { mutableStateOf(userName) }
    var showUserNameDialog by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(!tutorialPreferences.getBoolean("completed", false)) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val disciplineStats by viewModel.disciplineStats.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val calendarDisciplineFilter by viewModel.calendarDisciplineFilter.collectAsState()

    var activeModal by remember { mutableStateOf<DashboardModalType?>(null) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var isDisciplineFilterExpanded by remember { mutableStateOf(false) }

    val disciplinesMap = disciplines.associateBy { it.id }
    val calendarActivities = remember(activities, calendarDisciplineFilter) {
        filterCalendarActivities(activities, calendarDisciplineFilter)
    }

    LaunchedEffect(disciplines, calendarDisciplineFilter) {
        if (calendarDisciplineFilter != null && disciplines.none { it.id == calendarDisciplineFilter }) {
            viewModel.setCalendarDisciplineFilter(null)
        }
    }

    // Categorized lists for Dashboard
    val todayActivities = calendarActivities.filter { it.date == selectedDate }
    val upcomingTestsAndAssignments = activities
        .filter { it.activityType != ActivityType.AULA && it.status != ActivityStatus.CONCLUIDA }
        .sortedBy { "${it.date} ${it.time}" }

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
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .animateContentSize()
                ) {
                    // Header Bar with Monogram Badge, App Title & Quick Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .clickable {
                                    userNameDraft = userName
                                    showUserNameDialog = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Olá, $userName",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatDisplayDate(currentDate),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    tutorialStep = 0
                                    showTutorial = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = "Abrir tutorial",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onToggleTheme) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                    contentDescription = if (isDarkTheme) "Ativar modo claro" else "Ativar modo escuro",
                                    tint = MaterialTheme.colorScheme.primary
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
                            fontSize = 14.sp,
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
                                Text("Hoje", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDisciplineFilterExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = disciplinesMap[calendarDisciplineFilter]?.name ?: "Todas as disciplinas",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar disciplina")
                        }
                        DropdownMenu(
                            expanded = isDisciplineFilterExpanded,
                            onDismissRequest = { isDisciplineFilterExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas as disciplinas") },
                                leadingIcon = {
                                    if (calendarDisciplineFilter == null) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    viewModel.setCalendarDisciplineFilter(null)
                                    isDisciplineFilterExpanded = false
                                }
                            )
                            disciplines.forEach { discipline ->
                                DropdownMenuItem(
                                    text = { Text(discipline.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = {
                                        if (calendarDisciplineFilter == discipline.id) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setCalendarDisciplineFilter(discipline.id)
                                        isDisciplineFilterExpanded = false
                                    }
                                )
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

                    val weekDays = remember(selectedDate, calendarDays) {
                        calendarWeekFor(calendarDays, selectedDate)
                    }

                    if (isCalendarExpanded) {
                        ExpandedMonthCalendar(
                            allDays = calendarDays,
                            selectedDate = selectedDate,
                            currentDate = currentDate,
                            activities = calendarActivities,
                            onSelectDate = viewModel::setSelectedDate
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(weekDays) { dayItem ->
                                val isSelected = dayItem.dateStr == selectedDate
                                val isToday = dayItem.dateStr == currentDate
                                val dayActs = calendarActivities.filter { it.date == dayItem.dateStr }
                                val indicatorColor = calendarDayIndicatorColor(dayActs)

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
                                            fontSize = 13.sp,
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
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White
                                                else if (isToday) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Dot indicator: vazio para dias livres, verde para apenas aulas, vermelho para provas/avaliações
                                        if (indicatorColor == null) {
                                            Spacer(modifier = Modifier.height(5.dp))
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(indicatorColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { isCalendarExpanded = !isCalendarExpanded },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = if (isCalendarExpanded) Icons.Default.ExpandLess else Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCalendarExpanded) "Recolher calendário" else "Expandir calendário",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                }
            }
        }

        // Resumo compacto de provas, trabalhos e prazos
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { activeModal = DashboardModalType.PROVAS },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFBA1A1A).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Provas e trabalhos", color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        val nextItem = upcomingTestsAndAssignments.firstOrNull()
                        Text(
                            if (nextItem != null) "${upcomingTestsAndAssignments.size} pendentes • próximo em ${formatDdMm(nextItem.date)}" else "Nenhuma prova ou trabalho pendente",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
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
                        fontSize = 18.sp,
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
                        Text("Adicionar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (todayActivities.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                                fontSize = 15.sp
                            )
                        }
                    }
                    } else {
                        todayActivities.forEach { activity ->
                            val disc = disciplinesMap[activity.disciplineId]
                            DashboardActivityCard(
                                activity = activity,
                                discipline = disc,
                                onStatusChange = { newStatus -> viewModel.updateActivityStatus(activity.id, newStatus) },
                                onGradeChange = { grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
                                onDateChange = { date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) },
                                onClick = { viewModel.selectActivity(activity) }
                            )
                        }
                    }
                }
            }
        }

    }

    if (showUserNameDialog) {
        AlertDialog(
            onDismissRequest = { showUserNameDialog = false },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            title = { Text("Seu nome") },
            text = {
                OutlinedTextField(
                    value = userNameDraft,
                    onValueChange = { if (it.length <= 30) userNameDraft = it },
                    label = { Text("Como deseja ser chamado?") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = userNameDraft.isNotBlank(),
                    onClick = {
                        userName = userNameDraft.trim()
                        profilePreferences.edit().putString("user_name", userName).apply()
                        showUserNameDialog = false
                    }
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showUserNameDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showTutorial) {
        DashboardTutorialDialog(
            step = tutorialStep,
            onPrevious = { tutorialStep = (tutorialStep - 1).coerceAtLeast(0) },
            onNext = { tutorialStep = (tutorialStep + 1).coerceAtMost(DASHBOARD_TUTORIAL_LAST_STEP) },
            onDismiss = {
                tutorialPreferences.edit().putBoolean("completed", true).apply()
                showTutorial = false
            },
            onFinishAndImport = {
                tutorialPreferences.edit().putBoolean("completed", true).apply()
                showTutorial = false
                onNavigateToImport()
            }
        )
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
            onGradeChange = { activity, grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
            onDateChange = { activity, date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) },
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

private const val DASHBOARD_TUTORIAL_LAST_STEP = 4

@Composable
private fun DashboardTutorialDialog(
    step: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    onFinishAndImport: () -> Unit
) {
    val (title, description, icon) = when (step) {
        0 -> Triple(
            "Personalize seu nome",
            "Toque no seu nome ou no ícone de perfil, no topo da tela inicial, para escolher como deseja ser chamado.",
            Icons.Default.Person
        )
        1 -> Triple(
            "Escolha o tema",
            "Use o ícone de lua ou sol, no canto superior direito, para alternar rapidamente entre os modos claro e escuro.",
            Icons.Outlined.DarkMode
        )
        2 -> Triple(
            "Cadastre suas disciplinas",
            "Abra Disciplinas na barra inferior para registrar matérias, horários e salas. As aulas podem ser adicionadas automaticamente ao calendário.",
            Icons.Outlined.MenuBook
        )
        3 -> Triple(
            "Organize suas atividades",
            "Use Adicionar na tela inicial para criar provas, trabalhos e compromissos. Toque em um dia do calendário para consultar sua programação.",
            Icons.Outlined.EventNote
        )
        else -> Triple(
            "Importe arquivos com IA",
            "Em Importar IA, selecione PDFs, fotos ou capturas de tela. O app analisa o conteúdo e permite revisar os eventos antes de salvá-los.",
            Icons.Outlined.DocumentScanner
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp).size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Passo ${step + 1} de ${DASHBOARD_TUTORIAL_LAST_STEP + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = title, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { (step + 1f) / (DASHBOARD_TUTORIAL_LAST_STEP + 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            if (step == DASHBOARD_TUTORIAL_LAST_STEP) {
                Button(onClick = onFinishAndImport) {
                    Text("Experimentar agora")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(onClick = onNext) {
                    Text("Próximo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    TextButton(onClick = onPrevious) { Text("Voltar") }
                }
                TextButton(onClick = onDismiss) {
                    Text(if (step == DASHBOARD_TUTORIAL_LAST_STEP) "Concluir" else "Pular")
                }
            }
        }
    )
}

@Composable
fun DashboardActivityCard(
    activity: AcademicActivity,
    discipline: Discipline?,
    onStatusChange: (ActivityStatus) -> Unit,
    onGradeChange: (Double?) -> Unit,
    onDateChange: (String) -> Unit,
    onClick: () -> Unit
) {
    val discColor = parseColorHex(discipline?.colorHex ?: "#0061A4")
    val isCompleted = activity.status == ActivityStatus.CONCLUIDA
    val isCancelled = activity.status == ActivityStatus.CANCELADA
    val isFalta = activity.status == ActivityStatus.FALTA
    val isUrgent = activity.priority == Priority.URGENTE
    var showQuickGradeDialog by remember(activity.id) { mutableStateOf(false) }
    var showQuickDateDialog by remember(activity.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCancelled) 0.55f else 1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCancelled -> discColor.copy(alpha = 0.10f)
                isCompleted -> discColor.copy(alpha = 0.12f)
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.12f)
                else -> discColor.copy(alpha = 0.20f)
            }
        ),
        border = BorderStroke(
            if (isFalta) 1.5.dp else 1.dp,
            when {
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.5f)
                isCancelled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                else -> discColor.copy(alpha = 0.80f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
            // Left Column: Time and Period
            Surface(
                modifier = Modifier
                    .width(78.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = isoToBrazilianDate(activity.date),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = activity.time.ifBlank { "--:--" },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = getTimePeriod(activity.time),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        letterSpacing = 0.4.sp
                    )
                    if (!activity.originalDate.isNullOrBlank() && activity.originalDate != activity.date) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("DATA ALTERADA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                    }
                }
            }

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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    ActivityTypeBadge(type = activity.activityType)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isCompleted || isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (activity.activityType == ActivityType.PROVA && activity.grade != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "NOTA ${if (activity.grade % 1.0 == 0.0) activity.grade.toInt() else String.format(java.util.Locale.getDefault(), "%.1f", activity.grade)}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            color = Color(0xFF166534),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

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
                                        fontSize = 11.sp,
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
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (discipline?.classroom?.isNotBlank() == true) {
                            Text(
                                text = discipline.classroom,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (activity.activityType != ActivityType.AULA) {
                Spacer(modifier = Modifier.width(8.dp))

                // Quick Status Action Icon for assessments and assignments
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

            if (activity.activityType == ActivityType.AULA) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onStatusChange(if (isCancelled) ActivityStatus.PENDENTE else ActivityStatus.CANCELADA)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCancelled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = if (isCancelled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCancelled) "Cancelada" else "Aula cancelada", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onStatusChange(if (isFalta) ActivityStatus.PENDENTE else ActivityStatus.FALTA)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFalta) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.58f),
                            contentColor = if (isFalta) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFalta) "Falta marcada" else "Marcar falta", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (activity.activityType == ActivityType.PROVA) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showQuickDateDialog = true },
                        modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alterar data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showQuickGradeDialog = true },
                        modifier = Modifier.weight(1f).heightIn(min = 38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (activity.grade == null) "Lançar nota" else "Editar nota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showQuickGradeDialog) {
        QuickGradeDialog(
            activity = activity,
            onDismiss = { showQuickGradeDialog = false },
            onSave = { grade ->
                onGradeChange(grade)
                showQuickGradeDialog = false
            }
        )
    }
    if (showQuickDateDialog) {
        QuickDateDialog(
            activity = activity,
            onDismiss = { showQuickDateDialog = false },
            onSave = { date ->
                onDateChange(date)
                showQuickDateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDateDialog(
    activity: AcademicActivity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val initialMillis = remember(activity.date) {
        runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(activity.date)?.time
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.format(java.util.Date(millis))
                        onSave(date)
                    }
                }
            ) { Text("Salvar data") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = datePickerState, title = { Text("Alterar data da prova", modifier = Modifier.padding(24.dp)) })
    }
}

@Composable
private fun QuickGradeDialog(
    activity: AcademicActivity,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var gradeText by remember(activity.id, activity.grade) {
        mutableStateOf(activity.grade?.toString()?.replace('.', ',') ?: "")
    }
    val parsedGrade = gradeText.replace(',', '.').toDoubleOrNull()
    val isValid = gradeText.isBlank() || (parsedGrade != null && parsedGrade in 0.0..10.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Grade, contentDescription = null) },
        title = { Text(if (activity.grade == null) "Lançar nota" else "Editar nota") },
        text = {
            OutlinedTextField(
                value = gradeText,
                onValueChange = { value ->
                    if (value.length <= 5 && value.all { it.isDigit() || it == ',' || it == '.' }) {
                        gradeText = value
                    }
                },
                label = { Text("Nota de 0 a 10") },
                placeholder = { Text("Ex.: 8,5") },
                supportingText = {
                    if (!isValid) Text("Informe uma nota entre 0 e 10")
                },
                isError = !isValid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = gradeText.isNotBlank() && isValid,
                onClick = { onSave(parsedGrade) }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (activity.grade != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remover nota", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
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

fun calendarWeekFor(allDays: List<CalendarDayItem>, selectedDate: String): List<CalendarDayItem> {
    val selectedIndex = allDays.indexOfFirst { it.dateStr == selectedDate }
    if (selectedIndex < 0) return allDays.take(7)
    val day = allDays[selectedIndex]
    val mondayOffset = when (day.dayLabel) {
        "SEG" -> 0; "TER" -> 1; "QUA" -> 2; "QUI" -> 3
        "SEX" -> 4; "SÁB" -> 5; else -> 6
    }
    val start = (selectedIndex - mondayOffset).coerceAtLeast(0)
    return allDays.drop(start).take(7)
}

internal fun filterCalendarActivities(
    activities: List<AcademicActivity>,
    disciplineId: Long?
): List<AcademicActivity> = if (disciplineId == null) {
    activities
} else {
    activities.filter { it.disciplineId == disciplineId }
}

private fun calendarDayIndicatorColor(dayActivities: List<AcademicActivity>): Color? {
    if (dayActivities.isEmpty()) return null

    val hasExams = dayActivities.any {
        it.activityType == ActivityType.PROVA ||
            (it.activityType != ActivityType.AULA && (
                it.title.contains("prova", ignoreCase = true) ||
                    it.title.contains("avaliação", ignoreCase = true) ||
                    it.title.contains("avaliacao", ignoreCase = true) ||
                    it.title.contains("exame", ignoreCase = true) ||
                    it.title.contains("teste", ignoreCase = true)
                ))
    }
    val hasOnlyClasses = dayActivities.all { it.activityType == ActivityType.AULA }

    return when {
        hasExams -> Color(0xFFBA1A1A)
        hasOnlyClasses -> Color(0xFF006E1C)
        else -> Color(0xFF0288D1)
    }
}

@Composable
private fun ExpandedMonthCalendar(
    allDays: List<CalendarDayItem>,
    selectedDate: String,
    currentDate: String,
    activities: List<AcademicActivity>,
    onSelectDate: (String) -> Unit
) {
    val availableMonths = remember(allDays) {
        allDays.map { it.dateStr.take(7) }.distinct()
    }
    var visibleMonth by remember(selectedDate) {
        mutableStateOf(selectedDate.take(7))
    }
    val visibleMonthIndex = availableMonths.indexOf(visibleMonth)
    val canNavigatePrevious = visibleMonthIndex > 0
    val canNavigateNext = visibleMonthIndex in 0 until availableMonths.lastIndex
    val monthDays = allDays.filter { it.dateStr.startsWith(visibleMonth) }
    val firstOffset = when (monthDays.firstOrNull()?.dayLabel) {
        "SEG" -> 0; "TER" -> 1; "QUA" -> 2; "QUI" -> 3
        "SEX" -> 4; "SÁB" -> 5; else -> 6
    }
    val cells: List<CalendarDayItem?> = List(firstOffset) { null } + monthDays
    val monthTitle = monthDays.firstOrNull()?.monthName.orEmpty()
    val year = visibleMonth.take(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (canNavigatePrevious) {
                        visibleMonth = availableMonths[visibleMonthIndex - 1]
                    }
                },
                enabled = canNavigatePrevious,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Mês anterior",
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "$monthTitle $year",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    if (canNavigateNext) {
                        visibleMonth = availableMonths[visibleMonthIndex + 1]
                    }
                },
                enabled = canNavigateNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Próximo mês",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        cells.chunked(7).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth()) {
                (rowCells + List(7 - rowCells.size) { null }).forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val selected = day.dateStr == selectedDate
                            val today = day.dateStr == currentDate
                            val indicatorColor = calendarDayIndicatorColor(
                                activities.filter { it.date == day.dateStr }
                            )
                            Column(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            selected -> MaterialTheme.colorScheme.primary
                                            today -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDate(day.dateStr) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayNum,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (indicatorColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarIndicatorLegendItem(Color(0xFF006E1C), "Aulas")
            CalendarIndicatorLegendItem(Color(0xFF0288D1), "Trabalhos")
            CalendarIndicatorLegendItem(Color(0xFFBA1A1A), "Provas")
        }
    }
}

@Composable
private fun CalendarIndicatorLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
    onGradeChange: (AcademicActivity, Double?) -> Unit,
    onDateChange: (AcademicActivity, String) -> Unit,
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
                                    onGradeChange = { grade -> onGradeChange(act, grade) },
                                    onDateChange = { date -> onDateChange(act, date) },
                                    onClick = { onSelectActivity(act) }
                                )
                            }
                        }
                    }
                }

                DashboardModalType.PROVAS -> {
                    val examsAndAssignments = activities
                        .filter { it.activityType != ActivityType.AULA }
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
                                    .background(Color(0xFFBA1A1A).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Provas e trabalhos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${examsAndAssignments.size} provas, trabalhos e prazos",
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

                    if (examsAndAssignments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma prova ou trabalho cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 450.dp)
                        ) {
                            items(examsAndAssignments) { exam ->
                                val disc = disciplinesMap[exam.disciplineId]
                                DashboardActivityCard(
                                    activity = exam,
                                    discipline = disc,
                                    onStatusChange = { newS -> onStatusChange(exam.id, newS) },
                                    onGradeChange = { grade -> onGradeChange(exam, grade) },
                                    onDateChange = { date -> onDateChange(exam, date) },
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
                        Text("Adicionar prova ou trabalho", fontWeight = FontWeight.Bold)
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
                                    onGradeChange = { grade -> onGradeChange(item, grade) },
                                    onDateChange = { date -> onDateChange(item, date) },
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
