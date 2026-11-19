package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsHeader() {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().drawBehind {
            val radius = 156.dp.toPx()
            val center = Offset(size.width, 0f)
            drawCircle(Brush.radialGradient(
                listOf(colors.primaryContainer.copy(alpha = 0.45f), colors.background.copy(alpha = 0f)),
                center = center, radius = radius
            ), radius = radius, center = center)
        }.padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Configurações", fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold,
                color = colors.onSurface, modifier = Modifier.semantics { heading() })
            Text("Personalize sua agenda acadêmica", fontSize = 14.sp, lineHeight = 20.sp,
                color = colors.onSurfaceVariant)
        }
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.primary))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.onPrimary,
                modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsSectionLabel(label: String) {
    Text(label, modifier = Modifier.padding(start = 10.dp, top = 26.dp, bottom = 12.dp)
        .semantics { heading() }, fontSize = 12.sp, letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    avatar: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val interaction = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
    Row(
        Modifier.fillMaxWidth().then(interaction).heightIn(min = 64.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (avatar) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(colors.secondaryContainer, colors.primaryContainer))),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
        } else Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            subtitle?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant) }
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null,
            tint = colors.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}
