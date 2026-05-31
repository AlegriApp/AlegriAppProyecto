package com.example.myapplication.domain.model.sync

/**
 * Estados oficiales de sincronización por entidad, alineados al cronograma
 * del proyecto (CRONOGRAMA_PROYECTO_FINAL → sealed class "Estados de acción").
 *
 * Se persiste en Room como TEXT en la columna `sync_status` usando los
 * literales definidos en [Stored].
 *
 * Reglas de transición:
 *   IDLE    → SENDING    (al iniciar push)
 *   SENDING → SUCCESS    (push exitoso)
 *   SENDING → ERROR      (push falló)
 *   ERROR   → SENDING    (reintento manual o automático)
 *   SUCCESS → IDLE       (al editar localmente otra vez)
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Sending : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()

    /** Literal persistido en Room. */
    fun storedValue(): String = when (this) {
        Idle -> Stored.IDLE
        Sending -> Stored.SENDING
        Success -> Stored.SUCCESS
        is Error -> Stored.ERROR
    }

    companion object {
        /** Reconstruye un [SyncState] desde el TEXT guardado en Room. */
        fun fromStored(value: String?, error: String? = null): SyncState = when (value) {
            Stored.SENDING -> Sending
            Stored.SUCCESS -> Success
            Stored.ERROR -> Error(error.orEmpty())
            else -> Idle
        }
    }

    /** Literales canónicos persistidos. */
    object Stored {
        const val IDLE = "IDLE"
        const val SENDING = "SENDING"
        const val SUCCESS = "SUCCESS"
        const val ERROR = "ERROR"
    }
}
