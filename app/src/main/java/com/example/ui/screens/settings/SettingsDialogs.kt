package com.example.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.keyboardDismissAction

internal data class AcademicDataSummary(
    val disciplines: Int,
    val classes: Int,
    val activities: Int,
    val documents: Int
)

@Composable
internal fun ProfileSettingsDialog(accountEmail: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("academic_profile", Context.MODE_PRIVATE) }
    var name by rememberSaveable { mutableStateOf(preferences.getString("user_name", "Estudante").orEmpty()) }
    val dismissKeyboard = keyboardDismissAction()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
        title = { Text("Perfil") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Como você aparece na agenda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("E-mail da conta", style = MaterialTheme.typography.labelMedium)
                    Text(accountEmail?.takeIf { it.isNotBlank() } ?: "E-mail não disponível",
                        style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(name, onValueChange = { name = it.take(30) },
                    label = { Text("Como deseja ser chamado?") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                preferences.edit().putString("user_name", name.trim()).apply()
                dismissKeyboard()
                onDismiss()
            }, enabled = name.isNotBlank()) { Text("Salvar perfil") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
internal fun AcademicDataDialog(summary: AcademicDataSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(AcademicDataIcon, contentDescription = null) },
        title = { Text("Dados acadêmicos") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Dados salvos na sua agenda", color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf("Disciplinas" to summary.disciplines, "Aulas" to summary.classes,
                    "Atividades" to summary.activities, "Documentos" to summary.documents).forEach { (label, count) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, modifier = Modifier.weight(1f))
                        Text(count.toString(), fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Gerencie disciplinas e atividades pelas abas da agenda. Para remover todos os dados, use Limpar tudo nas configurações.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
internal fun ClearAcademicDataDialog(enabled: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Limpar todos os dados acadêmicos?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Todas as disciplinas, aulas, atividades, notas e documentos importados serão apagados desta agenda. Os lembretes agendados serão cancelados.")
                Text("Sua conta, seu perfil e suas preferências serão mantidos.")
                Text("Esta ação é permanente e não pode ser desfeita.", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Apagar tudo")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
