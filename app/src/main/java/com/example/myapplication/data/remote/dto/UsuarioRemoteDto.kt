package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UsuarioRemoteDto(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val email: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("rol_id") val rolId: Long,
    val estado: String? = null,
    @SerializedName("ultimo_acceso") val ultimoAcceso: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    val roles: RolRemoteDto? = null
)

data class RolRemoteDto(
    val nombre: String? = null
)
