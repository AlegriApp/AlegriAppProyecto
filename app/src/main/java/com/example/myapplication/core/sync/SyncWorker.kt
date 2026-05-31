package com.example.myapplication.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.core.notifications.SyncNotifications
import com.example.myapplication.domain.model.sync.SyncOutcome

/**
 * Worker que ejecuta una pasada de sincronización completa.
 *
 * **Los incidentes salen por DOS canales independientes y ambos se activan
 * al recuperar conexión**:
 *
 *   Canal 1 — Supabase (Fase 14):
 *     - PUSH de incidentes con `sync_status IN ('IDLE','ERROR')`.
 *     - Upsert idempotente por `uuid` vía `SupabaseApiService.upsertIncidente`.
 *     - Se ejecuta dentro de `syncRepository.syncAll()` → `syncPendingRecords()`.
 *     - Al éxito marca `sync_status='SUCCESS'`, `remote_id=<serial>`, `local_only=0`.
 *
 *   Canal 2 — Telegram (Fase 13):
 *     - ENVÍO de incidentes con `enviado=0 AND local_only=1` (incidentes locales
 *       que aún no se han comunicado al chat).
 *     - Se ejecuta vía `SendPendingIncidentsUseCase`.
 *     - Mientras dura, el worker se vuelve foreground service con notificación
 *       del sistema "Enviando incidentes pendientes — X/N". Al terminar publica
 *       una notificación de resultado.
 *     - Al éxito marca `enviado=1`.
 *
 * Las dos columnas (`sync_status` y `enviado`) son **ortogonales**: cada canal
 * tiene su propia cola y su propia marca de éxito. Si Supabase falla pero
 * Telegram funciona (o viceversa), cada cual reintenta en el próximo sync.
 *
 * Pasos del worker:
 *   1. PULL estudiantes desde Supabase.
 *   2. PULL incidentes desde Supabase (LWW por `server_updated_at`).
 *   3. PUSH asistencias + calificaciones + incidentes pendientes (Canal 1).
 *   4. ENVÍO Telegram de incidentes locales pendientes (Canal 2) con
 *      notificación foreground.
 *
 * Política de reintentos: pide [Result.retry] hasta [MAX_ATTEMPTS] ante errores
 * recuperables. WorkManager aplica backoff exponencial configurado desde
 * [SyncScheduler].
 *
 * Persiste metadatos (último sync exitoso, último error) vía
 * [com.example.myapplication.core.preferences.SyncPreferences].
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val syncRepository = AppModule.provideSyncRepository(ctx)
        val prefs = AppModule.provideSyncPreferences(ctx)
        val sendPendingIncidents = AppModule.provideSendPendingIncidentsUseCase(ctx)

        SyncNotifications.ensureChannel(ctx)

        // ---------- 1. Sync Supabase (estudiantes + incidentes + push records) ----------
        val outcome = runCatching { syncRepository.syncAll() }
            .getOrElse { SyncOutcome.Failure(it.message.orEmpty()) }

        // ---------- 2. Envío Telegram de incidentes pendientes ----------
        // Se ejecuta solo si hay algo que enviar. La notificación foreground
        // aparece únicamente cuando hay trabajo real (no molesta en runs vacíos).
        //
        // El setForeground(...) está envuelto en try/catch porque en algunos
        // dispositivos / versiones de Android puede fallar por restricciones de
        // foregroundServiceType (Android 14+). Si falla, caemos a notificación
        // normal (sin foreground) y el envío sigue su curso — la prioridad es
        // que los incidentes lleguen a Telegram, no la animación de la barra.
        val pendingResult = runCatching {
            sendPendingIncidents { current, total ->
                if (total > 0) {
                    runCatching {
                        setForeground(
                            SyncNotifications.foregroundInfoSending(ctx, current, total)
                        )
                    }
                }
            }
        }.getOrElse { error ->
            // Fallo masivo durante el envío. Reportar y no abortar el worker.
            com.example.myapplication.domain.usecase.incidents.SendPendingIncidentsResult(
                total = -1, sent = 0, failed = 0
            ).also {
                SyncNotifications.cancelInProgress(ctx)
                SyncNotifications.postResult(ctx, sent = 0, failed = 1)
                prefs.setLastError("Envío de incidentes pendientes: ${error.message.orEmpty()}")
            }
        }

        if (pendingResult.hasWork) {
            SyncNotifications.cancelInProgress(ctx)
            SyncNotifications.postResult(
                context = ctx,
                sent = pendingResult.sent,
                failed = pendingResult.failed
            )
        }

        // ---------- 3. Resultado final del worker ----------
        return when (outcome) {
            is SyncOutcome.Success -> {
                prefs.setLastSuccessfulSync(System.currentTimeMillis())
                if (pendingResult.failed == 0) prefs.setLastError(null)
                Result.success()
            }
            is SyncOutcome.Skipped -> {
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
