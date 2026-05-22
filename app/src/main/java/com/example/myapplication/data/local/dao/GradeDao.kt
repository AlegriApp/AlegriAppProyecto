package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Query("SELECT * FROM calificaciones WHERE estudiante_id = :studentId ORDER BY id DESC")
    fun observeGradesByStudent(studentId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM calificaciones WHERE materia_nombre = :subject ORDER BY id DESC")
    fun observeGradesBySubjectName(subject: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM calificaciones WHERE periodo_nombre = :period ORDER BY id DESC")
    fun observeGradesByPeriodName(period: String): Flow<List<GradeEntity>>

    @Query(
        "SELECT * FROM calificaciones " +
            "WHERE materia_nombre = :subject AND periodo_nombre = :period ORDER BY id DESC"
    )
    fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<GradeEntity>>

    @Query(
        "SELECT * FROM calificaciones " +
            "WHERE estudiante_id = :studentId AND materia_nombre = :subject " +
            "AND periodo_nombre = :period AND descripcion = :description LIMIT 1"
    )
    suspend fun getByStudentSubjectPeriodAndDescription(
        studentId: Long,
        subject: String,
        period: String,
        description: String
    ): GradeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrade(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrades(grades: List<GradeEntity>)

    @Query("SELECT * FROM calificaciones WHERE sincronizacion_pendiente = 1")
    suspend fun getPendingSyncGrades(): List<GradeEntity>

    @Query("UPDATE calificaciones SET sincronizacion_pendiente = 0 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM calificaciones")
    suspend fun countGrades(): Int
}
