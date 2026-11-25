package com.example.ai

import com.example.data.model.ActivityType
import com.example.data.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

data class ExtractedSyllabus(
    val documentTitle: String,
    val disciplineName: String,
    val disciplineCode: String = "",
    val professor: String = "",
    val term: String = "2026/2",
    val classroom: String = "",
    val schedule: String = "",
    val recommendedColorHex: String = "#2563EB",
    val events: List<ExtractedEvent> = emptyList()
)

data class ExtractedEvent(
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String = "", // HH:MM; vazio quando não informado
    val activityType: ActivityType = ActivityType.TRABALHO,
    val priority: Priority = Priority.MEDIA,
    val description: String = "",
    val dueDate: String? = null,
    var isSelected: Boolean = true
)

object GeminiDocumentParser {
    // AI extraction is handled by the backend. Keep this legacy entry point local.
    suspend fun parseAcademicDocument(documentText: String, fileName: String = "documento.pdf"): ExtractedSyllabus = withContext(Dispatchers.IO) {
        smartLocalParser(documentText, fileName)
    }

    fun smartLocalParser(text: String, fileName: String): ExtractedSyllabus {
        val lines = text.lines()
        var discName = "Disciplina"
        var prof = "Professor"
        var code = ""
        var term = "2026/2"
        var room = ""
        var sched = ""
        var color = "#2563EB"

        // Generic regex extraction for subject & professor if not found
        if (discName == "Disciplina") {
            val discRegex = Pattern.compile("(?i)(?:disciplina|matéria|curso|componente)\\s*[:\\-]?\\s*([^\n\r\\(]+)").matcher(text)
            if (discRegex.find()) {
                discName = discRegex.group(1)?.trim() ?: "Disciplina"
            }
        }
        if (prof == "Professor") {
            val profRegex = Pattern.compile("(?i)(?:professor|professora|docente|prof\\.?|dra?\\.?)\\s*[:\\-]?\\s*([^\n\r]+)").matcher(text)
            if (profRegex.find()) {
                prof = profRegex.group(1)?.trim() ?: "Professor"
            }
        }

        // Assign discipline-specific color based on name keywords
        color = when {
            discName.contains("Cálculo", true) || discName.contains("Física", true) || discName.contains("Matemática", true) -> "#2563EB" // Azul
            discName.contains("Software", true) || discName.contains("Engenharia", true) || discName.contains("Projeto", true) -> "#16A34A" // Verde
            discName.contains("Banco", true) || discName.contains("Dados", true) && discName.contains("Relacional", true) -> "#EA580C" // Laranja
            discName.contains("Estrutura", true) || discName.contains("Algoritmo", true) || discName.contains("Computação", true) -> "#9333EA" // Roxo
            else -> "#0D9488"
        }

        // Extract dates and events
        val datePattern = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})(?:[/\\-](\\d{2,4}))?")
        val events = mutableListOf<ExtractedEvent>()

        for (line in lines) {
            val trimmed = line.trim().removePrefix("-").removePrefix("*").trim()
            val matcher = datePattern.matcher(trimmed)
            if (matcher.find()) {
                val day = matcher.group(1)?.padStart(2, '0') ?: "01"
                val month = matcher.group(2)?.padStart(2, '0') ?: "09"
                val year = matcher.group(3)?.let { if (it.length == 2) "20$it" else it } ?: "2026"
                val dateStr = "$year-$month-$day"

                // Extract time if present
                val timeMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(trimmed)
                val timeStr = if (timeMatcher.find()) {
                    val h = timeMatcher.group(1)?.padStart(2, '0') ?: "08"
                    val m = timeMatcher.group(2) ?: "00"
                    "$h:$m"
                } else ""

                // Clean event title
                var eventTitle = trimmed.substring(matcher.end()).trim().removePrefix(":").removePrefix("-").trim()
                if (timeMatcher.find(0)) {
                    eventTitle = eventTitle.replace(timeMatcher.group(0) ?: "", "").trim().removePrefix(":").removePrefix("-").trim()
                }

                if (eventTitle.length > 2) {
                    val lower = eventTitle.lowercase()
                    val type = when {
                        lower.contains("prova") || lower.contains("avaliação") || lower.contains("exame") || lower.contains("p1") || lower.contains("p2") || lower.contains("p3") -> ActivityType.PROVA
                        lower.contains("entrega") || lower.contains("trabalho") -> ActivityType.TRABALHO
                        lower.contains("seminário") || lower.contains("apresentação") -> ActivityType.SEMINARIO
                        lower.contains("projeto") -> ActivityType.PROJETO
                        lower.contains("exercício") || lower.contains("lista") -> ActivityType.EXERCICIO
                        lower.contains("leitura") || lower.contains("artigo") -> ActivityType.LEITURA
                        lower.contains("aula") -> ActivityType.AULA
                        else -> ActivityType.PRAZO_ENTREGA
                    }

                    val priority = when (type) {
                        ActivityType.PROVA -> Priority.URGENTE
                        ActivityType.PROJETO, ActivityType.TRABALHO, ActivityType.SEMINARIO -> Priority.ALTA
                        ActivityType.EXERCICIO, ActivityType.PRAZO_ENTREGA -> Priority.MEDIA
                        ActivityType.AULA, ActivityType.LEITURA, ActivityType.EVENTO -> Priority.BAIXA
                    }

                    events.add(
                        ExtractedEvent(
                            title = eventTitle,
                            date = dateStr,
                            time = timeStr,
                            activityType = type,
                            priority = priority,
                            description = "Extraído de $fileName",
                            dueDate = if (type == ActivityType.TRABALHO || type == ActivityType.PROJETO) dateStr else null
                        )
                    )
                }
            }
        }

        return ExtractedSyllabus(
            documentTitle = "Plano de Ensino — $discName",
            disciplineName = discName,
            disciplineCode = code,
            professor = prof,
            term = term,
            classroom = room,
            schedule = sched,
            recommendedColorHex = color,
            events = events
        )
    }
}
