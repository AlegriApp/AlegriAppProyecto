package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.CatalogDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.TelegramConfigEntity
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.remote.dto.ConfiguracionTelegramRemoteDto
import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.domain.model.TelegramDestination
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.repository.CatalogRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class CatalogRepositoryImpl(
    private val supabaseApi: SupabaseApiService?,
    private val catalogDao: CatalogDao,
    private val studentDao: StudentDao,
    private val authPreferences: AuthPreferences
) : CatalogRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCourses(): Flow<List<com.example.myapplication.domain.model.CourseCatalog>> =
        authPreferences.session.flatMapLatest { session ->
            if (session == null) {
                catalogDao.observeCursos()
            } else {
                catalogDao.observeCursosForTeacher(session.userId)
            }
        }.map { list -> list.map { it.toDomain() } }

    override fun observeSubjectsByCourse(courseId: Long) =
        catalogDao.observeMateriasByCourse(courseId).map { list -> list.map { it.toDomain() } }

    override fun observeEvaluationTypes() =
        catalogDao.observeTiposEvaluacion().map { list -> list.map { it.toDomain() } }

    override fun observeAcademicPeriods() =
        catalogDao.observePeriodos().map { list -> list.map { it.toDomain() } }

    override fun observeIncidentTypes() =
        catalogDao.observeTiposIncidente().map { list -> list.map { it.toDomain() } }

    override suspend fun resolveTelegramForStudent(studentId: Long): TelegramDestination? {
        val student = studentDao.getStudentById(studentId) ?: return null
        student.telegramChatId?.takeIf { it.isNotBlank() }?.let { chatId ->
            val token = student.representativeId?.let { catalogDao.getTelegramConfigForRepresentante(it)?.botToken }
            return TelegramDestination(chatId = chatId, botToken = token?.takeIf { it.isNotBlank() })
        }
        val repId = student.representativeId
            ?: catalogDao.getPrincipalRepresentanteId(studentId)
            ?: return null
        val config = catalogDao.getTelegramConfigForRepresentante(repId) ?: return null
        return TelegramDestination(
            chatId = config.chatId,
            botToken = config.botToken?.takeIf { it.isNotBlank() }
        )
    }

    override suspend fun syncCatalogsFromRemote(): SyncOutcome {
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")
        return runCatching {
            val cursos = api.getCursosActivos().map { it.toEntity() }
            val materias = api.getMateriasActivas().map { it.toEntity() }
            val tiposEval = api.getTiposEvaluacionActivos().map { it.toEntity() }
            val periodos = api.getPeriodosActivos().map { it.toEntity() }
            val tiposInc = api.getTiposIncidenteActivos().map { it.toEntity() }
            val telegram = api.getConfiguracionTelegramActiva().map { it.toEntity() }

            catalogDao.replaceCursos(cursos)
            catalogDao.replaceMaterias(materias)
            catalogDao.replaceTiposEvaluacion(tiposEval)
            catalogDao.replacePeriodos(periodos)
            catalogDao.replaceTiposIncidente(tiposInc)
            catalogDao.replaceTelegramConfigs(telegram)

            SyncOutcome.Success(
                "Catálogos: ${cursos.size} cursos, ${materias.size} materias, " +
                    "${tiposEval.size} tipos eval., ${periodos.size} periodos, " +
                    "${tiposInc.size} tipos incidente, ${telegram.size} configs Telegram."
            )
        }.getOrElse { error ->
            SyncOutcome.Failure(error.message ?: "Error al sincronizar catálogos.")
        }
    }

    private fun ConfiguracionTelegramRemoteDto.toEntity(): TelegramConfigEntity = TelegramConfigEntity(
        id = id,
        representanteId = representanteId,
        chatId = chatId,
        botToken = tokenBot,
        estadoIntegracion = estadoIntegracion ?: "activo"
    )
}
