package com.example.myapplication.domain.model.sync

sealed class SyncOutcome {
    data class Success(val message: String) : SyncOutcome()
    data class Skipped(val reason: String) : SyncOutcome()
    data class Failure(val message: String) : SyncOutcome()
}
