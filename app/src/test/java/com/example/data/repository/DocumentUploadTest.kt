package com.example.data.repository

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.remote.AcademicApi
import com.example.data.remote.ExtractDocumentMetaDto
import com.example.data.remote.ExtractDocumentResponse
import com.example.data.remote.ExtractedDocumentDto
import com.example.data.remote.ExtractedSyllabusDto
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DocumentUploadTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: AcademicRepository
    private val requests = mutableListOf<List<MultipartBody.Part>>()

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = AcademicRepository(context, database, object : AcademicApi {
            override suspend fun extractDocuments(files: List<MultipartBody.Part>): ExtractDocumentResponse {
                requests += files
                return ExtractDocumentResponse(
                    data = ExtractedDocumentDto(listOf(ExtractedSyllabusDto(
                        documentTitle = "Plano ${requests.size}", disciplineName = "Disciplina ${requests.size}"
                    ))),
                    meta = ExtractDocumentMetaDto("plano", 10, 0)
                )
            }
        })
    }

    @After fun close() { database.close() }

    private fun document(name: String, mimeType: String, bytes: ByteArray): DocumentUpload {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val uri = Uri.parse("content://documents/$name")
        shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(bytes))
        return DocumentUpload(uri, name, mimeType, bytes.size.toLong())
    }

    @Test fun multiplePdfsUseSeparateRequestsAndKeepAllResultsInOrder() = runBlocking {
        val documents = listOf("primeiro.pdf", "segundo.pdf").map {
            document(it, "application/pdf", "%PDF-1.4".toByteArray())
        }
        val results = repository.extractDocuments(documents)

        assertEquals(listOf("Disciplina 1", "Disciplina 2"), results.map { it.disciplineName })
        assertEquals(listOf(1, 1), requests.map { it.size })
        documents.forEachIndexed { index, file ->
            assertEquals("form-data; name=\"file\"; filename=\"${file.fileName}\"",
                requests[index].single().headers?.get("Content-Disposition"))
        }
    }

    @Test fun multipleImagesStillUseOneBatchRequest() = runBlocking {
        val documents = listOf("frente.jpg", "verso.jpg").map {
            document(it, "image/jpeg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        }
        repository.extractDocuments(documents)

        assertEquals(2, requests.single().size)
        assertTrue(requests.single().all { it.headers?.get("Content-Disposition")?.contains("name=\"files\"") == true })
    }

    @Test fun moreThanTenPdfsAreRejectedBeforeSending() = runBlocking {
        val documents = (1..11).map { document("$it.pdf", "application/pdf", "%PDF-1.4".toByteArray()) }
        val error = runCatching { repository.extractDocuments(documents) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Selecione no máximo 10 arquivos.", error?.message)
        assertTrue(requests.isEmpty())
    }
}
