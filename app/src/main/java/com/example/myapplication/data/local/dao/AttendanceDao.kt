package com.example.myapplication.data.local.dao

import com.example.myapplication.data.local.entity.AttendanceEntity

interface AttendanceDao {
    suspend fun upsertAll(records: List<AttendanceEntity>)
    suspend fun upsert(record: AttendanceEntity)
    suspend fun getAttendanceByDate(date: String): List<AttendanceEntity>
    suspend fun getAttendanceByDateAndSection(date: String, gradeSection: String): List<AttendanceEntity>
    suspend fun getPendingSync(): List<AttendanceEntity>
    suspend fun markAsSynced(ids: List<Long>)
}
