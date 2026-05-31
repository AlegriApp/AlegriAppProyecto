package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE is_deleted = 0 ORDER BY fullName ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query(
        "SELECT s.* FROM students s " +
            "INNER JOIN student_courses sc ON sc.student_id = s.id " +
            "WHERE sc.course_id = :courseId AND s.is_deleted = 0 " +
            "ORDER BY s.fullName ASC"
    )
    fun observeStudentsByCourse(courseId: Long): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceStudents(students: List<StudentEntity>)

    @Query("SELECT * FROM students WHERE id = :studentId AND is_deleted = 0 LIMIT 1")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): StudentEntity?

    @Query("SELECT COUNT(*) FROM students WHERE is_deleted = 0")
    suspend fun countStudents(): Int

    @Query("SELECT * FROM students WHERE is_deleted = 0")
    suspend fun getAllActiveStudents(): List<StudentEntity>

    // ---------- Cola de sincronización ----------

    @Query("SELECT * FROM students WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0")
    suspend fun getPendingSync(): List<StudentEntity>

    @Query("UPDATE students SET sync_status = 'SENDING', last_sync_attempt = :now WHERE uuid = :uuid")
    suspend fun markAsSending(uuid: String, now: Long)

    @Query(
        "UPDATE students SET sync_status = 'SUCCESS', sync_error = NULL, " +
            "remote_id = :remoteId, server_updated_at = :serverTs " +
            "WHERE uuid = :uuid"
    )
    suspend fun markAsSynced(uuid: String, remoteId: Long?, serverTs: Long?)

    @Query("UPDATE students SET sync_status = 'ERROR', sync_error = :error, last_sync_attempt = :now WHERE uuid = :uuid")
    suspend fun markAsFailed(uuid: String, error: String, now: Long)

    @Query(
        "SELECT COUNT(*) FROM students " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    fun observePendingCount(): Flow<Int>
}
