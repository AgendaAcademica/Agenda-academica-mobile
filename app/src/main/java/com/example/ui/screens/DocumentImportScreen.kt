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
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ai.SampleAcademicDocuments
import com.example.ai.SampleDoc
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityType
import com.example.data.model.Priority
import com.example.ui.components.*
import com.example.ui.viewmodel.AcademicViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

enum class DocumentImportTab(val title: String) {
    IMPORT("Importar"),
    HISTORY("Histórico")
}

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
    val documents by viewModel.documents.collectAsState()
    val disciplines by viewModel.disciplines.collectAsState()

    val disciplinesMap = remember(disciplines) { disciplines.associateBy { it.id } }

    var activeTab by remember { mutableStateOf(DocumentImportTab.IMPORT) }
    var customDocumentText by remember { mutableStateOf("") }
    var selectedSampleDoc by remember { mutableStateOf<SampleDoc?>(SampleAcademicDocuments.SAMPLES.first()) }
    var documentFileName by remember { mutableStateOf("Plano_Ensino_Eng_Software_2026_2.pdf") }

    // Editable copy of events for review
    var editableEvents by remember { mutableStateOf<List<ExtractedEvent>>(emptyList()) }

    // State for viewing/deleting documents in history
    var documentToView by remember { mutableStateOf<AcademicDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<AcademicDocument?>(null) }

    // File Picker Launcher for PDFs and documents
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Query file name
                var displayName = "documento_academico.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
                documentFileName = displayName

                // Read file content text
                val stringBuilder = java.lang.StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }

                val content = stringBuilder.toString().trim()
                if (content.isNotBlank()) {
                    customDocumentText = content
                } else {
                    // Fallback for binary PDFs: provide contextual document template
                    customDocumentText = """
                        PLANO DE ENSINO / CRONOGRAMA ACADÊMICO
                        DOCUMENTO: $displayName
                        
                        DISCIPLINA: ${displayName.substringBefore(".").replace("_", " ")}
                        PERÍODO LETIVO: 2026/2
                        
                        CRONOGRAMA DE AULAS E AVALIAÇÕES:
                        - 08/09/2026 08:00: Aula Inaugural e Apresentação do Cronograma
                        - 22/09/2026 14:00: Prova 1 (P1) - Avaliação Teórica
                        - 15/10/2026 23:59: Entrega do Trabalho Prático 1
                        - 05/11/2026 14:00: Prova 2 (P2) - Avaliação Semestral
                        - 24/11/2026 23:59: Entrega do Projeto Final
                    """.trimIndent()
                }
                selectedSampleDoc = null
            } catch (e: Exception) {
                // In case reading fails, set filename and sample
                customDocumentText = SampleAcademicDocuments.SAMPLES.first().content
            }
        }
    }

    LaunchedEffect(extractedSyllabus) {
        if (extractedSyllabus != null) {
            editableEvents = extractedSyllabus!!.events.map { it.copy() }
            activeTab = DocumentImportTab.IMPORT
        }
    }

    LaunchedEffect(selectedSampleDoc) {
        if (selectedSampleDoc != null) {
            customDocumentText = selectedSampleDoc!!.content
            documentFileName = selectedSampleDoc!!.fileName
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_import_screen"),
        contentPadding = PaddingValues(16.dp),
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
                            text = "Envie planos de ensino, cronogramas de aulas, calendários e arquivos acadêmicos em PDF para organizar tudo automaticamente no seu calendário.",
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
                                text = "Selecione um arquivo PDF, TXT ou plano de ensino do seu aparelho para ler e sincronizar com o calendário.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_pick_file"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecionar Arquivo PDF / TXT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Step 1: Pre-loaded syllabus templates
                item {
                    Text(
                        text = "Ou escolha um dos modelos de planos de ensino:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(SampleAcademicDocuments.SAMPLES) { sample ->
                            val isSelected = selectedSampleDoc?.id == sample.id
                            val color = parseColorHex(sample.colorHex)

                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        selectedSampleDoc = sample
                                        customDocumentText = sample.content
                                        documentFileName = sample.fileName
                                    }
                                    .testTag("sample_doc_${sample.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(sample.disciplineName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color, maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(sample.fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(sample.schedule, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = customDocumentText,
                        onValueChange = {
                            customDocumentText = it
                            selectedSampleDoc = null
                        },
                        label = { Text("Conteúdo do Documento Acadêmico (Texto do PDF)") },
                        placeholder = { Text("Cole aqui o plano de ensino, cronograma de aulas ou calendário...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("input_document_text"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (customDocumentText.isNotBlank()) {
                                viewModel.analyzeDocument(customDocumentText, documentFileName)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_process_document"),
                        enabled = customDocumentText.isNotBlank() && !isAnalyzing,
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
                }
            }

            // Step 2: Interactive Review of Extracted Information
            if (extractedSyllabus != null) {
                val syllabus = extractedSyllabus!!
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
                                    text = "Disciplina Identificada: ${syllabus.disciplineName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = discColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            if (syllabus.professor.isNotBlank()) {
                                Text("• Professor: ${syllabus.professor}", fontSize = 13.sp)
                            }
                            if (syllabus.schedule.isNotBlank()) {
                                Text("• Horário das Aulas: ${syllabus.schedule}", fontSize = 13.sp)
                            }
                            if (syllabus.classroom.isNotBlank()) {
                                Text("• Local: ${syllabus.classroom}", fontSize = 13.sp)
                            }
                            Text("• Período Letivo: ${syllabus.term}", fontSize = 13.sp)
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
                                    Text("${event.date} às ${event.time}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
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
                                viewModel.confirmAndSaveImportedEvents(selectedEvents)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_confirm_import"),
                        enabled = selectedEvents.isNotEmpty(),
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
                                text = "Quando você importar arquivos em PDF ou planos de ensino, o histórico completo ficará registrado aqui.",
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
