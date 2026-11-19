package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityType
import com.example.ui.screens.settings.*
import com.example.ui.viewmodel.AcademicViewModel

@Composable
fun SettingsScreen(
    viewModel: AcademicViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToWidget: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onSignOut: () -> Unit,
    accountEmail: String? = null
) {
    val clearing by viewModel.isClearing.collectAsState()
    val savingImport by viewModel.isSavingImport.collectAsState()
    val analyzing by viewModel.isAnalyzingDocument.collectAsState()
    val message by viewModel.settingsMessage.collectAsState()
    val disciplines by viewModel.disciplines.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val classes = activities.count { it.activityType == ActivityType.AULA }
    SettingsContent(
        isDarkTheme = isDarkTheme,
        accountEmail = accountEmail,
        summary = AcademicDataSummary(disciplines.size, classes, activities.size - classes, documents.size),
        clearing = clearing,
        importing = analyzing || savingImport,
        message = message,
        onToggleTheme = onToggleTheme,
        onNavigateToWidget = onNavigateToWidget,
        onNavigateToReminders = onNavigateToReminders,
        onClear = viewModel::clearAcademicData,
        onSignOut = onSignOut
    )
}

private enum class SettingsDialog { PROFILE, DATA, CLEAR }

@Composable
internal fun SettingsContent(
    isDarkTheme: Boolean,
    accountEmail: String?,
    summary: AcademicDataSummary,
    clearing: Boolean = false,
    importing: Boolean = false,
    message: String? = null,
    onToggleTheme: () -> Unit,
    onNavigateToWidget: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onClear: () -> Unit,
    onSignOut: () -> Unit
) {
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val colors = MaterialTheme.colorScheme
    val canClear = !clearing && !importing
    Box(Modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 24.dp)
        ) {
            SettingsHeader()
            SettingsCard {
                SettingsRow("Perfil", "Como você aparece na agenda", Icons.Outlined.PersonOutline,
                    onClick = { dialog = SettingsDialog.PROFILE }, avatar = true)
            }
            SettingsSectionLabel("PREFERÊNCIAS")
            SettingsCard {
                SettingsRow("Tema escuro", icon = Icons.Outlined.DarkMode, trailing = {
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() },
                        modifier = Modifier.semantics { contentDescription = "Tema escuro" },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            checkedBorderColor = colors.primary,
                            uncheckedThumbColor = colors.onSurfaceVariant,
                            uncheckedTrackColor = colors.surfaceVariant,
                            uncheckedBorderColor = colors.outline
                        ))
                })
                HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = colors.outline)
                SettingsRow("Widgets", "Configurar widgets", Icons.Outlined.GridView,
                    onClick = onNavigateToWidget)
                HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = colors.outline)
                SettingsRow("Lembretes", "Gerencie notificações e prazos", Icons.Outlined.NotificationsNone,
                    onClick = onNavigateToReminders)
            }
            SettingsSectionLabel("DADOS")
            SettingsCard {
                SettingsRow("Dados acadêmicos", "Disciplinas, aulas, atividades e documentos",
                    AcademicDataIcon, onClick = { dialog = SettingsDialog.DATA })
            }
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { dialog = SettingsDialog.CLEAR }, enabled = canClear,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (canClear) colors.error else colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.error.copy(alpha = 0.10f), contentColor = colors.error)
            ) {
                if (clearing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(if (clearing) "Limpando…" else "Limpar tudo", style = MaterialTheme.typography.titleMedium)
            }
            if (importing) {
                Text("Aguarde a análise ou o salvamento da importação para limpar os dados.",
                    Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
            message?.let {
                Text(it, Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSignOut, enabled = !clearing,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sair da conta", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    when (dialog) {
        SettingsDialog.PROFILE -> ProfileSettingsDialog(accountEmail, onDismiss = { dialog = null })
        SettingsDialog.DATA -> AcademicDataDialog(summary, onDismiss = { dialog = null })
        SettingsDialog.CLEAR -> ClearAcademicDataDialog(
            enabled = canClear,
            onDismiss = { dialog = null },
            onConfirm = { dialog = null; onClear() }
        )
        null -> Unit
    }
}
