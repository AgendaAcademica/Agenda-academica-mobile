package com.example.data.local

import androidx.room.*
import com.example.data.model.Discipline
import kotlinx.coroutines.flow.Flow

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM disciplines ORDER BY name ASC")
    fun getAllDisciplines(): Flow<List<Discipline>>

    @Query("SELECT * FROM disciplines WHERE id = :id")
    suspend fun getDisciplineById(id: Long): Discipline?

    @Query("SELECT * FROM disciplines WHERE name = :name LIMIT 1")
    suspend fun getDisciplineByName(name: String): Discipline?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscipline(discipline: Discipline): Long

    @Update
    suspend fun updateDiscipline(discipline: Discipline)

    @Delete
    suspend fun deleteDiscipline(discipline: Discipline)

    @Query("DELETE FROM disciplines WHERE id = :id")
    suspend fun deleteDisciplineById(id: Long)
}
