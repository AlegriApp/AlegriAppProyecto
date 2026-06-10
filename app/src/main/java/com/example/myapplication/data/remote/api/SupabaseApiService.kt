package com.example.myapplication.data.remote.api

import com.example.myapplication.core.network.SupabaseConfig
import com.example.myapplication.data.remote.dto.AsistenciaInsertDto
import com.example.myapplication.data.remote.dto.AsistenciaRemoteResponseDto
import com.example.myapplication.data.remote.dto.CalificacionInsertDto
import com.example.myapplication.data.remote.dto.CalificacionRemoteResponseDto
import com.example.myapplication.data.remote.dto.ConfiguracionTelegramRemoteDto
import com.example.myapplication.data.remote.dto.CursoCatalogRemoteDto
import com.example.myapplication.data.remote.dto.EstudianteCursoEnrollmentRemoteDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import com.example.myapplication.data.remote.dto.IncidenteInsertDto
import com.example.myapplication.data.remote.dto.IncidenteRemoteDto
import com.example.myapplication.data.remote.dto.MateriaCatalogRemoteDto
import com.example.myapplication.data.remote.dto.PeriodoAcademicoRemoteDto
import com.example.myapplication.data.remote.dto.TipoEvaluacionRemoteDto
import com.example.myapplication.data.remote.dto.TipoIncidenteRemoteDto
import com.example.myapplication.data.remote.dto.UsuarioRemoteDto
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

    // ---------- AUTH (PULL) ----------

    @GET(SupabaseConfig.USUARIOS_TABLE)
    suspend fun getUsuariosByEmail(
        @Query("select") select: String = SupabaseConfig.USUARIO_SELECT,
        @Query("email") emailFilter: String,
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null",
        @Query("limit") limit: Int = 1
    ): List<UsuarioRemoteDto>

    // ---------- CATÁLOGOS (PULL) ----------

    @GET(SupabaseConfig.CURSOS_TABLE)
    suspend fun getCursosActivos(
        @Query("select") select: String = "id,nombre,paralelo,anio_lectivo,periodo_academico_id,estado",
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<CursoCatalogRemoteDto>

    @GET(SupabaseConfig.MATERIAS_TABLE)
    suspend fun getMateriasActivas(
        @Query("select") select: String = "id,nombre,curso_id,estado",
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<MateriaCatalogRemoteDto>

    @GET(SupabaseConfig.TIPOS_EVALUACION_TABLE)
    suspend fun getTiposEvaluacionActivos(
        @Query("select") select: String = "id,nombre,activo",
        @Query("activo") activoFilter: String = "eq.true"
    ): List<TipoEvaluacionRemoteDto>

    @GET(SupabaseConfig.PERIODOS_ACADEMICOS_TABLE)
    suspend fun getPeriodosActivos(
        @Query("select") select: String = "id,nombre,anio_lectivo,activo",
        @Query("activo") activoFilter: String = "eq.true"
    ): List<PeriodoAcademicoRemoteDto>

    @GET(SupabaseConfig.TIPOS_INCIDENTE_TABLE)
    suspend fun getTiposIncidenteActivos(
        @Query("select") select: String = "id,nombre,activo",
        @Query("activo") activoFilter: String = "eq.true"
    ): List<TipoIncidenteRemoteDto>

    @GET(SupabaseConfig.CONFIGURACION_TELEGRAM_TABLE)
    suspend fun getConfiguracionTelegramActiva(
        @Query("select") select: String = "id,chat_id,token_bot_encriptado,representante_id,estado_integracion,deleted_at",
        @Query("estado_integracion") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<ConfiguracionTelegramRemoteDto>

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

    /** Matrículas activas (fuente fiable para filtrar estudiantes por curso). */
    @GET(SupabaseConfig.ESTUDIANTE_CURSO_TABLE)
    suspend fun getEstudianteCursosActivos(
        @Query("select") select: String = "estudiante_id,curso_id,estado",
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("limit") limit: Int = 2000
    ): List<EstudianteCursoEnrollmentRemoteDto>

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

    // ---------- INCIDENTES (PULL + PUSH desde Fase 14) ----------

    /** PULL de todos los incidentes activos. */
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

    /**
     * PUSH idempotente de incidentes desde mobile.
     * Requiere `supabase_grant_insert_incidentes.sql` aplicado en el servidor.
     */
    @POST(SupabaseConfig.INCIDENTES_TABLE)
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertIncidente(
        @Query("on_conflict") onConflict: String = "uuid",
        @Body body: IncidenteInsertDto
    ): List<IncidenteRemoteDto>
}
