package com.example.myapplication.data.remote.api

import com.example.myapplication.core.network.SupabaseConfig
import com.example.myapplication.data.remote.dto.AsistenciaInsertDto
import com.example.myapplication.data.remote.dto.AsistenciaRemoteResponseDto
import com.example.myapplication.data.remote.dto.CalificacionInsertDto
import com.example.myapplication.data.remote.dto.CalificacionRemoteResponseDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import com.example.myapplication.data.remote.dto.IncidenteRemoteDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API REST de Supabase (PostgREST).
 *
 * **Incidentes son PULL only** (decisión del equipo en Fase 0).
 * No se expone `insertIncidente` intencionalmente. La inserción la realiza
 * un proceso externo a la app móvil.
 */
interface SupabaseApiService {

    // ---------- ESTUDIANTES (PULL) ----------

    @GET(SupabaseConfig.ESTUDIANTES_TABLE)
    suspend fun getEstudiantesActivos(
        @Query("select") select: String = SupabaseConfig.ESTUDIANTE_SELECT,
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<EstudianteRemoteDto>

    @GET(SupabaseConfig.ESTUDIANTES_TABLE)
    suspend fun getEstudiantesUpdatedSince(
        @Query("select") select: String = SupabaseConfig.ESTUDIANTE_SELECT,
        @Query("updated_at") updatedSince: String,
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<EstudianteRemoteDto>

    // ---------- ASISTENCIAS (PUSH + PULL) ----------

    @POST(SupabaseConfig.ASISTENCIAS_TABLE)
    @Headers(
        "Prefer: resolution=merge-duplicates,return=representation"
    )
    suspend fun upsertAsistencia(
        @Query("on_conflict") onConflict: String = "uuid",
        @Body body: AsistenciaInsertDto
    ): List<AsistenciaRemoteResponseDto>

    /** Compat con código viejo; redirige a upsert. */
    @POST(SupabaseConfig.ASISTENCIAS_TABLE)
    @Headers("Prefer: return=representation")
    suspend fun insertAsistencia(
        @Body body: AsistenciaInsertDto
    ): List<AsistenciaRemoteResponseDto>

    // ---------- CALIFICACIONES (PUSH + PULL) ----------

    @POST(SupabaseConfig.CALIFICACIONES_TABLE)
    @Headers(
        "Prefer: resolution=merge-duplicates,return=representation"
    )
    suspend fun upsertCalificacion(
        @Query("on_conflict") onConflict: String = "uuid",
        @Body body: CalificacionInsertDto
    ): List<CalificacionRemoteResponseDto>

    /** Compat con código viejo; redirige a upsert. */
    @POST(SupabaseConfig.CALIFICACIONES_TABLE)
    @Headers("Prefer: return=representation")
    suspend fun insertCalificacion(
        @Body body: CalificacionInsertDto
    ): List<CalificacionRemoteResponseDto>

    // ---------- INCIDENTES (PULL ONLY) ----------

    /**
     * PULL de todos los incidentes activos. NO existe método POST aquí
     * intencionalmente — mobile NO escribe incidentes.
     */
    @GET(SupabaseConfig.INCIDENTES_TABLE)
    suspend fun getIncidentes(
        @Query("select") select: String = SupabaseConfig.INCIDENTE_SELECT,
        @Query("deleted_at") deletedFilter: String = "is.null",
        @Query("order") order: String = "updated_at.desc",
        @Query("limit") limit: Int = 500
    ): List<IncidenteRemoteDto>

    /** PULL incremental: solo trae incidentes modificados después de `updatedSince`. */
    @GET(SupabaseConfig.INCIDENTES_TABLE)
    suspend fun getIncidentesUpdatedSince(
        @Query("select") select: String = SupabaseConfig.INCIDENTE_SELECT,
        @Query("updated_at") updatedSince: String,
        @Query("deleted_at") deletedFilter: String = "is.null",
        @Query("order") order: String = "updated_at.desc"
    ): List<IncidenteRemoteDto>
}
