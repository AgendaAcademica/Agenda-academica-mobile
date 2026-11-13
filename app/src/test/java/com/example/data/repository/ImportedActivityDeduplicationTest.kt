package com.example.data.repository

import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportedActivityDeduplicationTest {
    private val exam = AcademicActivity(disciplineId = 1, title = "Avaliação 1", date = "2026-09-11")

    @Test fun repeatedImportPreservesCompletedActivity() {
        val saved = exam.copy(id = 4, status = ActivityStatus.CONCLUIDA, grade = 9.0)
        assertTrue(uniqueImportedActivities(listOf(saved), listOf(exam.copy(title = " AVALIACAO 1 "))).isEmpty())
    }

    @Test fun removesDuplicatesWithinSameImport() {
        assertEquals(listOf(exam), uniqueImportedActivities(emptyList(), listOf(exam, exam)))
    }

    @Test fun keepsDifferentEvents() {
        val other = listOf(exam.copy(date = "2026-09-12"), exam.copy(disciplineId = 2), exam.copy(title = "Avaliação 2"))
        assertEquals(other, uniqueImportedActivities(listOf(exam), other))
    }

    @Test fun timetableNumberChangesDoNotDuplicateSameClass() {
        val lesson = exam.copy(activityType = ActivityType.AULA, title = "Aula #1")
        assertTrue(uniqueImportedActivities(listOf(lesson), listOf(lesson.copy(title = "Aula #2"))).isEmpty())
    }

    @Test fun rescheduledActivityIsNotRecreatedOnOriginalDate() {
        assertTrue(uniqueImportedActivities(listOf(exam.copy(date = "2026-09-20", originalDate = exam.date)), listOf(exam)).isEmpty())
    }
}
