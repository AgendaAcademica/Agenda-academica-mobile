package com.example.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.ai.ExtractedEvent
import com.example.ai.ExtractedSyllabus
import com.example.data.local.AppDatabase
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AcademicRepositoryDataTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AcademicRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        runCatching { WorkManager.initialize(context, Configuration.Builder().build()) }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = AcademicRepository(context, database)
    }

    @After fun close() { database.close() }

    private val syllabus = ExtractedSyllabus(documentTitle = "Plano", disciplineName = "Matemática")
    private val event = ExtractedEvent(title = "Prova 1", date = "2026-09-11", activityType = ActivityType.PROVA)

    @Test fun repeatedPlanPreservesIdsGradesAndStatus() = runBlocking {
        repository.saveImportedSyllabus(syllabus, listOf(event, event))
        val saved = repository.allActivities.first().single().copy(status = ActivityStatus.CONCLUIDA, grade = 8.5)
        database.activityDao().updateActivity(saved)
        repository.saveImportedSyllabus(syllabus, listOf(event))
        assertEquals(saved, repository.allActivities.first().single())
        assertEquals(1, repository.allDisciplines.first().size)
        assertEquals(1, repository.allDocuments.first().size)
    }

    @Test fun clearRemovesLinkedAndStandaloneActivitiesAndDocuments() = runBlocking {
        repository.saveImportedSyllabus(syllabus, listOf(event))
        database.activityDao().insertActivity(AcademicActivity(title = "Pessoal", date = "2026-09-11"))
        repository.clearAcademicData()
        assertTrue(repository.allActivities.first().isEmpty())
        assertTrue(repository.allDisciplines.first().isEmpty())
        assertTrue(repository.allDocuments.first().isEmpty())
        repository.clearAcademicData()
        repository.saveImportedSyllabus(syllabus, listOf(event))
        assertEquals(1, repository.allActivities.first().size)
    }

    @Test fun repeatedFileIsDetectedAndReplacementOnlyClearsItsDiscipline() = runBlocking {
        val id = repository.saveImportedSyllabus(syllabus, listOf(event), fileFingerprint = "same-file")
        database.activityDao().insertActivity(AcademicActivity(disciplineId = id, title = "Manual", date = "2026-09-12"))
        val other = syllabus.copy(disciplineName = "Física")
        repository.saveImportedSyllabus(other, listOf(event), fileFingerprint = "another-file")
        val otherActivities = repository.allActivities.first().filter { it.disciplineId != id }
        assertTrue(repository.isRepeatedImport(syllabus.copy(documentTitle = "Renomeado"), "same-file"))
        assertFalse(repository.isRepeatedImport(syllabus, "different-file"))
        repository.saveImportedSyllabus(syllabus, listOf(event.copy(title = "Nova prova")),
            fileFingerprint = "same-file", replaceExistingActivities = true)
        assertEquals(listOf("Nova prova"), repository.allActivities.first().filter { it.disciplineId == id }.map { it.title })
        assertEquals(otherActivities, repository.allActivities.first().filter { it.disciplineId != id })
    }

    @Test fun recognizesLegacyImportWithoutFingerprint() = runBlocking {
        repository.saveImportedSyllabus(syllabus, listOf(event))
        assertTrue(repository.isRepeatedImport(syllabus, "new-fingerprint"))
    }

    @Test fun fingerprintMigrationPreservesExistingDocument() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE academic_documents (id INTEGER PRIMARY KEY, title TEXT NOT NULL)")
                    db.execSQL("INSERT INTO academic_documents VALUES (1, 'Plano existente')")
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.use {
            val db = it.writableDatabase
            AppDatabase.MIGRATION_3_4.migrate(db)
            db.query("SELECT title, fileFingerprint FROM academic_documents WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Plano existente", cursor.getString(0))
                assertEquals("", cursor.getString(1))
            }
        }
    }
}
