package com.example.myapplication.data.repository

import com.example.myapplication.core.common.ResultState
import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.remote.dto.UsuarioRemoteDto
import com.example.myapplication.domain.model.AuthSession
import com.example.myapplication.domain.repository.AuthRepository
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val supabaseApi: SupabaseApiService?,
    private val authPreferences: AuthPreferences
) : AuthRepository {

    override fun observeSession(): Flow<AuthSession?> = authPreferences.session

    override suspend fun login(email: String, password: String): ResultState<AuthSession> {
        val normalizedEmail = email.trim().lowercase()
        val normalizedPassword = password.trim()

        if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
            return ResultState.Error("Completa tu correo y tu contrasena.")
        }

        val api = supabaseApi ?: return loginWithLocalFallback(
            email = normalizedEmail,
            password = normalizedPassword
        )

        return runCatching {
            api.getUsuariosByEmail(emailFilter = "eq.$normalizedEmail")
                .firstOrNull()
        }.fold(
            onSuccess = { user ->
                when {
                    user == null -> ResultState.Error("Correo o contrasena incorrectos.")
                    !user.estado.equals("activo", ignoreCase = true) ->
                        ResultState.Error("Tu cuenta no esta habilitada para ingresar.")
                    !PasswordVerifier.matches(
                        plainText = normalizedPassword,
                        storedHash = user.passwordHash
                    ) -> ResultState.Error("Correo o contrasena incorrectos.")
                    else -> {
                        val session = user.toSession()
                        authPreferences.saveSession(session)
                        ResultState.Success(session)
                    }
                }
            },
            onFailure = { error ->
                ResultState.Error(
                    message = error.toLoginMessage(),
                    cause = error
                )
            }
        )
    }

    override suspend fun logout() {
        authPreferences.clearSession()
    }

    private suspend fun loginWithLocalFallback(
        email: String,
        password: String
    ): ResultState<AuthSession> {
        if (email == DEMO_EMAIL && password == DEMO_PASSWORD) {
            val session = AuthSession(
                userId = DEMO_USER_ID,
                fullName = DEMO_FULL_NAME,
                email = DEMO_EMAIL,
                roleName = DEMO_ROLE
            )
            authPreferences.saveSession(session)
            return ResultState.Success(session)
        }

        return ResultState.Error(
            "No se pudo iniciar sesion porque falta configurar Supabase. " +
                "Agrega SUPABASE_KEY en local.properties y recompila la app."
        )
    }

    private fun UsuarioRemoteDto.toSession(): AuthSession = AuthSession(
        userId = id,
        fullName = listOf(nombre, apellido)
            .joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .trim(),
        email = email,
        roleName = roles?.nombre
    )

    private fun Throwable.toLoginMessage(): String = when (this) {
        is IOException -> "No se pudo conectar. Revisa tu internet e intentalo de nuevo."
        else -> message?.takeIf { it.isNotBlank() }
            ?: "No pudimos iniciar sesion. Intentalo nuevamente."
    }

    private companion object {
        const val DEMO_USER_ID = -1L
        const val DEMO_EMAIL = "docente@alegriapp.com"
        const val DEMO_PASSWORD = "1234"
        const val DEMO_FULL_NAME = "Kelvin Docente"
        const val DEMO_ROLE = "Docente"
    }
}

private object PasswordVerifier {
    fun matches(plainText: String, storedHash: String): Boolean {
        val normalizedHash = storedHash.trim()
        if (normalizedHash.isBlank()) return false

        if (plainText == normalizedHash) return true

        return listOf(
            hash("MD5", plainText),
            hash("SHA-256", plainText),
            hash("SHA-512", plainText)
        ).any { candidate ->
            candidate.equals(normalizedHash, ignoreCase = true)
        }
    }

    private fun hash(algorithm: String, plainText: String): String {
        val digest = MessageDigest.getInstance(algorithm)
            .digest(plainText.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
