package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.sync.SyncOutcome

interface SyncRepository {
    suspend fun syncAll(): SyncOutcome
    suspend fun syncStudentsFromRemote(): SyncOutcome
    suspend fun syncPendingRecords(): SyncOutcome
}
