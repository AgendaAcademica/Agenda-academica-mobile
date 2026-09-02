package com.example.ui.dialogs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority
import com.example.ui.components.brazilianToIsoDate
import com.example.ui.components.isoToBrazilianDate
import com.example.ui.components.parseColorHex
import java.text.SimpleDateFormat
import java.util.*

val PRESET_COLORS = listOf(
    "#0061A4" to "Azul Acadêmico",
    "#006E1C" to "Verde Floresta",
    "#8B5000" to "Âmbar / Laranja",
    "#6750A4" to "Roxo Acadêmico",
    "#BA1A1A" to "Vermelho Rubi",
    "#006874" to "Teal Oceano",
    "#7C3AED" to "Violeta",
    "#435E91" to "Azul Petróleo"
)

data class WeekDayOption(
    val calendarDay: Int, // Calendar.MONDAY, etc.
    val shortLabel: String, // SEG, TER, ...
    val fullLabel: String // Segunda, Terça, ...
)

val WEEKDAY_OPTIONS = listOf(
    WeekDayOption(Calendar.MONDAY, "SEG", "Segunda"),
    WeekDayOption(Calendar.TUESDAY, "TER", "Terça"),
    WeekDayOption(Calendar.WEDNESDAY, "QUA", "Quarta"),
    WeekDayOption(Calendar.THURSDAY, "QUI", "Quinta"),
    WeekDayOption(Calendar.FRIDAY, "SEX", "Sexta"),
    WeekDayOption(Calendar.SATURDAY, "SÁB", "Sábado"),
    WeekDayOption(Calendar.SUNDAY, "DOM", "Domingo")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDisciplineDialog(
    initialDiscipline: Discipline? = null,
    onDismiss: () -> Unit,
    onSave: (Discipline, List<AcademicActivity>) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialDiscipline?.name ?: "") }
    var code by remember { mutableStateOf(initialDiscipline?.code ?: "") }
    var professor by remember { mutableStateOf(initialDiscipline?.professor ?: "") }
    var classroom by remember { mutableStateOf(initialDiscipline?.classroom ?: "") }
    var term by remember { mutableStateOf(initialDiscipline?.term ?: "2026/2") }
    var selectedColor by remember { mutableStateOf(initialDiscipline?.colorHex ?: "#0061A4") }
    var notes by remember { mutableStateOf(initialDiscipline?.syllabusNotes ?: "") }

    // Class Schedule configurations
    var selectedDays by remember {
        mutableStateOf(
            if (initialDiscipline != null) {
                // Try to infer days from schedule string if editing
                val s = initialDiscipline.schedule.lowercase()
                val set = mutableSetOf<Int>()
                if (s.contains("seg")) set.add(Calendar.MONDAY)
                if (s.contains("ter")) set.add(Calendar.TUESDAY)
                if (s.contains("qua")) set.add(Calendar.WEDNESDAY)
                if (s.contains("qui")) set.add(Calendar.THURSDAY)
                if (s.contains("sex")) set.add(Calendar.FRIDAY)
                if (s.contains("sáb") || s.contains("sab")) set.add(Calendar.SATURDAY)
                if (s.contains("dom")) set.add(Calendar.SUNDAY)
                if (set.isEmpty()) setOf(Calendar.MONDAY, Calendar.WEDNESDAY) else set
            } else {
                setOf(Calendar.MONDAY, Calendar.WEDNESDAY)
            }
        )
    }

    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var startDateBr by remember { mutableStateOf("01/09/2026") }
    var repeatUntilDateBr by remember { mutableStateOf("15/12/2026") }
    var autoGenerateCalendarClasses by remember { mutableStateOf(true) }

    // Calculate preview of generated classes
    val generatedClasses = remember(name, selectedDays, startTime, startDateBr, repeatUntilDateBr, autoGenerateCalendarClasses) {
        if (!autoGenerateCalendarClasses || selectedDays.isEmpty() || name.isBlank()) {
            emptyList()
        } else {
            generateClassOccurrences(
                disciplineName = name,
                selectedDays = selectedDays,
                startTime = startTime,
                startDateIso = brazilianToIsoDate(startDateBr),
                repeatUntilDateIso = brazilianToIsoDate(repeatUntilDateBr)
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                                .background(parseColorHex(selectedColor).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = parseColorHex(selectedColor), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialDiscipline == null) "Nova Disciplina" else "Editar Disciplina",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Semestre $term",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Disciplina *") },
                        placeholder = { Text("Ex: Inteligência Artificial") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                    )

                    // Code & Term
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Código") },
                            placeholder = { Text("IA401") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) }
                        )
                        OutlinedTextField(
                            value = term,
                            onValueChange = { term = it },
                            label = { Text("Período") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Professor & Classroom
                    OutlinedTextField(
                        value = professor,
                        onValueChange = { professor = it },
                        label = { Text("Professor(a)") },
                        placeholder = { Text("Ex: Prof. Dr. Silva") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Sala / Laboratório") },
                        placeholder = { Text("Ex: Sala 104 - Bloco C") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
                    )

                    // SECTION: Horário das Aulas & Agendamento
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dias e Horários das Aulas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Selecione os dias da semana:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Day selector chips SEG to DOM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                WEEKDAY_OPTIONS.forEach { opt ->
                                    val isSelected = selectedDays.contains(opt.calendarDay)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    selectedDays - opt.calendarDay
                                                } else {
                                                    selectedDays + opt.calendarDay
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt.shortLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Time Picker Row (Início e Fim)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start Time
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("Horário Início") },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val parts = startTime.split(":")
                                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                                startTime = String.format("%02d:%02d", hourOfDay, minute)
                                            }, h, m, true).show()
                                        },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val parts = startTime.split(":")
                                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                                startTime = String.format("%02d:%02d", hourOfDay, minute)
                                            }, h, m, true).show()
                                        }) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Selecionar horário")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // End Time
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text("Horário Término") },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val parts = endTime.split(":")
                                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 10
                                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                                endTime = String.format("%02d:%02d", hourOfDay, minute)
                                            }, h, m, true).show()
                                        },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val parts = endTime.split(":")
                                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 10
                                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                            TimePickerDialog(context, { _, hourOfDay, minute ->
                                                endTime = String.format("%02d:%02d", hourOfDay, minute)
                                            }, h, m, true).show()
                                        }) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Selecionar horário")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Date range (Início e Repetir até)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start Date
                                OutlinedTextField(
                                    value = startDateBr,
                                    onValueChange = { startDateBr = it },
                                    label = { Text("Início das Aulas") },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance()
                                            val parts = startDateBr.split("/")
                                            val day = parts.getOrNull(0)?.toIntOrNull() ?: 1
                                            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 9) - 1
                                            val year = parts.getOrNull(2)?.toIntOrNull() ?: 2026
                                            DatePickerDialog(context, { _, y, m, d ->
                                                startDateBr = String.format("%02d/%02d/%04d", d, m + 1, y)
                                            }, year, month, day).show()
                                        },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val parts = startDateBr.split("/")
                                            val day = parts.getOrNull(0)?.toIntOrNull() ?: 1
                                            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 9) - 1
                                            val year = parts.getOrNull(2)?.toIntOrNull() ?: 2026
                                            DatePickerDialog(context, { _, y, m, d ->
                                                startDateBr = String.format("%02d/%02d/%04d", d, m + 1, y)
                                            }, year, month, day).show()
                                        }) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar início")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Repeat until Date
                                OutlinedTextField(
                                    value = repeatUntilDateBr,
                                    onValueChange = { repeatUntilDateBr = it },
                                    label = { Text("Repetir até *") },
                                    placeholder = { Text("DD/MM/AAAA") },
                                    readOnly = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val parts = repeatUntilDateBr.split("/")
                                            val day = parts.getOrNull(0)?.toIntOrNull() ?: 15
                                            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 12) - 1
                                            val year = parts.getOrNull(2)?.toIntOrNull() ?: 2026
                                            DatePickerDialog(context, { _, y, m, d ->
                                                repeatUntilDateBr = String.format("%02d/%02d/%04d", d, m + 1, y)
                                            }, year, month, day).show()
                                        },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val parts = repeatUntilDateBr.split("/")
                                            val day = parts.getOrNull(0)?.toIntOrNull() ?: 15
                                            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 12) - 1
                                            val year = parts.getOrNull(2)?.toIntOrNull() ?: 2026
                                            DatePickerDialog(context, { _, y, m, d ->
                                                repeatUntilDateBr = String.format("%02d/%02d/%04d", d, m + 1, y)
                                            }, year, month, day).show()
                                        }) {
                                            Icon(Icons.Default.Event, contentDescription = "Selecionar fim da disciplina")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Auto-generate Classes Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Adicionar aulas ao calendário",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (generatedClasses.isNotEmpty())
                                            "📅 ${generatedClasses.size} aulas serão criadas e listadas na aba de Aulas"
                                        else
                                            "Cria automaticamente os eventos das aulas até $repeatUntilDateBr",
                                        fontSize = 10.sp,
                                        color = if (generatedClasses.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoGenerateCalendarClasses,
                                    onCheckedChange = { autoGenerateCalendarClasses = it }
                                )
                            }
                        }
                    }

                    // Color Selection
                    Text("Cor de Identificação da Matéria:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PRESET_COLORS.take(4).forEach { (hex, label) ->
                            val color = parseColorHex(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PRESET_COLORS.drop(4).forEach { (hex, label) ->
                            val color = parseColorHex(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            // Build schedule text e.g. "Segunda e Quarta, 08:00 - 10:00"
                            val dayNames = WEEKDAY_OPTIONS.filter { selectedDays.contains(it.calendarDay) }.map { it.fullLabel }
                            val scheduleStr = if (dayNames.isNotEmpty()) {
                                "${dayNames.joinToString(", ")}, $startTime - $endTime"
                            } else {
                                "$startTime - $endTime"
                            }

                            val disciplineToSave = Discipline(
                                id = initialDiscipline?.id ?: 0L,
                                name = name.trim(),
                                code = code.trim(),
                                professor = professor.trim(),
                                classroom = classroom.trim(),
                                schedule = scheduleStr,
                                term = term.trim(),
                                colorHex = selectedColor,
                                syllabusNotes = notes
                            )

                            onSave(disciplineToSave, generatedClasses)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (generatedClasses.isNotEmpty())
                            "Salvar Disciplina (+ ${generatedClasses.size} Aulas)"
                        else
                            "Salvar Disciplina",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Generates regular class occurrences for a discipline matching the selected days of the week.
 */
fun generateClassOccurrences(
    disciplineName: String,
    selectedDays: Set<Int>,
    startTime: String,
    startDateIso: String,
    repeatUntilDateIso: String
): List<AcademicActivity> {
    val result = mutableListOf<AcademicActivity>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    try {
        val startCalendar = Calendar.getInstance().apply {
            time = sdf.parse(startDateIso) ?: Date()
        }
        val endCalendar = Calendar.getInstance().apply {
            time = sdf.parse(repeatUntilDateIso) ?: Date()
        }

        var classCount = 1
        val currentCalendar = startCalendar.clone() as Calendar

        while (!currentCalendar.after(endCalendar)) {
            val dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK)
            if (selectedDays.contains(dayOfWeek)) {
                val dateStr = sdf.format(currentCalendar.time)
                result.add(
                    AcademicActivity(
                        disciplineId = null, // will be attached when saved
                        title = "Aula #$classCount — $disciplineName",
                        description = "Aula regular da disciplina $disciplineName.",
                        activityType = ActivityType.AULA,
                        date = dateStr,
                        time = startTime,
                        priority = Priority.MEDIA,
                        status = ActivityStatus.PENDENTE,
                        sourceDocument = "Horário das Aulas"
                    )
                )
                classCount++
            }
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    } catch (e: Exception) {
        // Return empty or partial list on parse issue
    }

    return result
}

