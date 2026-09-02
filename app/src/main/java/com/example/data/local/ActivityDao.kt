package com.example.data.local

import androidx.room.*
import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityStatus
import com.example.data.model.ActivityType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY date ASC, time ASC")
    fun getAllActivities(): Flow<List<AcademicActivity>>

    @Query("SELECT * FROM activities WHERE date = :date ORDER BY time ASC")
    fun getActivitiesByDate(date: String): Flow<List<AcademicActivity>>

    @Query("SELECT * FROM activities WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, time ASC")
    fun getActivitiesInRange(startDate: String, endDate: String): Flow<List<AcademicActivity>>

    @Query("SELECT * FROM activities WHERE disciplineId = :disciplineId ORDER BY date ASC, time ASC")
    fun getActivitiesByDiscipline(disciplineId: Long): Flow<List<AcademicActivity>>

    @Query("SELECT * FROM activities WHERE activityType = :type ORDER BY date ASC, time ASC")
    fun getActivitiesByType(type: ActivityType): Flow<List<AcademicActivity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): AcademicActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: AcademicActivity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<AcademicActivity>): List<Long>

    @Update
    suspend fun updateActivity(activity: AcademicActivity)

    @Query("UPDATE activities SET status = :status WHERE id = :id")
    suspend fun updateActivityStatus(id: Long, status: ActivityStatus)

    @Delete
    suspend fun deleteActivity(activity: AcademicActivity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("SELECT COUNT(*) FROM activities WHERE disciplineId = :disciplineId")
    suspend fun countByDiscipline(disciplineId: Long): Int

    @Query("SELECT COUNT(*) FROM activities WHERE disciplineId = :disciplineId AND status = 'CONCLUIDA'")
    suspend fun countCompletedByDiscipline(disciplineId: Long): Int
}
