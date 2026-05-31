package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // ---------- Lectura para UI (filtra eliminados lógicos) ----------

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE fecha = :date AND is_deleted = 0 " +
            "ORDER BY estudiante_id ASC"
    )
    fun observeAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE fecha = :date AND curso_id = :courseId AND materia_id = :subjectId " +
            "AND is_deleted = 0 ORDER BY estudiante_id ASC"
    )
    fun observeAttendanceByDateCourseSubject(
        date: String,
        courseId: Long,
        subjectId: Long
    ): Flow<List<AttendanceEntity>>

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE estudiante_id = :studentId AND fecha = :date " +
            "AND curso_id = :courseId AND materia_id = :subjectId AND is_deleted = 0 " +
            "LIMIT 1"
    )
    suspend fun getByStudentDateCourseSubject(
        studentId: Long,
        date: String,
        courseId: Long,
        subjectId: Long
    ): AttendanceEntity?

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE estudiante_id = :studentId AND fecha = :date AND is_deleted = 0 " +
            "LIMIT 1"
    )
    suspend fun getByStudentAndDate(studentId: Long, date: String): AttendanceEntity?

    @Query("SELECT * FROM asistencias WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): AttendanceEntity?

    // ---------- Escritura ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAttendanceList(attendanceList: List<AttendanceEntity>)

    // ---------- Cola de sincronización (estados oficiales del cronograma) ----------

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    suspend fun getPendingSync(): List<AttendanceEntity>

    @Query(
        "SELECT * FROM asistencias " +
            "WHERE is_deleted = 1 AND sync_status != 'SUCCESS'"
    )
    suspend fun getPendingDeletes(): List<AttendanceEntity>

    @Query(
        "UPDATE asistencias SET sync_status = 'SENDING', last_sync_attempt = :now " +
            "WHERE uuid = :uuid"
    )
    suspend fun markAsSending(uuid: String, now: Long)

    @Query(
        "UPDATE asistencias SET sync_status = 'SUCCESS', sync_error = NULL, " +
            "remote_id = :remoteId, server_updated_at = :serverTs, " +
            "sincronizacion_pendiente = 0 " +
            "WHERE uuid = :uuid"
    )
    suspend fun markAsSynced(uuid: String, remoteId: Long?, serverTs: Long?)

    @Query(
        "UPDATE asistencias SET sync_status = 'ERROR', sync_error = :error, " +
            "last_sync_attempt = :now WHERE uuid = :uuid"
    )
    suspend fun markAsFailed(uuid: String, error: String, now: Long)

    @Query(
        "UPDATE asistencias SET is_deleted = 1, sync_status = 'IDLE' " +
            "WHERE uuid = :uuid"
    )
    suspend fun softDelete(uuid: String)

    // ---------- Compat (queda durante la transición) ----------

    @Query("SELECT * FROM asistencias WHERE sincronizacion_pendiente = 1 AND is_deleted = 0")
    suspend fun getPendingSyncAttendance(): List<AttendanceEntity>

    @Query("UPDATE asistencias SET sincronizacion_pendiente = 0, sync_status = 'SUCCESS' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    // ---------- Métricas ----------

    @Query(
        "SELECT COUNT(*) FROM asistencias " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    fun observePendingCount(): Flow<Int>
}
