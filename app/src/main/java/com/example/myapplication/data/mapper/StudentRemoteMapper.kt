package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.StudentCourseEntity
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.data.local.entity.StudentRepresentativeEntity
import com.example.myapplication.data.local.entity.TelegramConfigEntity
import com.example.myapplication.data.remote.dto.ConfiguracionTelegramRemoteDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import com.example.myapplication.domain.model.sync.SyncState

data class StudentSyncBundle(
    val student: StudentEntity,
    val courseLinks: List<StudentCourseEntity>,
    val representatives: List<StudentRepresentativeEntity>,
    val telegramConfigs: List<TelegramConfigEntity>
)

fun EstudianteRemoteDto.toSyncBundle(): StudentSyncBundle {
    val activeEnrollments = estudianteCurso.orEmpty().filter { it.estado == "activo" }
    val enrollment = activeEnrollments.firstOrNull() ?: estudianteCurso?.firstOrNull()

    val nivel = enrollment?.cursos?.nivelAcademico?.nombre?.trim().orEmpty()
    val paralelo = enrollment?.cursos?.paralelo?.trim().orEmpty()
    val courseName = enrollment?.cursos?.nombre?.trim().orEmpty()

    val repLinks = estudianteRepresentante.orEmpty().filter { it.deletedAt == null }
    val principalRep = repLinks.firstOrNull { it.esPrincipal == true } ?: repLinks.firstOrNull()

    var chatId: String? = null
    var botToken: String? = null
    var representativeId: Long? = principalRep?.representanteId ?: principalRep?.representantes?.id
    var representativeName = ""

    principalRep?.representantes?.let { rep ->
        representativeId = rep.id
        representativeName = listOfNotNull(rep.nombre, rep.apellido).joinToString(" ").trim()
        val activeConfig = rep.configuracionTelegram
            ?.firstOrNull { it.estadoIntegracion == "activo" && it.deletedAt == null }
        chatId = activeConfig?.chatId
        botToken = activeConfig?.tokenBot
    }

    val student = StudentEntity(
        id = id,
        fullName = listOf(nombre, apellido).joinToString(" ").trim(),
        grade = nivel.ifBlank { courseName.ifBlank { "Sin nivel" } },
        section = paralelo.ifBlank { "?" },
        representativeName = representativeName,
        telegramChatId = chatId,
        representativeId = representativeId,
        uuid = uuid ?: newUuid(),
        remoteId = id,
        syncStatus = SyncState.Stored.SUCCESS,
        serverUpdatedAt = updatedAt?.toEpochMillisOrNull(),
        isDeleted = deletedAt != null
    )

    val courseLinks = activeEnrollments.mapNotNull { ec ->
        val courseId = ec.cursoId ?: ec.cursos?.id ?: return@mapNotNull null
        StudentCourseEntity(studentId = id, courseId = courseId)
    }.distinctBy { it.courseId }

    val representatives = repLinks.mapNotNull { link ->
        val repId = link.representanteId ?: link.representantes?.id ?: return@mapNotNull null
        StudentRepresentativeEntity(
            studentId = id,
            representanteId = repId,
            esPrincipal = link.esPrincipal == true
        )
    }

    val telegramFromRep = repLinks.flatMap { link ->
        link.representantes?.configuracionTelegram.orEmpty()
            .filter { it.estadoIntegracion == "activo" && it.deletedAt == null }
            .map { it.toTelegramEntity(link.representantes?.id) }
    }

    return StudentSyncBundle(
        student = student,
        courseLinks = courseLinks,
        representatives = representatives,
        telegramConfigs = telegramFromRep
    )
}

private fun ConfiguracionTelegramRemoteDto.toTelegramEntity(
    representanteId: Long?
): TelegramConfigEntity {
    val repId = representanteId ?: this.representanteId
    val stableId = id.takeIf { it > 0L }
        ?: ((repId ?: 0L) * 10_000L + chatId.hashCode().toLong().let { if (it < 0) -it else it })
    return TelegramConfigEntity(
    id = stableId,
    representanteId = repId,
    chatId = chatId,
    botToken = tokenBot,
    estadoIntegracion = estadoIntegracion ?: "activo"
    )
}
