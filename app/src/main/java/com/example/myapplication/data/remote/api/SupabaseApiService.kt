package com.example.myapplication.data.remote.api

import com.example.myapplication.core.network.SupabaseConfig
import com.example.myapplication.data.remote.dto.AsistenciaInsertDto
import com.example.myapplication.data.remote.dto.AsistenciaRemoteResponseDto
import com.example.myapplication.data.remote.dto.CalificacionInsertDto
import com.example.myapplication.data.remote.dto.CalificacionRemoteResponseDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    @GET(SupabaseConfig.ESTUDIANTES_TABLE)
    suspend fun getEstudiantesActivos(
        @Query("select") select: String = SupabaseConfig.ESTUDIANTE_SELECT,
        @Query("estado") estadoFilter: String = "eq.activo",
        @Query("deleted_at") deletedFilter: String = "is.null"
    ): List<EstudianteRemoteDto>

    @POST(SupabaseConfig.ASISTENCIAS_TABLE)
    @Headers("Prefer: return=representation")
    suspend fun insertAsistencia(
        @Body body: AsistenciaInsertDto
    ): List<AsistenciaRemoteResponseDto>

    @POST(SupabaseConfig.CALIFICACIONES_TABLE)
    @Headers("Prefer: return=representation")
    suspend fun insertCalificacion(
        @Body body: CalificacionInsertDto
    ): List<CalificacionRemoteResponseDto>
}
