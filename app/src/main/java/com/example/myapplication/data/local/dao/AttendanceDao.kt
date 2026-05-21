package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM asistencias WHERE fecha = :date ORDER BY estudiante_id ASC")
    fun observeAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAttendanceList(attendanceList: List<AttendanceEntity>)

    @Query("SELECT * FROM asistencias WHERE sincronizacion_pendiente = 1")
    suspend fun getPendingSyncAttendance(): List<AttendanceEntity>

    @Query("UPDATE asistencias SET sincronizacion_pendiente = 0 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
}
