package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.ui.components.ActivityTypeBadge
import com.example.ui.components.DisciplineTag
import com.example.ui.components.PriorityBadge
import com.example.ui.viewmodel.AcademicViewModel

@Composable
fun RemindersCenterScreen(
    viewModel: AcademicViewModel
) {
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val disciplinesMap = disciplines.associateBy { it.id }

    var testSentMessage by remember { mutableStateOf<String?>(null) }

    val pendingReminders = activities
        .filter { it.status != ActivityStatus.CONCLUIDA && it.remindersEnabled }
        .sortedBy { "${it.date} ${it.time}" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
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
                                colors = listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF06B6D4))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Central de Notificações", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lembretes automáticos configurados para avisar você com antecedência para você nunca perder uma prova ou prazo de entrega.",
                            color = Color(0xFFE0F2FE),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Notification Schedule Rules
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Regras de Antecedência Automática:", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    listOf(
                        "7 dias antes" to "Preparação inicial de estudos, leituras e divisão de projetos em grupo.",
                        "3 dias antes" to "Aviso de média antecedência para revisão e elaboração de exercícios.",
                        "1 dia antes" to "Alerta crítico na véspera de provas, testes e prazos de envio.",
                        "No dia da entrega" to "Notificação no horário de início ou prazo final para confirmação."
                    ).forEach { (cadence, desc) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.AlarmOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(cadence, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (testSentMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(testSentMessage!!, color = Color(0xFF14532D), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Active Scheduled Reminders
        item {
            Text("Próximos Lembretes Agendados (${pendingReminders.size}):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(pendingReminders) { act ->
            val disc = disciplinesMap[act.disciplineId]
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DisciplineTag(discipline = disc)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ActivityTypeBadge(type = act.activityType)
                            PriorityBadge(priority = act.priority)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(act.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Data: ${act.date} às ${act.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (disc != null) {
                                    viewModel.triggerTestReminder(act, disc, "Lembrete: Faltam 3 dias")
                                    testSentMessage = "Notificação disparada com sucesso para ${act.title}!"
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enviar Teste Agora", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
