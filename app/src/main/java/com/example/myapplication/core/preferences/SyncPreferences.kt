package com.example.myapplication.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Metadatos de sincronización persistidos con DataStore.
 *
 * - `lastSuccessfulSync`: epoch millis del último `SyncOutcome.Success` de syncAll.
 * - `lastError`: mensaje del último `SyncOutcome.Failure` (se limpia al éxito).
 *
 * UI lo consume vía `lastSuccessfulSync: Flow<Long?>` para mostrar
 * "Última sincronización: hace N minutos".
 */
class SyncPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val lastSuccessfulSync: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SUCCESS]?.takeIf { it > 0L }
    }

    val lastError: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_ERROR]?.takeIf { it.isNotBlank() }
    }

    suspend fun setLastSuccessfulSync(epochMs: Long) {
        dataStore.edit { it[KEY_LAST_SUCCESS] = epochMs }
    }

    suspend fun setLastError(message: String?) {
        dataStore.edit { prefs ->
            if (message == null) {
                prefs.remove(KEY_LAST_ERROR)
            } else {
                prefs[KEY_LAST_ERROR] = message
            }
        }
    }

    companion object {
        private const val DATASTORE_NAME = "alegriapp_sync_prefs"
        private val KEY_LAST_SUCCESS = longPreferencesKey("last_successful_sync_ms")
        private val KEY_LAST_ERROR = stringPreferencesKey("last_sync_error")

        private val Context.dataStore: androidx.datastore.core.DataStore<Preferences>
                by preferencesDataStore(name = DATASTORE_NAME)
    }
}
