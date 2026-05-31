package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    // ---------- Lectura para UI (filtra eliminados lógicos) ----------

    @Query("SELECT * FROM calificaciones WHERE estudiante_id = :studentId AND is_deleted = 0 ORDER BY id DESC")
    fun observeGradesByStudent(studentId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM calificaciones WHERE materia_nombre = :subject AND is_deleted = 0 ORDER BY id DESC")
    fun observeGradesBySubjectName(subject: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM calificaciones WHERE periodo_nombre = :period AND is_deleted = 0 ORDER BY id DESC")
    fun observeGradesByPeriodName(period: String): Flow<List<GradeEntity>>

    @Query(
        "SELECT * FROM calificaciones " +
            "WHERE materia_nombre = :subject AND periodo_nombre = :period AND is_deleted = 0 " +
            "ORDER BY id DESC"
    )
    fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<GradeEntity>>

    @Query(
        "SELECT * FROM calificaciones " +
            "WHERE estudiante_id = :studentId AND materia_nombre = :subject " +
            "AND periodo_nombre = :period AND descripcion = :description AND is_deleted = 0 " +
            "LIMIT 1"
    )
    suspend fun getByStudentSubjectPeriodAndDescription(
        studentId: Long,
        subject: String,
        period: String,
        description: String
    ): GradeEntity?

    @Query("SELECT * FROM calificaciones WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): GradeEntity?

    // ---------- Escritura ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrade(grade: GradeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceGrades(grades: List<GradeEntity>)

    // ---------- Cola de sincronización ----------

    @Query(
        "SELECT * FROM calificaciones " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    suspend fun getPendingSync(): List<GradeEntity>

    @Query("SELECT * FROM calificaciones WHERE is_deleted = 1 AND sync_status != 'SUCCESS'")
    suspend fun getPendingDeletes(): List<GradeEntity>

    @Query("UPDATE calificaciones SET sync_status = 'SENDING', last_sync_attempt = :now WHERE uuid = :uuid")
    suspend fun markAsSending(uuid: String, now: Long)

    @Query(
        "UPDATE calificaciones SET sync_status = 'SUCCESS', sync_error = NULL, " +
            "remote_id = :remoteId, server_updated_at = :serverTs, " +
            "sincronizacion_pendiente = 0 " +
            "WHERE uuid = :uuid"
    )
    suspend fun markAsSynced(uuid: String, remoteId: Long?, serverTs: Long?)

    @Query("UPDATE calificaciones SET sync_status = 'ERROR', sync_error = :error, last_sync_attempt = :now WHERE uuid = :uuid")
    suspend fun markAsFailed(uuid: String, error: String, now: Long)

    @Query("UPDATE calificaciones SET is_deleted = 1, sync_status = 'IDLE' WHERE uuid = :uuid")
    suspend fun softDelete(uuid: String)

    // ---------- Compat ----------

    @Query("SELECT * FROM calificaciones WHERE sincronizacion_pendiente = 1 AND is_deleted = 0")
    suspend fun getPendingSyncGrades(): List<GradeEntity>

    @Query("UPDATE calificaciones SET sincronizacion_pendiente = 0, sync_status = 'SUCCESS' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM calificaciones WHERE is_deleted = 0")
    suspend fun countGrades(): Int

    // ---------- Métricas ----------

    @Query(
        "SELECT COUNT(*) FROM calificaciones " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    fun observePendingCount(): Flow<Int>
}
