package com.example.myapplication.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.work.ForegroundInfo

/**
 * Notificaciones del sistema (fuera de la app) para la sincronización.
 *
 * - Canal único `alegriapp_sync` con prioridad LOW (no hace ruido, no vibra).
 * - Notificación foreground mientras el worker envía incidentes pendientes
 *   por Telegram (progreso "X / Y enviados").
 * - Notificación final con resultado ("3 enviados, 1 con error").
 *
 * En Android 13+ requiere `POST_NOTIFICATIONS` (runtime). Si el usuario no la
 * concedió, las notificaciones se intentan igual y la `SecurityException`
 * resultante se ignora silenciosamente (el sync no se aborta).
 */
object SyncNotifications {

    const val CHANNEL_ID = "alegriapp_sync"
    private const val CHANNEL_NAME = "Sincronización"
    private const val CHANNEL_DESC = "Avisos cuando se envían reportes pendientes al recuperar conexión"

    const val NOTIF_ID_INCIDENTS_SENDING = 1001
    const val NOTIF_ID_INCIDENTS_RESULT = 1002

    /**
     * Crea el canal de notificación si no existe. Idempotente.
     * Llamar al menos una vez antes de notificar (típicamente en Application.onCreate).
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService<NotificationManager>() ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESC
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    /**
     * Devuelve [ForegroundInfo] para mostrar progreso de envío de incidentes.
     * El worker la usa con `setForeground(...)` para volverse foreground service
     * y mostrar una notificación persistente mientras envía.
     */
    fun foregroundInfoSending(context: Context, current: Int, total: Int): ForegroundInfo {
        ensureChannel(context)
        val safeTotal = total.coerceAtLeast(1)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Enviando incidentes pendientes")
            .setContentText("$current / $total enviados")
            .setProgress(safeTotal, current, current == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIF_ID_INCIDENTS_SENDING,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIF_ID_INCIDENTS_SENDING, notif)
        }
    }

    /**
     * Publica la notificación final con el resumen del envío.
     * Tolera ausencia de permiso POST_NOTIFICATIONS (no aborta el sync).
     */
    fun postResult(context: Context, sent: Int, failed: Int) {
        ensureChannel(context)
        val (title, icon) = when {
            failed == 0 && sent > 0 ->
                "Incidentes enviados" to android.R.drawable.stat_sys_upload_done
            failed > 0 && sent > 0 ->
                "Envío parcial de incidentes" to android.R.drawable.stat_notify_error
            else ->
                "No se pudieron enviar incidentes" to android.R.drawable.stat_notify_error
        }
        val text = buildString {
            append("$sent enviado${if (sent == 1) "" else "s"} por Telegram")
            if (failed > 0) append(" • $failed con error")
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIF_ID_INCIDENTS_RESULT, notif)
        } catch (_: SecurityException) {
            // Android 13+ sin POST_NOTIFICATIONS concedido. Silencioso a propósito:
            // el envío ya ocurrió, solo no podemos avisar visualmente.
        }
    }

    /**
     * Cancela la notificación de envío "en progreso". Útil cuando el worker
     * termina y queremos sustituirla por la de resultado final.
     */
    fun cancelInProgress(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID_INCIDENTS_SENDING)
        } catch (_: SecurityException) {
            // Ignorar.
        }
    }
}
