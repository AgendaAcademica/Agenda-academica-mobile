package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ExtractedEvent
import com.example.ai.ExtractedSyllabus
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityType
import com.example.data.model.Priority
import com.example.data.repository.DocumentUpload
import com.example.ui.components.*
import com.example.ui.viewmodel.AcademicViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DocumentImportTab(val title: String) {
    IMPORT("Importar"),
    HISTORY("Histórico")
}

private data class SelectedImportFile(val uri: Uri, val name: String, val mimeType: String, val size: Long?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentImportScreen(
    viewModel: AcademicViewModel,
    onFinishedImport: () -> Unit
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingDocument.collectAsState()
    val extractedSyllabus by viewModel.extractedSyllabus.collectAsState()
    val importSuccessMessage by viewModel.importSuccessMessage.collectAsState()
    val documentAnalysisError by viewModel.documentAnalysisError.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val disciplines by viewModel.disciplines.collectAsState()

    val disciplinesMap = remember(disciplines) { disciplines.associateBy { it.id } }

    var activeTab by remember { mutableStateOf(DocumentImportTab.IMPORT) }
    var selectedFiles by remember { mutableStateOf<List<SelectedImportFile>>(emptyList()) }

    // Editable copy of events for review
    var editableEvents by remember { mutableStateOf<List<ExtractedEvent>>(emptyList()) }
    var editableSyllabus by remember { mutableStateOf<ExtractedSyllabus?>(null) }
    var editingEventIndex by remember { mutableStateOf<Int?>(null) }

    // State for viewing/deleting documents in history
    var documentToView by remember { mutableStateOf<AcademicDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<AcademicDocument?>(null) }

    fun readSelectedFile(uri: Uri): SelectedImportFile {
        var displayName = "documento_academico"
        var fileSize: Long? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) displayName = cursor.getString(nameIndex)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) fileSize = cursor.getLong(sizeIndex)
            }
        }
        return SelectedImportFile(uri, displayName, context.contentResolver.getType(uri) ?: "application/octet-stream", fileSize)
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFiles = runCatching { listOf(readSelectedFile(uri)) }.getOrDefault(emptyList())
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val added = uris.mapNotNull { uri -> runCatching { readSelectedFile(uri) }.getOrNull() }
        selectedFiles = (selectedFiles + added).distinctBy { it.uri }.take(10)
    }

    LaunchedEffect(extractedSyllabus) {
        val extracted = extractedSyllabus
        if (extracted != null) {
            val events = extracted.events.map { it.copy() }
            editableSyllabus = extracted.copy(
                schedule = inferScheduleDaysFromClasses(extracted.schedule, events)
            )
            editableEvents = events
            activeTab = DocumentImportTab.IMPORT
        } else {
            editableSyllabus = null
            editableEvents = emptyList()
            editingEventIndex = null
        }
    }

    LaunchedEffect(importSuccessMessage) {
        if (importSuccessMessage != null) selectedFiles = emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("document_import_screen"),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (extractedSyllabus != null) 104.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Hero Header Banner
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
                                colors = listOf(
                                    Color(0xFF4F46E5), // Indigo
                                    Color(0xFF7C3AED), // Purple
                                    Color(0xFF2563EB)  // Blue
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Inteligência Artificial Gemini", color = Color(0xFFC7D2FE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Leitor & Extrator de Documentos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Envie PDFs, fotos e capturas de tela com informações acadêmicas para organizar tudo automaticamente no seu calendário.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Navigation Tabs: Importar vs Histórico de Documentos
        item {
            TabRow(
                selectedTabIndex = if (activeTab == DocumentImportTab.IMPORT) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = activeTab == DocumentImportTab.IMPORT,
                    onClick = { activeTab = DocumentImportTab.IMPORT },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar Documento", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_import_document")
                )
                Tab(
                    selected = activeTab == DocumentImportTab.HISTORY,
                    onClick = { activeTab = DocumentImportTab.HISTORY },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Histórico (${documents.size})", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_document_history")
                )
            }
        }

        // Success Alert
        if (importSuccessMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Importação Concluída!", color = Color(0xFF14532D), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(importSuccessMessage!!, color = Color(0xFF166534), fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.clearExtractedSyllabus() }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF166534))
                        }
                    }
                }
            }
        }

        if (documentAnalysisError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = documentAnalysisError!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // TAB 1: IMPORTAR DOCUMENTO
        if (activeTab == DocumentImportTab.IMPORT) {
            if (extractedSyllabus == null) {
                // Dedicated File Import Action Buttons
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Importar Documento do Dispositivo",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Escolha um PDF ou imagem. A IA identifica disciplinas, aulas e avaliações para você revisar antes de adicionar ao calendário.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.weight(1f).testTag("btn_pick_pdf"),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        selectedFiles = emptyList()
                                        imagePickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_pick_images"),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Imagens", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            AnimatedVisibility(visible = selectedFiles.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("selected_documents_confirmation"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    if (selectedFiles.size == 1) "Arquivo pronto para processar" else "${selectedFiles.size} imagens da mesma disciplina",
                                                    color = Color(0xFF14532D),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                if (selectedFiles.size > 1) {
                                                    Text("As imagens serão analisadas em conjunto", color = Color(0xFF15803D), fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        selectedFiles.forEachIndexed { index, file ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (file.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = Color(0xFF166534),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(file.name, color = Color(0xFF166534), fontSize = 11.sp, maxLines = 1)
                                                    file.size?.let { Text(formatFileSize(it), color = Color(0xFF15803D), fontSize = 10.sp) }
                                                }
                                                IconButton(
                                                    onClick = { selectedFiles = selectedFiles.filterIndexed { itemIndex, _ -> itemIndex != index } },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remover ${file.name}", tint = Color(0xFF166534), modifier = Modifier.size(17.dp))
                                                }
                                            }
                                        }
                                        if (selectedFiles.all { it.mimeType.startsWith("image/") } && selectedFiles.size < 10) {
                                            TextButton(
                                                onClick = { imagePickerLauncher.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(17.dp))
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text("Selecionar novamente (até 10 imagens)", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.analyzeDocuments(
                                    selectedFiles.map { DocumentUpload(it.uri, it.name, it.mimeType) }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_process_document"),
                            enabled = selectedFiles.isNotEmpty() && !isAnalyzing,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analisando com Inteligência Artificial...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processar Documento e Extrair Eventos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                        if (isAnalyzing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "A leitura e organização do documento pode levar alguns minutos. Mantenha o aplicativo aberto.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Step 2: Interactive Review of Extracted Information
            if (extractedSyllabus != null) {
                val syllabus = editableSyllabus ?: extractedSyllabus!!
                val discColor = parseColorHex(syllabus.recommendedColorHex)

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Revisão das Informações Identificadas",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = { viewModel.clearExtractedSyllabus() }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Extracted Subject Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, discColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(discColor))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Revise os dados da disciplina",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = discColor
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = syllabus.disciplineName,
                                onValueChange = { editableSyllabus = syllabus.copy(disciplineName = it) },
                                label = { Text("Nome da disciplina *") },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = syllabus.disciplineCode,
                                    onValueChange = { editableSyllabus = syllabus.copy(disciplineCode = it) },
                                    label = { Text("Código") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = syllabus.term,
                                    onValueChange = { editableSyllabus = syllabus.copy(term = it) },
                                    label = { Text("Período") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = syllabus.professor,
                                onValueChange = { editableSyllabus = syllabus.copy(professor = it) },
                                label = { Text("Professor(a)") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = syllabus.classroom,
                                onValueChange = { editableSyllabus = syllabus.copy(classroom = it) },
                                label = { Text("Local / Sala") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ImportedScheduleEditor(
                                schedule = syllabus.schedule,
                                onScheduleChange = { schedule ->
                                    editableSyllabus = syllabus.copy(schedule = schedule)
                                }
                            )
                            val scheduleStartTime = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
                                .find(syllabus.schedule)?.value?.padStart(5, '0')
                            if (scheduleStartTime != null && editableEvents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        editableEvents = editableEvents.map { it.copy(time = scheduleStartTime) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Aplicar $scheduleStartTime a todos os eventos")
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedCount = editableEvents.count { it.isSelected }
                        Text(
                            text = "Eventos Identificados ($selectedCount/${editableEvents.size} selecionados):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        TextButton(onClick = {
                            val allSelected = editableEvents.all { it.isSelected }
                            editableEvents = editableEvents.map { it.copy(isSelected = !allSelected) }
                        }) {
                            Text(if (editableEvents.all { it.isSelected }) "Desmarcar Todos" else "Marcar Todos", fontSize = 12.sp)
                        }
                    }
                }

                // List of Extracted Events with toggle & editing
                itemsIndexed(editableEvents) { index, event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editableEvents = editableEvents.toMutableList().also { list ->
                                    list[index] = event.copy(isSelected = !event.isSelected)
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (event.isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (event.isSelected) BorderStroke(1.dp, discColor.copy(alpha = 0.4f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = event.isSelected,
                                onCheckedChange = { checked ->
                                    editableEvents = editableEvents.toMutableList().also { list ->
                                        list[index] = event.copy(isSelected = checked)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ActivityTypeBadge(type = event.activityType)
                                    PriorityBadge(priority = event.priority)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (event.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (event.time.isBlank()) "${formatImportDate(event.date)} • Horário não informado" else "${formatImportDate(event.date)} às ${event.time}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { editingEventIndex = index }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar evento")
                            }
                        }
                    }
                }

                // Confirm Button
                item {
                    val selectedEvents = editableEvents.filter { it.isSelected }
                    Button(
                        onClick = {
                            if (selectedEvents.isNotEmpty()) {
                                viewModel.confirmAndSaveImportedEvents(syllabus, selectedEvents)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_confirm_import"),
                        enabled = selectedEvents.isNotEmpty() && syllabus.disciplineName.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar e Adicionar ${selectedEvents.size} Eventos ao Calendário", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // TAB 2: HISTÓRICO DE DOCUMENTOS ENVIADOS
        if (activeTab == DocumentImportTab.HISTORY) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Histórico de Documentos Enviados",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${documents.size} documento(s) processados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { activeTab = DocumentImportTab.IMPORT },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar Novo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (documents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nenhum documento enviado ainda",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Quando você importar PDFs ou imagens acadêmicas, o histórico completo ficará registrado aqui.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { activeTab = DocumentImportTab.IMPORT },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importar Primeiro Documento", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(documents) { doc ->
                    val disc = disciplinesMap[doc.disciplineId]
                    val discColor = disc?.let { parseColorHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { documentToView = doc }
                            .testTag("doc_history_item_${doc.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFEE2E2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = doc.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = doc.fileName,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { documentToDelete = doc },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Excluir Documento",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (disc != null) {
                                    Surface(
                                        color = discColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(discColor))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = disc.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = discColor
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Disciplina geral",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${doc.itemsExtractedCount} eventos extraídos",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (doc.parsedSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = doc.parsedSummary,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
            }
        }

        val reviewSyllabus = editableSyllabus ?: extractedSyllabus
        val selectedReviewEvents = editableEvents.filter { it.isSelected }
        if (
            activeTab == DocumentImportTab.IMPORT &&
            reviewSyllabus != null &&
            reviewSyllabus.disciplineName.isNotBlank() &&
            selectedReviewEvents.isNotEmpty()
        ) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.confirmAndSaveImportedEvents(reviewSyllabus, selectedReviewEvents) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_confirm_import"),
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("Salvar ${selectedReviewEvents.size} eventos", fontWeight = FontWeight.Bold) }
            )
        }
    }

    editingEventIndex?.let { index ->
        editableEvents.getOrNull(index)?.let { event ->
            EditImportedEventDialog(
                event = event,
                onDismiss = { editingEventIndex = null },
                onSave = { updated ->
                    editableEvents = editableEvents.toMutableList().also { list ->
                        if (index in list.indices) list[index] = updated
                    }
                    editingEventIndex = null
                }
            )
        }
    }

    // Detail Dialog for selected document in history
    if (documentToView != null) {
        val doc = documentToView!!
        val disc = disciplinesMap[doc.disciplineId]
        val discColor = disc?.let { parseColorHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary

        AlertDialog(
            onDismissRequest = { documentToView = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(doc.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• Arquivo: ${doc.fileName}", fontSize = 13.sp)
                    if (disc != null) {
                        Text("• Disciplina: ${disc.name} (${disc.code})", fontSize = 13.sp, color = discColor, fontWeight = FontWeight.Bold)
                        if (disc.professor.isNotBlank()) {
                            Text("• Professor: ${disc.professor}", fontSize = 13.sp)
                        }
                    }
                    Text("• Itens Importados: ${doc.itemsExtractedCount} eventos no calendário", fontSize = 13.sp)
                    if (doc.parsedSummary.isNotBlank()) {
                        Text("• Resumo da Extração: ${doc.parsedSummary}", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { documentToView = null }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (documentToDelete != null) {
        val doc = documentToDelete!!
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir do Histórico?") },
            text = {
                Text("Deseja remover \"${doc.title}\" do histórico de documentos enviados? Os eventos criados no calendário continuarão salvos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc.id)
                        documentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EditImportedEventDialog(
    event: ExtractedEvent,
    onDismiss: () -> Unit,
    onSave: (ExtractedEvent) -> Unit
) {
    var title by remember(event) { mutableStateOf(event.title) }
    var date by remember(event) { mutableStateOf(formatImportDate(event.date)) }
    var time by remember(event) { mutableStateOf(event.time) }
    var description by remember(event) { mutableStateOf(event.description) }
    val parsedDate = parseImportDate(date)
    val validDate = parsedDate != null
    val validTime = time.isBlank() || Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
        title = { Text("Editar evento importado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = formatBrazilianDateInput(it) },
                    label = { Text("Data *") },
                    placeholder = { Text("DD/MM/AA") },
                    supportingText = { if (date.isNotBlank() && !validDate) Text("Informe uma data válida no formato DD/MM/AA") },
                    isError = date.isNotBlank() && !validDate,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { value ->
                        if (value.length <= 5 && value.all { it.isDigit() || it == ':' }) time = value
                    },
                    label = { Text("Horário") },
                    placeholder = { Text("HH:MM") },
                    supportingText = { if (!validTime) Text("Use um horário entre 00:00 e 23:59") },
                    isError = !validTime,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && validDate && validTime,
                onClick = {
                    onSave(event.copy(title = title.trim(), date = parsedDate!!, time = time, description = description.trim()))
                }
            ) { Text("Salvar alterações") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private data class ImportWeekDay(val shortName: String, val fullName: String)

private val importWeekDays = listOf(
    ImportWeekDay("SEG", "Segunda"),
    ImportWeekDay("TER", "Terça"),
    ImportWeekDay("QUA", "Quarta"),
    ImportWeekDay("QUI", "Quinta"),
    ImportWeekDay("SEX", "Sexta"),
    ImportWeekDay("SÁB", "Sábado"),
    ImportWeekDay("DOM", "Domingo")
)

internal fun formatImportDate(isoDate: String): String {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    val output = SimpleDateFormat("dd/MM/yy", Locale.forLanguageTag("pt-BR"))
    return runCatching { input.parse(isoDate)?.let(output::format) }.getOrNull() ?: isoDate
}

internal fun parseImportDate(brazilianDate: String): String? {
    val parts = brazilianDate.split('/')
    if (parts.size != 3 || parts[0].length != 2 || parts[1].length != 2 || parts[2].length != 2) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull()?.plus(2000) ?: return null
    val canonical = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month, year)
    val parser = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply { isLenient = false }
    val parsed = runCatching { parser.parse(canonical) }.getOrNull() ?: return null
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(parsed)
}

internal fun formatBrazilianDateInput(value: String): String {
    val digits = value.filter(Char::isDigit).take(6)
    return buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 2 || index == 4) append('/')
            append(digit)
        }
    }
}

internal fun inferScheduleDaysFromClasses(
    schedule: String,
    events: List<ExtractedEvent>
): String {
    val classDates = events
        .asSequence()
        .filter { it.activityType == ActivityType.AULA }
        .map { it.date }
        .distinct()
        .toList()
    if (classDates.isEmpty()) return schedule

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    val calendar = Calendar.getInstance()
    val inferredDays = classDates.mapNotNull { date ->
        val parsed = runCatching { formatter.parse(date) }.getOrNull() ?: return@mapNotNull null
        calendar.time = parsed
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> importWeekDays[0]
            Calendar.TUESDAY -> importWeekDays[1]
            Calendar.WEDNESDAY -> importWeekDays[2]
            Calendar.THURSDAY -> importWeekDays[3]
            Calendar.FRIDAY -> importWeekDays[4]
            Calendar.SATURDAY -> importWeekDays[5]
            Calendar.SUNDAY -> importWeekDays[6]
            else -> null
        }
    }.distinct().sortedBy { importWeekDays.indexOf(it) }
    if (inferredDays.isEmpty()) return schedule

    val normalized = schedule.lowercase(Locale.ROOT)
    val alreadyRepresented = inferredDays.filter { day ->
        when (day.shortName) {
            "SEG" -> normalized.contains("seg")
            "TER" -> normalized.contains("ter")
            "QUA" -> normalized.contains("qua")
            "QUI" -> normalized.contains("qui")
            "SEX" -> normalized.contains("sex")
            "SÁB" -> normalized.contains("sáb") || normalized.contains("sab")
            else -> normalized.contains("dom")
        }
    }.toSet()
    val missingDays = inferredDays.filterNot { it in alreadyRepresented }
    if (missingDays.isEmpty()) return schedule

    val inferredText = missingDays.joinToString(" e ") { it.fullName }
    return listOf(inferredText, schedule.trim()).filter { it.isNotBlank() }.joinToString(", ")
}

@Composable
private fun ImportedScheduleEditor(
    schedule: String,
    onScheduleChange: (String) -> Unit
) {
    val context = LocalContext.current
    val normalizedSchedule = schedule.lowercase()
    val selectedDays = importWeekDays.filter { day ->
        when (day.shortName) {
            "SEG" -> normalizedSchedule.contains("seg")
            "TER" -> normalizedSchedule.contains("ter")
            "QUA" -> normalizedSchedule.contains("qua")
            "QUI" -> normalizedSchedule.contains("qui")
            "SEX" -> normalizedSchedule.contains("sex")
            "SÁB" -> normalizedSchedule.contains("sáb") || normalizedSchedule.contains("sab")
            else -> normalizedSchedule.contains("dom")
        }
    }
    val times = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
        .findAll(schedule).map { it.value.padStart(5, '0') }.toList()
    val startTime = times.getOrNull(0).orEmpty()
    val endTime = times.getOrNull(1).orEmpty()

    fun rebuild(days: List<ImportWeekDay>, start: String, end: String): String {
        val dayText = days.joinToString(" e ") { it.fullName }
        val timeText = when {
            start.isNotBlank() && end.isNotBlank() -> "$start - $end"
            start.isNotBlank() -> start
            else -> ""
        }
        return listOf(dayText, timeText).filter { it.isNotBlank() }.joinToString(", ")
    }

    fun openTimePicker(current: String, onSelected: (String) -> Unit) {
        val parts = current.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        android.app.TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            onSelected(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(7.dp))
            Text("Dias e horários das aulas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            importWeekDays.forEach { day ->
                val selected = day in selectedDays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(9.dp))
                        .clickable {
                            val updatedDays = if (selected) selectedDays - day else (selectedDays + day).sortedBy { importWeekDays.indexOf(it) }
                            onScheduleChange(rebuild(updatedDays, startTime, endTime))
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(day.shortName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Horário inicial" to startTime, "Horário final" to endTime).forEachIndexed { index, (label, value) ->
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(label, fontSize = 11.sp) },
                        placeholder = { Text("--:--") },
                        trailingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "Selecionar $label") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp)
                    )
                    Box(
                        modifier = Modifier.matchParentSize().clickable {
                            openTimePicker(value) { selectedTime ->
                                onScheduleChange(
                                    if (index == 0) rebuild(selectedDays, selectedTime, endTime)
                                    else rebuild(selectedDays, startTime, selectedTime)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
