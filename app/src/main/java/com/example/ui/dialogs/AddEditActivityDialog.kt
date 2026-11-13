package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority
import com.example.ui.components.parseColorHex
import java.util.Calendar
import java.util.TimeZone

/**
 * Converts ISO "YYYY-MM-DD" to Brazilian format "DD/MM/YYYY".
 */
fun isoToBrazilianDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    val parts = isoDate.split("-")
    return if (parts.size == 3) {
        "${parts[2].padStart(2, '0')}/${parts[1].padStart(2, '0')}/${parts[0]}"
    } else isoDate
}

/**
 * Converts Brazilian format "DD/MM/YYYY" or "DD/MM/YY" to ISO "YYYY-MM-DD".
 */
fun brazilianToIsoDate(brDate: String): String {
    if (brDate.isBlank()) return ""
    val parts = brDate.split("/")
    return if (parts.size == 3) {
        val rawYear = parts[2]
        val fullYear = if (rawYear.length == 2) "20$rawYear" else rawYear
        "$fullYear-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
    } else brDate
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditActivityDialog(
    initialActivity: AcademicActivity? = null,
    disciplines: List<Discipline>,
    preselectedDisciplineId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (AcademicActivity) -> Unit
) {
    val dismissKeyboard = com.example.ui.components.keyboardDismissAction()
    var title by remember { mutableStateOf(initialActivity?.title ?: "") }
    var description by remember { mutableStateOf(initialActivity?.description ?: "") }
    
    // Nullable discipline ID: null represents "Atividade Extra / Sem disciplina"
    var selectedDisciplineId by remember {
        mutableStateOf<Long?>(
            initialActivity?.disciplineId ?: preselectedDisciplineId ?: disciplines.firstOrNull()?.id
        )
    }

    var selectedType by remember { mutableStateOf(initialActivity?.activityType ?: ActivityType.TRABALHO) }
    var selectedPriority by remember { mutableStateOf(initialActivity?.priority ?: Priority.MEDIA) }
    var selectedStatus by remember { mutableStateOf(initialActivity?.status ?: ActivityStatus.PENDENTE) }
    
    // Internal ISO dates (YYYY-MM-DD)
    val todayIso = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
    var dateIso by remember { mutableStateOf(initialActivity?.date ?: todayIso) }
    var dueDateIso by remember { mutableStateOf(initialActivity?.dueDate ?: "") }
    var time by remember { mutableStateOf(initialActivity?.time ?: "08:00") }
    var notes by remember { mutableStateOf(initialActivity?.notes ?: "") }

    // Dialog & Picker States
    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var priorityDropdownExpanded by remember { mutableStateOf(false) }
    val isDisciplineLocked = initialActivity == null && preselectedDisciplineId != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialActivity == null) "Nova Atividade" else "Editar Atividade",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da Atividade *") },
                    placeholder = { Text("Ex: Prova 1, Estudar para Concurso, Reunião...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // The discipline is fixed when this dialog is opened from its detail screen.
                if (isDisciplineLocked) {
                    val linkedDiscipline = disciplines.firstOrNull { it.id == preselectedDisciplineId }
                    if (linkedDiscipline != null) {
                        val linkedColor = parseColorHex(linkedDiscipline.colorHex)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = linkedColor.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, linkedColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(linkedColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = linkedDiscipline.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Evento vinculado a esta disciplina",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Disciplina definida",
                                    tint = linkedColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Discipline Selection with Native Extra Option
                    Text(
                        text = "Vínculo da Atividade:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Option 1: Native "Atividade Extra / Sem Disciplina"
                    val isExtraSelected = selectedDisciplineId == null
                    val extraColor = Color(0xFF7C3AED)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExtraSelected) extraColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isExtraSelected) BorderStroke(1.5.dp, extraColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDisciplineId = null }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(extraColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = extraColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Atividade Extra (Sem Disciplina)",
                                    fontWeight = if (isExtraSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isExtraSelected) extraColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Compromisso pessoal, curso livre ou tarefa avulsa",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isExtraSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = extraColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Disciplines List
                        disciplines.forEach { disc ->
                        val isSelected = disc.id == selectedDisciplineId
                        val discColor = parseColorHex(disc.colorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) discColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) BorderStroke(1.5.dp, discColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDisciplineId = disc.id }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(discColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = disc.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isSelected) discColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (disc.professor.isNotBlank()) {
                                        Text(
                                            text = "${disc.code} • ${disc.professor}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = discColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Activity Type & Priority in row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            ActivityType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = priorityDropdownExpanded,
                        onExpandedChange = { priorityDropdownExpanded = !priorityDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedPriority.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Prioridade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityDropdownExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = priorityDropdownExpanded,
                            onDismissRequest = { priorityDropdownExpanded = false }
                        ) {
                            Priority.values().forEach { priority ->
                                DropdownMenuItem(
                                    text = { Text(priority.displayName) },
                                    onClick = {
                                        selectedPriority = priority
                                        priorityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date (DD/MM/AAAA) with Mini Calendar Click & Time (HH:MM) with Clock Dial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date Field (Clickable to open Mini Calendar)
                    OutlinedTextField(
                        value = isoToBrazilianDate(dateIso),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data (DD/MM/AAAA)") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Selecionar Data", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .weight(1.1f)
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Time Field (Clickable to open Interactive Clock Dial)
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horário") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Selecionar Horário", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Optional Due Date (DD/MM/AAAA) with Mini Calendar
                OutlinedTextField(
                    value = if (dueDateIso.isNotBlank()) isoToBrazilianDate(dueDateIso) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Prazo Final de Entrega (Opcional)") },
                    placeholder = { Text("Toque para selecionar data DD/MM/AAAA") },
                    leadingIcon = {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF8B5000), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (dueDateIso.isNotBlank()) {
                            IconButton(onClick = { dueDateIso = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar Prazo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            IconButton(onClick = { showDueDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar Prazo", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDueDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / Instruções (Opcional)") },
                    placeholder = { Text("Detalhes, referências, links...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Save Button
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            dismissKeyboard()
                            onSave(
                                AcademicActivity(
                                    id = initialActivity?.id ?: 0L,
                                    disciplineId = selectedDisciplineId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    activityType = selectedType,
                                    date = dateIso.ifBlank { todayIso },
                                    originalDate = initialActivity?.originalDate,
                                    time = time.ifBlank { "08:00" },
                                    dueDate = dueDateIso.ifBlank { null },
                                    priority = selectedPriority,
                                    status = selectedStatus,
                                    grade = initialActivity?.grade,
                                    sourceDocument = initialActivity?.sourceDocument ?: if (selectedDisciplineId == null) "Atividade Extra" else "Cadastro Manual",
                                    notes = notes
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvar Atividade", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Interactive DatePicker Dialog for Activity Date (Brazilian DD/MM/AAAA)
    if (showDatePicker) {
        val initialMillis = try {
            val parts = dateIso.split("-")
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
            }
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val y = cal.get(Calendar.YEAR)
                            val m = cal.get(Calendar.MONTH) + 1
                            val d = cal.get(Calendar.DAY_OF_MONTH)
                            dateIso = String.format("%04d-%02d-%02d", y, m, d)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        "Selecione a Data (DD/MM/AAAA)",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    }

    // Interactive DatePicker Dialog for Due Date (Prazo Final)
    if (showDueDatePicker) {
        val initialMillis = try {
            if (dueDateIso.isNotBlank()) {
                val parts = dueDateIso.split("-")
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
                }
                cal.timeInMillis
            } else System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val dueDatePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDatePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val y = cal.get(Calendar.YEAR)
                            val m = cal.get(Calendar.MONTH) + 1
                            val d = cal.get(Calendar.DAY_OF_MONTH)
                            dueDateIso = String.format("%04d-%02d-%02d", y, m, d)
                        }
                        showDueDatePicker = false
                    }
                ) {
                    Text("Confirmar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = dueDatePickerState,
                title = {
                    Text(
                        "Selecione o Prazo de Entrega (DD/MM/AAAA)",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            )
        }
    }

    // Interactive TimePicker Clock Dial Dialog
    if (showTimePicker) {
        val timeParts = time.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selecione o Horário",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                showTimePicker = false
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
