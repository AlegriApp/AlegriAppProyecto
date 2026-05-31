package com.example.myapplication.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Punto único de programación de sincronización.
 *
 * **`enqueueOneTime()`** se llama:
 *   - Al recuperar conexión (Fase 7).
 *   - Tras cada escritura local en repositorios (botón "Sincronizar ahora").
 *   - Al arrancar la app (boot warm-up).
 *
 * **`enqueuePeriodic()`** se llama una vez al iniciar la app y queda corriendo
 * cada 15 minutos como red de seguridad.
 *
 * Ambos usan `UNIQUE` names: si ya hay un sync encolado/corriendo, no se
 * duplica (la política `KEEP` para periódico, `REPLACE` para one-time).
 */
class SyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /**
     * Encola una sincronización inmediata. Si la red está disponible,
     * arranca en segundos; si no, queda esperando a que vuelva.
     */
    fun enqueueOneTime() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Programa la sincronización periódica (15 min). Idempotente: KEEP no
     * reemplaza si ya existe, evitando reiniciar el contador.
     */
    fun enqueuePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SyncWorker.UNIQUE_ONE_TIME_NAME)
        workManager.cancelUniqueWork(SyncWorker.UNIQUE_PERIODIC_NAME)
    }

    private fun networkConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    companion object {
        private const val PERIODIC_MINUTES = 15L
        private const val INITIAL_BACKOFF_SECONDS = 30L
    }
}
