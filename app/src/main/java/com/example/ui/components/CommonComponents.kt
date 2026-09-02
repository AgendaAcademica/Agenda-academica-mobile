package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority

fun parseColorHex(hex: String, defaultColor: Color = Color(0xFF0061A4)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

fun getTimePeriod(time: String): String {
    val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: 8
    return when {
        hour < 12 -> "MANHÃ"
        hour < 18 -> "TARDE"
        else -> "NOITE"
    }
}

@Composable
fun DisciplineTag(
    discipline: Discipline?,
    modifier: Modifier = Modifier
) {
    val color = if (discipline != null) parseColorHex(discipline.colorHex) else Color(0xFF6750A4)
    val text = discipline?.name?.uppercase() ?: "EXTRA"
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ActivityTypeBadge(
    type: ActivityType,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (type) {
        ActivityType.AULA -> Triple(Icons.Default.School, Color(0xFF0061A4), "Aula")
        ActivityType.PROVA -> Triple(Icons.Default.Quiz, Color(0xFFBA1A1A), "Prova")
        ActivityType.TRABALHO -> Triple(Icons.Default.Assignment, Color(0xFF006E1C), "Trabalho")
        ActivityType.EXERCICIO -> Triple(Icons.Default.EditNote, Color(0xFF6750A4), "Exercício")
        ActivityType.SEMINARIO -> Triple(Icons.Default.Groups, Color(0xFF8B5000), "Seminário")
        ActivityType.PROJETO -> Triple(Icons.Default.Code, Color(0xFF006874), "Projeto")
        ActivityType.LEITURA -> Triple(Icons.Default.MenuBook, Color(0xFF535F70), "Leitura")
        ActivityType.PRAZO_ENTREGA -> Triple(Icons.Default.HourglassTop, Color(0xFFBA1A1A), "Prazo")
        ActivityType.EVENTO -> Triple(Icons.Default.Event, Color(0xFF0061A4), "Evento")
    }

    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.20f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (bg, textColor) = when (priority) {
        Priority.URGENTE -> Color(0xFFFFDAD6) to Color(0xFF410002)
        Priority.ALTA -> Color(0xFFFFECCF) to Color(0xFF563100)
        Priority.MEDIA -> Color(0xFFD3E4FF) to Color(0xFF001C38)
        Priority.BAIXA -> Color(0xFFD7E8CD) to Color(0xFF0B3808)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = priority.displayName.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun isoToBrazilianDate(isoDate: String): String {
    val parts = isoDate.split("-")
    return if (parts.size == 3) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        isoDate
    }
}

fun brazilianToIsoDate(brDate: String): String {
    val parts = brDate.split("/")
    return if (parts.size == 3) {
        "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
    } else {
        brDate
    }
}

fun getBrazilianMonthName(month: Int): String {
    return when (month) {
        1 -> "Janeiro"
        2 -> "Fevereiro"
        3 -> "Março"
        4 -> "Abril"
        5 -> "Maio"
        6 -> "Junho"
        7 -> "Julho"
        8 -> "Agosto"
        9 -> "Setembro"
        10 -> "Outubro"
        11 -> "Novembro"
        12 -> "Dezembro"
        else -> "Mês $month"
    }
}

fun getBrazilianDayOfWeekName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        java.util.Calendar.SUNDAY -> "Domingo"
        java.util.Calendar.MONDAY -> "Segunda-feira"
        java.util.Calendar.TUESDAY -> "Terça-feira"
        java.util.Calendar.WEDNESDAY -> "Quarta-feira"
        java.util.Calendar.THURSDAY -> "Quinta-feira"
        java.util.Calendar.FRIDAY -> "Sexta-feira"
        java.util.Calendar.SATURDAY -> "Sábado"
        else -> ""
    }
}

@Composable
fun StatusSelector(
    status: ActivityStatus,
    onStatusChange: (ActivityStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActivityStatus.values().forEach { s ->
            val isSelected = s == status
            val bg = if (isSelected) {
                when (s) {
                    ActivityStatus.PENDENTE -> Color(0xFF8B5000)
                    ActivityStatus.EM_ANDAMENTO -> Color(0xFF005AC1)
                    ActivityStatus.CONCLUIDA -> Color(0xFF006E1C)
                    ActivityStatus.FALTA -> Color(0xFFBA1A1A)
                    ActivityStatus.CANCELADA -> Color(0xFF5F6368)
                }
            } else Color.Transparent

            val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .clickable { onStatusChange(s) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = s.displayName,
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

