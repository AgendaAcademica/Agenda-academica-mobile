package com.example.ui.screens

import com.example.ai.ExtractedEvent
import com.example.data.model.ActivityType
import com.example.data.model.Discipline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentImportScheduleTest {
    @Test
    fun `exibe data importada no padrão brasileiro`() {
        assertEquals("11/08/26", formatImportDate("2026-08-11"))
    }

    @Test
    fun `converte data brasileira para armazenamento ISO`() {
        assertEquals("2026-08-11", parseImportDate("11/08/26"))
        assertEquals(null, parseImportDate("31/02/26"))
    }

    @Test
    fun `adiciona separadores durante a digitação da data`() {
        assertEquals("11/08/26", formatBrazilianDateInput("110826"))
    }

    @Test
    fun `infere terca e quinta pelas datas das aulas`() {
        val events = listOf(
            ExtractedEvent("Aula 1", "2026-08-11", activityType = ActivityType.AULA),
            ExtractedEvent("Aula 2", "2026-08-13", activityType = ActivityType.AULA),
            ExtractedEvent("Prova", "2026-08-14", activityType = ActivityType.PROVA)
        )

        assertEquals("Terça e Quinta", inferScheduleDaysFromClasses("", events))
    }

    @Test
    fun `preserva horario identificado ao completar os dias`() {
        val events = listOf(
            ExtractedEvent("Aula", "2026-08-11", activityType = ActivityType.AULA)
        )

        val schedule = inferScheduleDaysFromClasses("14:00 - 16:00", events)
        assertTrue(schedule.contains("Terça"))
        assertTrue(schedule.contains("14:00 - 16:00"))
    }

    @Test
    fun `remove dia do cabecalho sem aula datada no cronograma`() {
        val events = listOf(
            ExtractedEvent("Aula 1", "2026-08-19", activityType = ActivityType.AULA),
            ExtractedEvent("Aula 2", "2026-08-21", activityType = ActivityType.AULA),
            ExtractedEvent("Reposição", "2026-09-26", activityType = ActivityType.AULA)
        )

        assertEquals(
            "Quarta e Sexta e Sábado, 10:00 - 12:00",
            inferScheduleDaysFromClasses(
                "Quarta-feira - 10:00/12:00, Sexta-feira - 10:00/12:00, Segunda-feira - 08:00/10:00",
                events
            )
        )
    }

    @Test
    fun `ignora provas ao inferir dias de aula`() {
        val events = listOf(
            ExtractedEvent("Prova", "2026-08-14", activityType = ActivityType.PROVA)
        )

        assertEquals("", inferScheduleDaysFromClasses("", events))
    }

    @Test
    fun `preserva horario e sala existentes quando plano nao informa`() {
        val extracted = com.example.ai.ExtractedSyllabus(
            documentTitle = "Plano",
            disciplineName = "Programação Web",
            disciplineCode = "ITI345",
            professor = "Professor",
            term = "2026/2",
            classroom = "",
            schedule = "",
            recommendedColorHex = "#2563EB",
            events = emptyList()
        )
        val existing = Discipline(
            id = 1,
            name = "Programação Web",
            code = "ITI345",
            professor = "Professor",
            classroom = "Lab 309",
            schedule = "Terça e Quinta, 16:00 - 18:00",
            term = "2026/2"
        )

        val merged = mergeExistingDisciplineData(extracted, existing)

        assertEquals("Lab 309", merged.classroom)
        assertEquals("Terça e Quinta, 16:00 - 18:00", merged.schedule)
    }
}
