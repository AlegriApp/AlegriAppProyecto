package com.example.myapplication.data.repository

import com.example.myapplication.core.common.ResultState
import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.remote.dto.UsuarioRemoteDto
import com.example.myapplication.domain.model.AuthSession
import com.example.myapplication.domain.repository.AuthRepository
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
            return ResultState.Error("Completa tu correo y tu contraseña.")
        }

        val api = supabaseApi ?: return ResultState.Error(
            "Supabase no está configurado. Revisa SUPABASE_URL y SUPABASE_KEY."
        )

        return runCatching {
            api.getUsuariosByEmail(emailFilter = "eq.$normalizedEmail")
                .firstOrNull()
        }.fold(
            onSuccess = { user ->
                when {
                    user == null -> ResultState.Error("Credenciales inválidas.")
                    !user.estado.equals("activo", ignoreCase = true) ->
                        ResultState.Error("Este usuario no está activo.")
                    !PasswordVerifier.matches(
                        plainText = normalizedPassword,
                        storedHash = user.passwordHash
                    ) -> ResultState.Error("Credenciales inválidas.")
                    else -> {
                        val session = user.toSession()
                        authPreferences.saveSession(session)
                        ResultState.Success(session)
                    }
                }
            },
            onFailure = { error ->
                ResultState.Error(
                    message = error.message ?: "No se pudo iniciar sesión.",
                    cause = error
                )
            }
        )
    }

    override suspend fun logout() {
        authPreferences.clearSession()
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
