package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.components.keyboardDismissAction

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
    val context = LocalContext.current
    val dismissKeyboard = keyboardDismissAction()
    val preferences = remember { context.getSharedPreferences("academic_profile", Context.MODE_PRIVATE) }
    var savedName by rememberSaveable { mutableStateOf(preferences.getString("user_name", "Estudante").orEmpty()) }
    var name by rememberSaveable { mutableStateOf(savedName) }
    var profileSaved by remember { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val clearing by viewModel.isClearing.collectAsState()
    val savingImport by viewModel.isSavingImport.collectAsState()
    val analyzing by viewModel.isAnalyzingDocument.collectAsState()
    val message by viewModel.settingsMessage.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Configurações", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Personalize sua agenda acadêmica", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard {
                    SettingsSectionHeader(Icons.Default.Person, "Perfil", "Como você aparece na agenda")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Email, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("E-mail da conta", fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(accountEmail?.takeIf { it.isNotBlank() } ?: "E-mail não disponível",
                                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    OutlinedTextField(name, { name = it.take(30); profileSaved = false },
                        label = { Text("Como deseja ser chamado?") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth())
                    if (name.trim() != savedName.trim()) {
                        Button(onClick = {
                            name = name.trim()
                            preferences.edit().putString("user_name", name).apply()
                            savedName = name
                            profileSaved = true
                            dismissKeyboard()
                        }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Salvar perfil", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (profileSaved) Text("Perfil salvo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    TextButton(onClick = onSignOut, enabled = !clearing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sair da conta")
                    }
            }
        }
        item {
            SettingsCard {
                    SettingsSectionHeader(Icons.Default.Palette, "Aparência e widgets", "Deixe a agenda do seu jeito")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Tema escuro", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(if (isDarkTheme) "Ativado" else "Desativado", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    SettingsNavigationButton("Configurar widgets", Icons.Default.Widgets, onNavigateToWidget)
            }
        }
        item {
            SettingsCard {
                    SettingsSectionHeader(Icons.Default.NotificationsActive, "Lembretes", "Acompanhe seus próximos prazos")
                    Text("Consulte os próximos lembretes e gerencie as notificações.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsNavigationButton("Abrir lembretes", Icons.Default.Notifications, onNavigateToReminders)
            }
        }
        item {
            SettingsCard {
                    SettingsSectionHeader(Icons.Default.DeleteOutline, "Dados acadêmicos", "Gerencie os dados da sua agenda", destructive = true)
                    Text("Apague todas as disciplinas, aulas, atividades, documentos importados e lembretes. Seu perfil e sua conta serão mantidos.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { confirmClear = true }, enabled = !clearing && !analyzing && !savingImport,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (clearing) "Limpando…" else "Limpar tudo")
                    }
                    if (analyzing || savingImport) Text("Aguarde a análise do documento para limpar os dados.")
                    message?.let { Text(it) }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Limpar todos os dados acadêmicos?") },
        text = { Text("Todas as disciplinas e atividades serão apagadas, incluindo notas, documentos e lembretes. Esta ação não pode ser desfeita.") },
        confirmButton = {
            TextButton(onClick = { confirmClear = false; viewModel.clearAcademicData() }, enabled = !clearing && !analyzing && !savingImport) {
                Text("Apagar tudo", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String, subtitle: String, destructive: Boolean = false) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.12f)) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsNavigationButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
