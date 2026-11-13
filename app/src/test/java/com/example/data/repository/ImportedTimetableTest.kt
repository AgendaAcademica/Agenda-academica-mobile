package com.example.data.repository

import com.example.ai.ExtractedSyllabus
import com.example.data.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedTimetableTest {
    @Test
    fun `uses academic semester end date`() {
        assertEquals("2026-06-30", academicTermEndDate("2026/1º Semestre", "2026-03-10"))
        assertEquals("2026-12-15", academicTermEndDate("2026/2º Semestre", "2026-09-08"))
    }

    @Test
    fun `generates only weekly classes from import date through term end`() {
        val classes = generateTimetableEvents(
            ExtractedSyllabus(
                documentTitle = "Quadro de horário",
                disciplineName = "Teste de Software",
                disciplineCode = "ITI330",
                term = "2026/2º Semestre",
                schedule = "Terça e Quinta, 10:00 - 12:00"
            ),
            createdDateIso = "2026-09-08"
        )

        assertTrue(classes.isNotEmpty())
        assertEquals("2026-09-08", classes.first().date)
        assertTrue(classes.last().date <= "2026-12-15")
        assertTrue(classes.all { it.activityType == ActivityType.AULA && it.time == "10:00" })
        assertTrue(classes.all { it.description.contains("sem base de plano acadêmico") })
    }

    @Test
    fun `normalizes discipline names for matching imported plans`() {
        assertEquals(
            normalizeDisciplineIdentity("GERÊNCIA DE PROJETOS DE SOFTWARE"),
            normalizeDisciplineIdentity("Gerencia de Projetos de Software")
        )
    }
}
