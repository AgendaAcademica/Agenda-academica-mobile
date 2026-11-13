package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.ui.components.*
import com.example.ui.viewmodel.AcademicViewModel

private fun abbreviateScheduleDays(schedule: String): String {
    val days = listOf(
        "segunda(?:[- ]feira)?" to "Seg",
        "ter[çc]a(?:[- ]feira)?" to "Ter",
        "quarta(?:[- ]feira)?" to "Qua",
        "quinta(?:[- ]feira)?" to "Qui",
        "sexta(?:[- ]feira)?" to "Sex",
        "s[áa]bado" to "Sáb",
        "domingo" to "Dom"
    )
    return days.fold(schedule) { text, (day, abbreviation) ->
        Regex("\\b$day\\b", RegexOption.IGNORE_CASE).replace(text, abbreviation)
    }
}

@Composable
fun DisciplinesScreen(
    viewModel: AcademicViewModel,
    onOpenAddDiscipline: () -> Unit,
    onOpenEditDiscipline: (Discipline) -> Unit,
    onOpenAddActivityForDiscipline: (Long) -> Unit
) {
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val selectedDiscipline by viewModel.selectedDiscipline.collectAsState()
    val disciplineStats by viewModel.disciplineStats.collectAsState()

    val disciplinesMap = disciplines.associateBy { it.id }

    if (selectedDiscipline != null) {
        DisciplineDetailView(
            discipline = selectedDiscipline!!,
            activities = activities.filter { it.disciplineId == selectedDiscipline!!.id },
            documents = documents.filter { it.disciplineId == selectedDiscipline!!.id },
            onBack = { viewModel.selectDiscipline(null) },
            onEdit = { onOpenEditDiscipline(selectedDiscipline!!) },
            onDelete = { viewModel.deleteDiscipline(selectedDiscipline!!.id) },
            onAddActivity = { onOpenAddActivityForDiscipline(selectedDiscipline!!.id) },
            onActivityClick = { act -> viewModel.selectActivity(act) },
            onStatusChange = { id, s -> viewModel.updateActivityStatus(id, s) },
            onGradeChange = { activity, grade -> viewModel.saveActivity(activity.copy(grade = grade)) },
            onDateChange = { activity, date -> viewModel.saveActivity(activity.copy(originalDate = activity.originalDate ?: activity.date, date = date)) }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Disciplinas do Semestre",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${disciplines.size} matérias cadastradas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onOpenAddDiscipline,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nova", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(disciplineStats) { stat ->
                val disc = stat.discipline
                val discColor = parseColorHex(disc.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectDiscipline(disc) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Top Color Accent Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(discColor)
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(discColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = null,
                                            tint = discColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = disc.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (disc.code.isNotBlank()) {
                                            Text(
                                                text = "${disc.code} • Semestre ${disc.term}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = discColor.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${(stat.progressFraction * 100).toInt()}%",
                                        color = discColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (disc.professor.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(disc.professor, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (disc.schedule.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(abbreviateScheduleDays(disc.schedule), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { stat.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = discColor,
                                trackColor = discColor.copy(alpha = 0.12f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${stat.completedActivities}/${stat.totalActivities} concluídas",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Toque para detalhes →",
                                    fontSize = 11.sp,
                                    color = discColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisciplineDetailView(
    discipline: Discipline,
    activities: List<AcademicActivity>,
    documents: List<com.example.data.model.AcademicDocument>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddActivity: () -> Unit,
    onActivityClick: (AcademicActivity) -> Unit,
    onStatusChange: (Long, ActivityStatus) -> Unit,
    onGradeChange: (AcademicActivity, Double?) -> Unit,
    onDateChange: (AcademicActivity, String) -> Unit
) {
    val context = LocalContext.current
    val tutorialPreferences = remember {
        context.getSharedPreferences("discipline_tutorial", android.content.Context.MODE_PRIVATE)
    }
    val discColor = parseColorHex(discipline.colorHex)
    val headerContentColor = Color.Black
    val exams = activities.filter { it.activityType == ActivityType.PROVA }
    val classes = activities.filter { it.activityType == ActivityType.AULA }
    val assignments = activities.filter { it.activityType != ActivityType.PROVA && it.activityType != ActivityType.AULA }

    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDisciplineTutorial by remember(discipline.id) {
        mutableStateOf(!tutorialPreferences.getBoolean("completed", false))
    }
    var tutorialStep by remember(discipline.id) { mutableIntStateOf(0) }
    val tabTitles = listOf("Atividades (${assignments.size})", "Provas (${exams.size})", "Aulas (${classes.size})", "Documentos (${documents.size})")

    LaunchedEffect(showDisciplineTutorial, tutorialStep) {
        if (showDisciplineTutorial) {
            selectedTab = when (tutorialStep) {
                2 -> 1
                3 -> 2
                else -> 0
            }
        }
    }

    fun finishTutorial() {
        tutorialPreferences.edit().putBoolean("completed", true).apply()
        showDisciplineTutorial = false
    }

    if (showDisciplineTutorial) {
        DisciplineTutorialDialog(
            step = tutorialStep,
            onPrevious = { tutorialStep = (tutorialStep - 1).coerceAtLeast(0) },
            onNext = { tutorialStep = (tutorialStep + 1).coerceAtMost(DISCIPLINE_TUTORIAL_LAST_STEP) },
            onDismiss = ::finishTutorial,
            onFinish = ::finishTutorial
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Excluir Disciplina?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Tem certeza de que deseja excluir a disciplina \"${discipline.name}\"? Todas as aulas, provas, trabalhos e eventos vinculados serão removidos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(discColor, discColor.copy(alpha = 0.85f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = headerContentColor)
                        }

                        Row {
                            IconButton(onClick = {
                                tutorialStep = 0
                                showDisciplineTutorial = true
                            }) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Abrir tutorial da disciplina", tint = headerContentColor)
                            }
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = headerContentColor)
                            }
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = headerContentColor)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = discipline.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerContentColor
                    )

                    if (discipline.code.isNotBlank()) {
                        Text(
                            text = "${discipline.code} • Semestre ${discipline.term}",
                            fontSize = 15.sp,
                            color = headerContentColor.copy(alpha = 0.78f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (discipline.professor.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = headerContentColor, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Professor: ${discipline.professor}", color = headerContentColor, fontSize = 14.sp)
                        }
                    }

                    if (discipline.classroom.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = headerContentColor, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Local: ${discipline.classroom}", color = headerContentColor, fontSize = 14.sp)
                        }
                    }

                    if (discipline.schedule.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = headerContentColor, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Horário: ${abbreviateScheduleDays(discipline.schedule)}", color = headerContentColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Action Bar for Discipline
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cronograma da Disciplina",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onAddActivity,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar Evento", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> { // Assignments
                if (assignments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum trabalho ou projeto registrado.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(assignments) { act ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            DashboardActivityCard(
                                activity = act,
                                discipline = discipline,
                                onStatusChange = { onStatusChange(act.id, it) },
                                onGradeChange = { grade -> onGradeChange(act, grade) },
                                onDateChange = { date -> onDateChange(act, date) },
                                onClick = { onActivityClick(act) }
                            )
                        }
                    }
                }
            }
            1 -> { // Exams
                val gradedExams = exams.mapNotNull { it.grade }
                val average = gradedExams.takeIf { it.isNotEmpty() }?.average()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Média das provas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("${gradedExams.size} de ${exams.size} notas lançadas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = average?.let { String.format(java.util.Locale.getDefault(), "%.2f", it) } ?: "—",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (exams.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhuma prova registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(exams) { exam ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            DashboardActivityCard(
                                activity = exam,
                                discipline = discipline,
                                onStatusChange = { onStatusChange(exam.id, it) },
                                onGradeChange = { grade -> onGradeChange(exam, grade) },
                                onDateChange = { date -> onDateChange(exam, date) },
                                onClick = { onActivityClick(exam) }
                            )
                        }
                    }
                }
            }
            2 -> { // Classes / Aulas with Attendance Tracking & Absence Counter
                val absencesCount = classes.count { it.status == ActivityStatus.FALTA }
                val cancelledCount = classes.count { it.status == ActivityStatus.CANCELADA }
                val pendingCount = classes.count { it.status == ActivityStatus.PENDENTE || it.status == ActivityStatus.EM_ANDAMENTO }
                val validClasses = classes.size - cancelledCount
                val attendanceRate = if (validClasses > 0) {
                    (((validClasses - absencesCount).toFloat() / validClasses) * 100).toInt().coerceIn(0, 100)
                } else {
                    100
                }

                // Attendance Metrics Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.FactCheck,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Controle de Frequência & Aulas",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${classes.size} aulas no total • $validClasses válidas",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Attendance Rate Pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (attendanceRate >= 75) Color(0xFF006E1C).copy(alpha = 0.12f) else Color(0xFFBA1A1A).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "$attendanceRate% freq.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (attendanceRate >= 75) Color(0xFF006E1C) else Color(0xFFBA1A1A),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Metrics 3-box Grid: Faltas, Canceladas, Previstas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Absence Counter Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (absencesCount > 0) Color(0xFFBA1A1A).copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (absencesCount > 0) Color(0xFFBA1A1A).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = Color(0xFFBA1A1A),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Faltas",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFBA1A1A)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$absencesCount",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (absencesCount > 0) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Cancelled Classes Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (cancelledCount > 0) Color(0xFF5F6368).copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (cancelledCount > 0) Color(0xFF5F6368).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.EventBusy,
                                                    contentDescription = null,
                                                    tint = Color(0xFF5F6368),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Canceladas",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF5F6368)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$cancelledCount",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cancelledCount > 0) Color(0xFF5F6368) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Pending Classes Box
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Futuras",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$pendingCount",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (classes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma aula cadastrada nesta disciplina.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(classes.sortedBy { "${it.date} ${it.time}" }) { cl ->
                        ClassAttendanceCard(
                            activity = cl,
                            discipline = discipline,
                            onStatusChange = { newStatus -> onStatusChange(cl.id, newStatus) },
                            onClick = { onActivityClick(cl) }
                        )
                    }
                }
            }
            3 -> { // Documents
                if (documents.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum plano de ensino ou documento PDF importado.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(documents) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(doc.parsedSummary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DISCIPLINE_TUTORIAL_LAST_STEP = 3

@Composable
private fun DisciplineTutorialDialog(
    step: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    val (title, description, icon) = when (step) {
        0 -> Triple(
            "Cronograma da disciplina",
            "Esta tela reúne tudo o que pertence à matéria. Use Adicionar Evento para criar uma atividade, prova ou outro compromisso já vinculado a esta disciplina.",
            Icons.Default.AddTask
        )
        1 -> Triple(
            "Atividades e trabalhos",
            "Na aba Atividades você acompanha trabalhos, projetos e prazos. Use os botões dos cards para atualizar rapidamente cada compromisso.",
            Icons.Default.Assignment
        )
        2 -> Triple(
            "Provas e notas",
            "Na aba Provas, toque em Lançar nota para registrar ou remover uma nota. A média é recalculada automaticamente conforme as notas informadas.",
            Icons.Default.Calculate
        )
        else -> Triple(
            "Aulas, frequência e faltas",
            "Na aba Aulas você consulta a frequência. Marque falta ou aula cancelada diretamente no card; os totais e o percentual são atualizados automaticamente.",
            Icons.Default.FactCheck
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
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
                    text = "Passo ${step + 1} de ${DISCIPLINE_TUTORIAL_LAST_STEP + 1}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(title, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { (step + 1f) / (DISCIPLINE_TUTORIAL_LAST_STEP + 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = if (step == DISCIPLINE_TUTORIAL_LAST_STEP) onFinish else onNext) {
                Text(if (step == DISCIPLINE_TUTORIAL_LAST_STEP) "Concluir" else "Próximo")
                if (step < DISCIPLINE_TUTORIAL_LAST_STEP) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = onPrevious) { Text("Voltar") }
                TextButton(onClick = onDismiss) {
                    Text(if (step == DISCIPLINE_TUTORIAL_LAST_STEP) "Fechar" else "Pular")
                }
            }
        }
    )
}

@Composable
fun ClassAttendanceCard(
    activity: AcademicActivity,
    discipline: Discipline?,
    onStatusChange: (ActivityStatus) -> Unit,
    onClick: () -> Unit
) {
    val discColor = parseColorHex(discipline?.colorHex ?: "#0061A4")
    val isFalta = activity.status == ActivityStatus.FALTA
    val isCancelled = activity.status == ActivityStatus.CANCELADA
    val brDate = isoToBrazilianDate(activity.date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (isCancelled) 0.6f else 1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.06f)
                isCancelled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            if (isFalta) 1.5.dp else 1.dp,
            when {
                isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.6f)
                isCancelled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isFalta -> Color(0xFFBA1A1A)
                                    isCancelled -> Color(0xFF5F6368)
                                    else -> discColor
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activity.time.isBlank()) "$brDate • Sem horário" else "$brDate • ${activity.time}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Exibe marcador somente quando existe uma ocorrência relevante.
                if (isFalta || isCancelled) Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isFalta -> Color(0xFFBA1A1A).copy(alpha = 0.15f)
                        isCancelled -> Color(0xFF5F6368).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFalta) Icons.Default.Cancel else Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = when {
                                isFalta -> Color(0xFFBA1A1A)
                                isCancelled -> Color(0xFF5F6368)
                                else -> Color(0xFF5F6368)
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isFalta -> "FALTA"
                                isCancelled -> "CANCELADA"
                                else -> "CANCELADA"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFalta -> Color(0xFFBA1A1A)
                                isCancelled -> Color(0xFF5F6368)
                                else -> Color(0xFF5F6368)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activity.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
            )

            if (activity.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activity.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Attendance Action Row: Quick Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancelled Class Button
                OutlinedButton(
                    onClick = {
                        if (isCancelled) onStatusChange(ActivityStatus.PENDENTE)
                        else onStatusChange(ActivityStatus.CANCELADA)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCancelled) Color(0xFF5F6368) else Color.Transparent,
                        contentColor = if (isCancelled) Color.White else Color(0xFF5F6368)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF5F6368)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(
                        Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCancelled) "Cancelada" else "Aula Cancelada",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Falta Button
                OutlinedButton(
                    onClick = {
                        if (isFalta) onStatusChange(ActivityStatus.PENDENTE)
                        else onStatusChange(ActivityStatus.FALTA)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isFalta) Color(0xFFBA1A1A) else Color.Transparent,
                        contentColor = if (isFalta) Color.White else Color(0xFFBA1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFalta) "Falta Marcada" else "Marcar Falta",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
