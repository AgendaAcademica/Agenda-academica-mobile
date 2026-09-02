package com.example.data.repository

import android.content.Context
import com.example.ai.ExtractedSyllabus
import com.example.ai.SampleAcademicDocuments
import com.example.data.local.AppDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.AcademicDocument
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import com.example.data.model.Priority
import com.example.widget.AcademicDayAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DisciplineWithStats(
    val discipline: Discipline,
    val totalActivities: Int,
    val completedActivities: Int,
    val nextActivity: AcademicActivity? = null
) {
    val progressFraction: Float
        get() = if (totalActivities > 0) completedActivities.toFloat() / totalActivities else 0f
}

data class DashboardOverview(
    val todayActivities: List<ActivityWithDiscipline>,
    val upcomingExams: List<ActivityWithDiscipline>,
    val urgentDeadlines: List<ActivityWithDiscipline>,
    val delayedActivities: List<ActivityWithDiscipline>,
    val weeklyPendingCount: Int,
    val totalCompletedCount: Int,
    val disciplineStats: List<DisciplineWithStats>
)

data class ActivityWithDiscipline(
    val activity: AcademicActivity,
    val discipline: Discipline
)

class AcademicRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val disciplineDao = database.disciplineDao()
    private val activityDao = database.activityDao()
    private val documentDao = database.documentDao()

    val allDisciplines: Flow<List<Discipline>> = disciplineDao.getAllDisciplines()
    val allActivities: Flow<List<AcademicActivity>> = activityDao.getAllActivities()
    val allDocuments: Flow<List<AcademicDocument>> = documentDao.getAllDocuments()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = disciplineDao.getAllDisciplines().first()
        if (existing.isNotEmpty()) return

        // Seed default Brazilian Computer Science semester 2026/2
        val calcId = disciplineDao.insertDiscipline(
            Discipline(
                name = "Cálculo",
                code = "MAT101",
                professor = "Profa. Elena Ramos",
                classroom = "Auditório Central 102",
                schedule = "Segunda e Quarta, 08:00 - 10:00",
                term = "2026/2",
                colorHex = "#0061A4", // Azul Acadêmico
                syllabusNotes = "Cálculo Diferencial e Integral I: limites, continuidade, derivadas e integrais."
            )
        )

        val engSoftId = disciplineDao.insertDiscipline(
            Discipline(
                name = "Engenharia de Software",
                code = "ES204",
                professor = "Prof. João Silva",
                classroom = "Lab Info 04 - Bloco C",
                schedule = "Terça e Quinta, 14:00 - 16:00",
                term = "2026/2",
                colorHex = "#006E1C", // Verde Profissional
                syllabusNotes = "Metodologias ágeis, Clean Architecture, UML, Testes e Engenharia de Requisitos."
            )
        )

        val bdId = disciplineDao.insertDiscipline(
            Discipline(
                name = "Banco de Dados",
                code = "BD301",
                professor = "Prof. Carlos Mendes",
                classroom = "Lab de Redes e Dados 02",
                schedule = "Segunda e Quarta, 18:30 - 20:30",
                term = "2026/2",
                colorHex = "#8B5000", // Âmbar Acadêmico
                syllabusNotes = "Normalização, SQL avançado, transações ACID, índices e NoSQL."
            )
        )

        val edId = disciplineDao.insertDiscipline(
            Discipline(
                name = "Estrutura de Dados",
                code = "ED202",
                professor = "Profa. Fernanda Costa",
                classroom = "Sala 204 - Bloco A",
                schedule = "Sexta-feira, 08:00 - 12:00",
                term = "2026/2",
                colorHex = "#6750A4", // Roxo Profissional
                syllabusNotes = "Árvores AVL, Tabelas Hash, Algoritmos em Grafos e Heaps."
            )
        )

        // Add seed activities
        val sampleActivities = listOf(
            // Today (2026-09-01) activities matching prompt example
            AcademicActivity(
                disciplineId = calcId,
                title = "Aula — Limites e Derivadas",
                description = "Revisão de funções elementares e definição formal de limites.",
                activityType = ActivityType.AULA,
                date = "2026-09-01",
                time = "08:00",
                priority = Priority.MEDIA,
                status = ActivityStatus.CONCLUIDA,
                sourceDocument = "Cronograma_Calculo_1_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Entrega da Atividade 3 — Modelagem Ágil",
                description = "Entrega do diagrama de classes de domínio e user stories no repositório.",
                activityType = ActivityType.TRABALHO,
                date = "2026-09-01",
                time = "14:00",
                dueDate = "2026-09-01",
                priority = Priority.URGENTE,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = bdId,
                title = "Aula — Normalização de Dados",
                description = "Primeira, Segunda e Terceira Formas Normais (1FN, 2FN, 3FN).",
                activityType = ActivityType.AULA,
                date = "2026-09-01",
                time = "18:30",
                priority = Priority.MEDIA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Banco_de_Dados_2026.pdf"
            ),

            // Additional semester activities matching prompt
            AcademicActivity(
                disciplineId = edId,
                title = "Aula — Listas Encadeadas e Pilhas",
                description = "Estruturas lineares dinâmicas e alocação de memória.",
                activityType = ActivityType.AULA,
                date = "2026-09-04",
                time = "08:00",
                priority = Priority.BAIXA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Programa_Estrutura_Dados_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = calcId,
                title = "Lista de Exercícios 1 — Limites Fundamentais",
                description = "Resolver exercícios da lista 1 sobre teorema do confronto.",
                activityType = ActivityType.EXERCICIO,
                date = "2026-09-08",
                time = "23:59",
                dueDate = "2026-09-08",
                priority = Priority.MEDIA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Cronograma_Calculo_1_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Entrega da Atividade 1 — Documento SRS",
                description = "Especificação de Requisitos de Software segundo padrão IEEE.",
                activityType = ActivityType.TRABALHO,
                date = "2026-09-10",
                time = "14:00",
                dueDate = "2026-09-10",
                priority = Priority.ALTA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = bdId,
                title = "Entrega do Trabalho Prático 1 — DER",
                description = "Diagrama Entidade-Relacionamento e script DDL PostgreSQL.",
                activityType = ActivityType.TRABALHO,
                date = "2026-09-12",
                time = "23:59",
                dueDate = "2026-09-12",
                priority = Priority.ALTA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Banco_de_Dados_2026.pdf"
            ),
            AcademicActivity(
                disciplineId = calcId,
                title = "Prova 1 (P1) — Limites e Derivadas",
                description = "Avaliação individual sem consulta sobre todo o conteúdo da unidade 1.",
                activityType = ActivityType.PROVA,
                date = "2026-09-22",
                time = "08:00",
                priority = Priority.URGENTE,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Cronograma_Calculo_1_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Prova 1 (P1) — Engenharia de Requisitos",
                description = "Avaliação teórica e prática sobre processos ágeis e elicitação.",
                activityType = ActivityType.PROVA,
                date = "2026-09-24",
                time = "14:00",
                priority = Priority.URGENTE,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Apresentação de Seminário — Design Patterns",
                description = "Seminário em duplas sobre padrões de projeto GoF.",
                activityType = ActivityType.SEMINARIO,
                date = "2026-10-15",
                time = "14:00",
                priority = Priority.ALTA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Entrega do Projeto — Arquitetura de Software",
                description = "Primeira entrega do sistema completo com arquitetura em camadas.",
                activityType = ActivityType.PROJETO,
                date = "2026-10-30",
                time = "23:59",
                dueDate = "2026-10-30",
                priority = Priority.URGENTE,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = engSoftId,
                title = "Prova 2 (P2) — Arquitetura e Testes",
                description = "Avaliação sobre padrões arquiteturais e testes unitários/integração.",
                activityType = ActivityType.PROVA,
                date = "2026-11-20",
                time = "14:00",
                priority = Priority.URGENTE,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Plano_Ensino_Eng_Software_2026_2.pdf"
            ),
            AcademicActivity(
                disciplineId = null,
                title = "Curso Livre — Prática de Conversação em Inglês",
                description = "Encontro semanal de conversação e vocabulário técnico acadêmico.",
                activityType = ActivityType.EVENTO,
                date = "2026-09-01",
                time = "20:00",
                priority = Priority.MEDIA,
                status = ActivityStatus.PENDENTE,
                sourceDocument = "Atividade Extra"
            )
        )

        activityDao.insertActivities(sampleActivities)

        // Seed document records
        documentDao.insertDocument(
            AcademicDocument(
                title = "Plano de Ensino — Engenharia de Software",
                fileName = "Plano_Ensino_Eng_Software_2026_2.pdf",
                disciplineId = engSoftId,
                parsedSummary = "6 eventos identificados (Aulas, Trabalhos, Seminários e Provas P1/P2)",
                itemsExtractedCount = 6
            )
        )
        documentDao.insertDocument(
            AcademicDocument(
                title = "Cronograma de Aulas e Provas — Cálculo I",
                fileName = "Cronograma_Calculo_1_2026_2.pdf",
                disciplineId = calcId,
                parsedSummary = "7 eventos identificados (Aulas, Listas de Exercícios, Provas P1/P2/P3)",
                itemsExtractedCount = 7
            )
        )

        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun saveImportedSyllabus(
        syllabus: ExtractedSyllabus,
        selectedEvents: List<com.example.ai.ExtractedEvent>
    ): Long {
        // Find existing discipline or create new one
        var discipline = disciplineDao.getDisciplineByName(syllabus.disciplineName)
        val disciplineId = if (discipline != null) {
            // Update details if missing
            val updated = discipline.copy(
                professor = if (discipline.professor.isBlank()) syllabus.professor else discipline.professor,
                classroom = if (discipline.classroom.isBlank()) syllabus.classroom else discipline.classroom,
                schedule = if (discipline.schedule.isBlank()) syllabus.schedule else discipline.schedule,
                term = syllabus.term
            )
            disciplineDao.updateDiscipline(updated)
            discipline.id
        } else {
            disciplineDao.insertDiscipline(
                Discipline(
                    name = syllabus.disciplineName,
                    code = syllabus.disciplineCode,
                    professor = syllabus.professor,
                    classroom = syllabus.classroom,
                    schedule = syllabus.schedule,
                    term = syllabus.term,
                    colorHex = syllabus.recommendedColorHex,
                    syllabusNotes = "Importado automaticamente via análise inteligente de documento."
                )
            )
        }

        // Insert document record
        val docId = documentDao.insertDocument(
            AcademicDocument(
                title = syllabus.documentTitle,
                fileName = "${syllabus.disciplineName.replace(" ", "_")}_2026.pdf",
                disciplineId = disciplineId,
                parsedSummary = "${selectedEvents.size} eventos e compromissos importados",
                itemsExtractedCount = selectedEvents.size
            )
        )

        // Insert activities
        val activitiesToInsert = selectedEvents.map { event ->
            AcademicActivity(
                disciplineId = disciplineId,
                title = event.title,
                description = event.description,
                activityType = event.activityType,
                date = event.date,
                time = event.time,
                dueDate = event.dueDate ?: if (event.activityType == ActivityType.TRABALHO || event.activityType == ActivityType.PROJETO) event.date else null,
                priority = event.priority,
                status = ActivityStatus.PENDENTE,
                sourceDocument = syllabus.documentTitle,
                notes = "Identificado automaticamente do plano de ensino."
            )
        }

        activityDao.insertActivities(activitiesToInsert)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return disciplineId
    }

    suspend fun insertDiscipline(discipline: Discipline): Long {
        val id = disciplineDao.insertDiscipline(discipline)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    suspend fun insertDisciplineWithClasses(discipline: Discipline, classes: List<AcademicActivity>): Long {
        val id = if (discipline.id == 0L) {
            disciplineDao.insertDiscipline(discipline)
        } else {
            disciplineDao.updateDiscipline(discipline)
            discipline.id
        }
        if (classes.isNotEmpty()) {
            val mapped = classes.map { it.copy(disciplineId = id) }
            activityDao.insertActivities(mapped)
        }
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    suspend fun insertActivities(activities: List<AcademicActivity>): List<Long> {
        val ids = activityDao.insertActivities(activities)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return ids
    }

    suspend fun updateDiscipline(discipline: Discipline) {
        disciplineDao.updateDiscipline(discipline)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun deleteDiscipline(disciplineId: Long) {
        disciplineDao.deleteDisciplineById(disciplineId)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun insertActivity(activity: AcademicActivity): Long {
        val id = activityDao.insertActivity(activity)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
        return id
    }

    suspend fun updateActivity(activity: AcademicActivity) {
        activityDao.updateActivity(activity)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun updateActivityStatus(id: Long, status: ActivityStatus) {
        activityDao.updateActivityStatus(id, status)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun deleteActivity(id: Long) {
        activityDao.deleteActivityById(id)
        AcademicDayAppWidgetProvider.updateAllWidgets(context)
    }

    fun getActivitiesByDate(date: String): Flow<List<AcademicActivity>> = activityDao.getActivitiesByDate(date)

    fun getActivitiesByDiscipline(disciplineId: Long): Flow<List<AcademicActivity>> = activityDao.getActivitiesByDiscipline(disciplineId)

    fun getDocumentsByDiscipline(disciplineId: Long): Flow<List<AcademicDocument>> = documentDao.getDocumentsByDiscipline(disciplineId)

    suspend fun deleteDocument(id: Long) {
        documentDao.deleteDocumentById(id)
    }
}
