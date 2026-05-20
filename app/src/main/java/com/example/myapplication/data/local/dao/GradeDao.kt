package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY id DESC")
    fun observeGradesByStudent(studentId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subject = :subject ORDER BY id DESC")
    fun observeGradesBySubject(subject: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE period = :period ORDER BY id DESC")
    fun observeGradesByPeriod(period: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subject = :subject AND period = :period ORDER BY id DESC")
    fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrade(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrades(grades: List<GradeEntity>)

    @Query("SELECT * FROM grades WHERE synced = 0")
    suspend fun getPendingSyncGrades(): List<GradeEntity>

    @Query("UPDATE grades SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
