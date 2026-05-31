package com.example.myapplication.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.domain.model.sync.SyncOutcome

/**
 * Worker que ejecuta una pasada de sincronización completa
 * (PULL estudiantes, PULL incidentes, PUSH pendientes).
 *
 * Política de reintentos: el worker pide [Result.retry] hasta [MAX_ATTEMPTS]
 * para errores recuperables. WorkManager aplica backoff exponencial configurado
 * desde [SyncScheduler].
 *
 * Persiste metadatos (último sync exitoso, último error) vía [SyncPreferences].
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val syncRepository = AppModule.provideSyncRepository(ctx)
        val prefs = AppModule.provideSyncPreferences(ctx)

        return when (val outcome = syncRepository.syncAll()) {
            is SyncOutcome.Success -> {
                prefs.setLastSuccessfulSync(System.currentTimeMillis())
                prefs.setLastError(null)
                Result.success()
            }
            is SyncOutcome.Skipped -> {
                // Sin red, sin Supabase configurado, etc. No es error fatal.
                Result.success()
            }
            is SyncOutcome.Failure -> {
                prefs.setLastError(outcome.message)
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME_NAME = "alegriapp_sync_one_time"
        const val UNIQUE_PERIODIC_NAME = "alegriapp_sync_periodic"
        const val MAX_ATTEMPTS = 5
    }
}
