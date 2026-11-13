package com.example.data.local

import androidx.room.*
import com.example.data.model.AcademicDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("DELETE FROM academic_documents")
    suspend fun deleteAll()

    @Query("SELECT * FROM academic_documents ORDER BY importedAt DESC")
    fun getAllDocuments(): Flow<List<AcademicDocument>>

    @Query("SELECT * FROM academic_documents WHERE disciplineId = :disciplineId ORDER BY importedAt DESC")
    fun getDocumentsByDiscipline(disciplineId: Long): Flow<List<AcademicDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: AcademicDocument): Long

    @Delete
    suspend fun deleteDocument(document: AcademicDocument)

    @Query("DELETE FROM academic_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
