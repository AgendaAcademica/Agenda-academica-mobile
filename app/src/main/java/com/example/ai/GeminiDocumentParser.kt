package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActivityType
import com.example.data.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
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
    val time: String = "08:00", // HH:MM
    val activityType: ActivityType = ActivityType.TRABALHO,
    val priority: Priority = Priority.MEDIA,
    val description: String = "",
    val dueDate: String? = null,
    var isSelected: Boolean = true
)

object GeminiDocumentParser {
    private const val TAG = "GeminiDocParser"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parseAcademicDocument(documentText: String, fileName: String = "documento.pdf"): ExtractedSyllabus = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildPrompt(documentText, fileName)
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            }
                            put("parts", parts)
                        })
                    }
                    put("contents", contents)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                        put("responseMimeType", "application/json")
                    })
                }

                val request = Request.Builder()
                    .url("$API_URL?key=$apiKey")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val parsed = parseStructuredJson(text, fileName, documentText)
                    if (parsed != null && parsed.events.isNotEmpty()) {
                        return@withContext parsed
                    }
                } else {
                    Log.w(TAG, "Gemini API error code: ${response.code}, falling back to semantic parser")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API call failed: ${e.message}, using smart local extractor", e)
            }
        }

        // Fallback: Smart local rule-based extractor
        return@withContext smartLocalParser(documentText, fileName)
    }

    private fun buildPrompt(documentText: String, fileName: String): String {
        return """
            Você é um assistente de inteligência artificial especializado em estruturar documentos acadêmicos (planos de ensino, cronogramas de aulas, calendários de provas e PDFs acadêmicos).
            
            Analise o seguinte documento acadêmico ("$fileName") e extraia rigorosamente todas as informações em formato JSON estruturado com o seguinte schema:
            {
              "documentTitle": "Título do Documento",
              "disciplineName": "Nome da Disciplina (ex: Engenharia de Software, Cálculo, etc)",
              "disciplineCode": "Código (ex: ES204, se houver)",
              "professor": "Nome do Professor",
              "term": "Período Letivo (ex: 2026/2)",
              "classroom": "Sala ou Laboratório",
              "schedule": "Dias e horários das aulas (ex: Terça e Quinta, 14:00 - 16:00)",
              "recommendedColorHex": "Cor Hex sugerida (ex: #2563EB para exatas, #16A34A para eng/software, #EA580C para dados, #9333EA para computação)",
              "events": [
                {
                  "title": "Nome descritivo da atividade ou aula",
                  "date": "YYYY-MM-DD (usando ano 2026 e datas identificadas)",
                  "time": "HH:MM (horário de início ou entrega)",
                  "activityType": "AULA | PROVA | TRABALHO | EXERCICIO | SEMINARIO | PROJETO | LEITURA | PRAZO_ENTREGA | EVENTO",
                  "priority": "BAIXA | MEDIA | ALTA | URGENTE",
                  "description": "Detalhes dos tópicos ou instruções",
                  "dueDate": "YYYY-MM-DD se for entrega"
                }
              ]
            }

            Texto do documento:
            $documentText
        """.trimIndent()
    }

    private fun parseStructuredJson(jsonStr: String, fallbackFileName: String, originalText: String): ExtractedSyllabus? {
        return try {
            val cleanJson = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)
            val docTitle = obj.optString("documentTitle", "Documento Acadêmico")
            val discName = obj.optString("disciplineName", "Disciplina")
            val discCode = obj.optString("disciplineCode", "")
            val prof = obj.optString("professor", "")
            val term = obj.optString("term", "2026/2")
            val room = obj.optString("classroom", "")
            val sched = obj.optString("schedule", "")
            val color = obj.optString("recommendedColorHex", "#2563EB")

            val eventsArr = obj.optJSONArray("events") ?: JSONArray()
            val events = mutableListOf<ExtractedEvent>()
            for (i in 0 until eventsArr.length()) {
                val eObj = eventsArr.getJSONObject(i)
                val typeStr = eObj.optString("activityType", "TRABALHO")
                val type = runCatching { ActivityType.valueOf(typeStr) }.getOrDefault(ActivityType.TRABALHO)
                val prioStr = eObj.optString("priority", "MEDIA")
                val prio = runCatching { Priority.valueOf(prioStr) }.getOrDefault(Priority.MEDIA)

                events.add(
                    ExtractedEvent(
                        title = eObj.optString("title", "Atividade"),
                        date = eObj.optString("date", "2026-09-10"),
                        time = eObj.optString("time", "08:00"),
                        activityType = type,
                        priority = prio,
                        description = eObj.optString("description", ""),
                        dueDate = if (eObj.has("dueDate") && !eObj.isNull("dueDate")) eObj.optString("dueDate") else null
                    )
                )
            }

            ExtractedSyllabus(
                documentTitle = docTitle,
                disciplineName = discName,
                disciplineCode = discCode,
                professor = prof,
                term = term,
                classroom = room,
                schedule = sched,
                recommendedColorHex = color,
                events = events
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}")
            null
        }
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

        // Check against sample signatures first for instant precision
        for (sample in SampleAcademicDocuments.SAMPLES) {
            if (text.contains(sample.disciplineName, ignoreCase = true) || fileName.contains(sample.id, ignoreCase = true)) {
                discName = sample.disciplineName
                prof = sample.professor
                code = sample.disciplineCode
                term = sample.term
                room = sample.classroom
                sched = sample.schedule
                color = sample.colorHex
                break
            }
        }

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
                } else "08:00"

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
