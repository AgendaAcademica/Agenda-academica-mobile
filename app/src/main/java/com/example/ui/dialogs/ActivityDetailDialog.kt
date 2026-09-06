package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.components.*

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ActivityDetailDialog(
    activity: AcademicActivity,
    discipline: Discipline?,
    onDismiss: () -> Unit,
    onStatusChange: (ActivityStatus) -> Unit,
    onUpdate: (AcademicActivity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showGradeDialog by remember { mutableStateOf(false) }
    var gradeText by remember(activity.id, activity.grade) { mutableStateOf(activity.grade?.toString() ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DisciplineTag(discipline = discipline)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = activity.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActivityTypeBadge(type = activity.activityType)
                    PriorityBadge(priority = activity.priority)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date and Time Info Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Data: ${isoToBrazilianDate(activity.date)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Horário: ${activity.time.ifBlank { "Não informado" }}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!activity.originalDate.isNullOrBlank() && activity.originalDate != activity.date) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Data alterada de ${isoToBrazilianDate(activity.originalDate)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!activity.dueDate.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Prazo Final: ${isoToBrazilianDate(activity.dueDate)}", fontSize = 14.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (discipline != null && discipline.classroom.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Local: ${discipline.classroom}", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activity.activityType == ActivityType.PROVA) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Dados da prova", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alterar data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showGradeDialog = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(activity.grade?.let { "Nota: ${formatGrade(it)}" } ?: "Lançar nota", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Status Switcher
                Text(
                    if (activity.activityType == ActivityType.AULA) "Registro da aula:" else "Status da Atividade:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusSelector(
                    status = activity.status,
                    onStatusChange = onStatusChange,
                    availableStatuses = if (activity.activityType == ActivityType.AULA) {
                        listOf(ActivityStatus.FALTA, ActivityStatus.CANCELADA)
                    } else {
                        ActivityStatus.values().toList()
                    }
                )

                if (activity.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Descrição / Conteúdo:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                if (activity.sourceDocument.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Documento de Origem: ${activity.sourceDocument}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminders info
                Text("Lembretes Automáticos Ativos:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("7 dias antes", "3 dias antes", "1 dia antes", "No dia").forEach { label ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(activity.date) {
            runCatching {
                val parts = activity.date.split("-")
                java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
                }.timeInMillis
            }.getOrNull()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.format(java.util.Date(millis))
                        if (newDate != activity.date) {
                            onUpdate(activity.copy(originalDate = activity.originalDate ?: activity.date, date = newDate))
                        }
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showGradeDialog) {
        val parsedGrade = gradeText.replace(',', '.').toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { showGradeDialog = false },
            icon = { Icon(Icons.Default.Grade, contentDescription = null) },
            title = { Text("Nota da prova") },
            text = {
                OutlinedTextField(
                    value = gradeText,
                    onValueChange = { value ->
                        if (value.length <= 5 && value.all { it.isDigit() || it == ',' || it == '.' }) gradeText = value
                    },
                    label = { Text("Nota de 0 a 10") },
                    singleLine = true,
                    supportingText = {
                        if (gradeText.isNotBlank() && (parsedGrade == null || parsedGrade !in 0.0..10.0)) {
                            Text("Informe uma nota entre 0 e 10", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsedGrade != null && parsedGrade in 0.0..10.0,
                    onClick = {
                        onUpdate(activity.copy(grade = parsedGrade))
                        showGradeDialog = false
                    }
                ) { Text("Salvar nota") }
            },
            dismissButton = {
                Row {
                    if (activity.grade != null) {
                        TextButton(onClick = {
                            gradeText = ""
                            onUpdate(activity.copy(grade = null))
                            showGradeDialog = false
                        }) { Text("Remover") }
                    }
                    TextButton(onClick = { showGradeDialog = false }) { Text("Cancelar") }
                }
            }
        )
    }
}

private fun formatGrade(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", value)
