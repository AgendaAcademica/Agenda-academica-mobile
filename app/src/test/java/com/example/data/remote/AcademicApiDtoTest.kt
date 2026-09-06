package com.example.data.remote

import com.example.data.model.ActivityType
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class AcademicApiDtoTest {
    @Test
    fun `maps backend response to mobile domain`() {
        val dto = ExtractedSyllabusDto(
            documentTitle = "Plano de Ensino",
            disciplineName = "Cálculo I",
            disciplineCode = "MAT101",
            events = listOf(
                ExtractedEventDto(
                    title = "Prova 1",
                    date = "2026-09-22",
                    activityType = "PROVA",
                    priority = "URGENTE"
                )
            )
        )
        val result = dto.toDomain()
        assertEquals("Cálculo I", result.disciplineName)
        assertEquals(ActivityType.PROVA, result.events.single().activityType)
        assertEquals(Priority.URGENTE, result.events.single().priority)
    }

    @Test
    fun `uses safe defaults for unknown enum values`() {
        val event = ExtractedEventDto(
            title = "Atividade",
            date = "2026-09-02",
            activityType = "DESCONHECIDO",
            priority = "DESCONHECIDA"
        ).toDomain()
        assertEquals(ActivityType.TRABALHO, event.activityType)
        assertEquals(Priority.MEDIA, event.priority)
    }

    @Test
    fun `applies discipline schedule start time to events without time`() {
        val result = ExtractedSyllabusDto(
            documentTitle = "Cronograma",
            disciplineName = "Programacao Web",
            schedule = "Segunda e quarta, 16:00 - 18:00",
            events = listOf(
                ExtractedEventDto(
                    title = "Apresentacao",
                    date = "2026-09-14",
                    activityType = "TRABALHO",
                    priority = "ALTA"
                )
            )
        ).toDomain()

        assertEquals("16:00", result.events.single().time)
    }

    @Test
    fun `preserves explicit event time instead of discipline schedule`() {
        val result = ExtractedSyllabusDto(
            documentTitle = "Cronograma",
            disciplineName = "Programacao Web",
            schedule = "Segunda e quarta, 16:00 - 18:00",
            events = listOf(
                ExtractedEventDto(
                    title = "Prova noturna",
                    date = "2026-09-14",
                    time = "19:30",
                    activityType = "PROVA",
                    priority = "URGENTE"
                )
            )
        ).toDomain()

        assertEquals("19:30", result.events.single().time)
    }
}
