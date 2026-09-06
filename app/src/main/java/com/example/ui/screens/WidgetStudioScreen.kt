package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.ui.components.parseColorHex
import com.example.ui.viewmodel.AcademicViewModel
import com.example.widget.AcademicDayAppWidgetProvider
import com.example.widget.WidgetSettings

@Composable
fun WidgetStudioScreen(
    viewModel: AcademicViewModel,
    isDarkTheme: Boolean,
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val widgetSettings by viewModel.widgetSettings.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    val disciplinesMap = disciplines.associateBy { it.id }

    var maxItems by remember(widgetSettings) { mutableStateOf(widgetSettings.maxItems) }
    var showOnlyPending by remember(widgetSettings) { mutableStateOf(widgetSettings.showOnlyPending) }
    var showClasses by remember(widgetSettings) { mutableStateOf(widgetSettings.showClasses) }
    var showTimes by remember(widgetSettings) { mutableStateOf(widgetSettings.showTimes) }
    var showDeadlines by remember(widgetSettings) { mutableStateOf(widgetSettings.showDeadlines) }
    var selectedDisciplineIds by remember(widgetSettings) { mutableStateOf(widgetSettings.selectedDisciplineIds) }

    var appliedNotification by remember { mutableStateOf(false) }
    var showPinInstructionsDialog by remember { mutableStateOf(false) }

    val widgetBackground = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val widgetBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val widgetItemBackground = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val widgetPrimaryText = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val widgetSecondaryText = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)

    // Filter preview activities
    var previewActivities = activities.filter { it.date == currentDate || it.dueDate == currentDate }
    if (showOnlyPending) {
        previewActivities = previewActivities.filter { it.status != ActivityStatus.CONCLUIDA }
    }
    if (!showClasses) {
        previewActivities = previewActivities.filter { it.activityType != ActivityType.AULA }
    }
    if (selectedDisciplineIds.isNotEmpty()) {
        previewActivities = previewActivities.filter { selectedDisciplineIds.contains(it.disciplineId) }
    }

    if (showPinInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showPinInstructionsDialog = false },
            icon = {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Como adicionar à Tela Inicial", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Vá para a tela inicial do seu celular.", fontSize = 13.sp)
                    Text("2. Pressione e segure o dedo em um espaço vazio.", fontSize = 13.sp)
                    Text("3. Toque em 'Widgets' no menu que aparecer.", fontSize = 13.sp)
                    Text("4. Procure por 'Agenda Acadêmica' e arraste o widget para onde desejar!", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPinInstructionsDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header with Back to Home Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onNavigateToHome,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Voltar ao Início",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Widget da Tela Inicial", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalButton(
                                onClick = onNavigateToHome,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Início", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Acompanhe seus compromissos, provas e aulas diretamente na tela inicial do seu celular, com atualização automática e visualização colorida por disciplina.",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Quick Action: Add Widget to Android Home Screen directly
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fixar Widget no Celular",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Adicione o atalho interativo à sua tela inicial com 1 toque.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val myProvider = ComponentName(context, AcademicDayAppWidgetProvider::class.java)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                Toast.makeText(context, "Solicitação de fixação enviada para a tela inicial!", Toast.LENGTH_SHORT).show()
                            } else {
                                showPinInstructionsDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adicionar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Live Simulated Widget Preview on Phone Screen
        item {
            Text("Pré-visualização do Widget (Tela Inicial):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            // Widget Simulation Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = widgetBackground),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, widgetBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header of Widget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Hoje — 01 de setembro", color = widgetPrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Agenda Acadêmica", color = widgetSecondaryText, fontSize = 10.sp)
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Items in Widget
                    if (previewActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sem atividades programadas para hoje.", color = widgetSecondaryText, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            previewActivities.take(maxItems).forEach { act ->
                                val disc = disciplinesMap[act.disciplineId]
                                val discColor = parseColorHex(disc?.colorHex ?: "#2563EB")

                                Surface(
                                    color = widgetItemBackground,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(discColor)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            val timePrefix = if (showTimes && act.time.isNotBlank()) "${act.time} — " else ""
                                            Text(
                                                text = "$timePrefix${disc?.name ?: "Geral"}",
                                                color = discColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${act.activityType.displayName}: ${act.title}",
                                                color = widgetPrimaryText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                            if (!disc?.classroom.isNullOrBlank()) {
                                                Text(
                                                    text = "📍 ${disc?.classroom}",
                                                    color = widgetSecondaryText,
                                                    fontSize = 10.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (showDeadlines && act.dueDate != null) {
                                            Surface(
                                                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Prazo Hoje",
                                                    color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        }

        // Customization Options
        item {
            Text("Opções de Personalização:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Max items
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Quantidade de Atividades", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Máximo de itens no widget", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 5).forEach { count ->
                                val isSelected = maxItems == count
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { maxItems = count }
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Show only pending
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar somente atividades pendentes", fontSize = 14.sp)
                        Switch(checked = showOnlyPending, onCheckedChange = { showOnlyPending = it })
                    }

                    // Show classes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar aulas do dia", fontSize = 14.sp)
                        Switch(checked = showClasses, onCheckedChange = { showClasses = it })
                    }

                    // Show times
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar horários", fontSize = 14.sp)
                        Switch(checked = showTimes, onCheckedChange = { showTimes = it })
                    }

                    // Show deadlines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mostrar indicador de próximos prazos", fontSize = 14.sp)
                        Switch(checked = showDeadlines, onCheckedChange = { showDeadlines = it })
                    }
                }
            }
        }

        // Filter by disciplines in widget
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Exibir somente determinadas disciplinas:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    disciplines.forEach { disc ->
                        val isChecked = selectedDisciplineIds.isEmpty() || selectedDisciplineIds.contains(disc.id)
                        val color = parseColorHex(disc.colorHex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDisciplineIds = if (selectedDisciplineIds.contains(disc.id)) {
                                        selectedDisciplineIds - disc.id
                                    } else {
                                        selectedDisciplineIds + disc.id
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedDisciplineIds = if (checked) selectedDisciplineIds + disc.id else selectedDisciplineIds - disc.id
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(disc.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Apply settings button & Return to Home
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.updateWidgetSettings(
                            WidgetSettings(
                                maxItems = maxItems,
                                showOnlyPending = showOnlyPending,
                                showClasses = showClasses,
                                showTimes = showTimes,
                                showDeadlines = showDeadlines,
                                selectedDisciplineIds = selectedDisciplineIds
                            )
                        )
                        appliedNotification = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar e Atualizar Widget do Android", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir para a Tela Inicial", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        if (appliedNotification) {
            item {
                Surface(
                    color = Color(0xFF006E1C).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF006E1C).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006E1C), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Configurações salvas e widget atualizado na tela inicial com sucesso!",
                            color = Color(0xFF006E1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
