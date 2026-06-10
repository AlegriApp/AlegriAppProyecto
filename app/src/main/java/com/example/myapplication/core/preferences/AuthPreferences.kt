package com.example.myapplication.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val session: Flow<AuthSession?> = dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val fullName = prefs[KEY_FULL_NAME].orEmpty()
        val email = prefs[KEY_EMAIL].orEmpty()
        val roleName = prefs[KEY_ROLE_NAME]

        if (fullName.isBlank() || email.isBlank()) {
            null
        } else {
            AuthSession(
                userId = userId,
                fullName = fullName,
                email = email,
                roleName = roleName
            )
        }
    }

    suspend fun saveSession(session: AuthSession) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_FULL_NAME] = session.fullName
            prefs[KEY_EMAIL] = session.email
            session.roleName?.takeIf { it.isNotBlank() }?.let {
                prefs[KEY_ROLE_NAME] = it
            } ?: prefs.remove(KEY_ROLE_NAME)
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_FULL_NAME)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_ROLE_NAME)
        }
    }

    companion object {
        private const val DATASTORE_NAME = "alegriapp_auth_prefs"
        private val KEY_USER_ID = longPreferencesKey("auth_user_id")
        private val KEY_FULL_NAME = stringPreferencesKey("auth_full_name")
        private val KEY_EMAIL = stringPreferencesKey("auth_email")
        private val KEY_ROLE_NAME = stringPreferencesKey("auth_role_name")

        private val Context.dataStore: androidx.datastore.core.DataStore<Preferences>
                by preferencesDataStore(name = DATASTORE_NAME)
    }
}
