package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.sync.SyncState

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["is_deleted"])
    ]
)
data class StudentEntity(
    @PrimaryKey val id: Long,
    val fullName: String,
    val grade: String,
    val section: String,
    val representativeName: String,
    val telegramChatId: String?,
    @ColumnInfo(name = "representative_id") val representativeId: Long? = null,

    // ----- Offline First v6 -----
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncState.Stored.SUCCESS,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
