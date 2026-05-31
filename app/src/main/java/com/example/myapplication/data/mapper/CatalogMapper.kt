package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.CursoCatalogEntity
import com.example.myapplication.data.local.entity.MateriaCatalogEntity
import com.example.myapplication.data.local.entity.PeriodoAcademicoCatalogEntity
import com.example.myapplication.data.local.entity.TipoEvaluacionCatalogEntity
import com.example.myapplication.data.local.entity.TipoIncidenteCatalogEntity
import com.example.myapplication.data.remote.dto.CursoCatalogRemoteDto
import com.example.myapplication.data.remote.dto.MateriaCatalogRemoteDto
import com.example.myapplication.data.remote.dto.PeriodoAcademicoRemoteDto
import com.example.myapplication.data.remote.dto.TipoEvaluacionRemoteDto
import com.example.myapplication.data.remote.dto.TipoIncidenteRemoteDto
import com.example.myapplication.domain.model.AcademicPeriodCatalog
import com.example.myapplication.domain.model.CourseCatalog
import com.example.myapplication.domain.model.EvaluationTypeCatalog
import com.example.myapplication.domain.model.IncidentTypeCatalog
import com.example.myapplication.domain.model.SubjectCatalog

fun CursoCatalogEntity.toDomain(): CourseCatalog = CourseCatalog(
    id = id,
    nombre = nombre,
    displayName = buildCourseDisplayName(nombre, paralelo, anioLectivo)
)

fun MateriaCatalogEntity.toDomain(): SubjectCatalog = SubjectCatalog(
    id = id,
    nombre = nombre,
    courseId = cursoId
)

fun TipoEvaluacionCatalogEntity.toDomain(): EvaluationTypeCatalog = EvaluationTypeCatalog(
    id = id,
    nombre = nombre
)

fun PeriodoAcademicoCatalogEntity.toDomain(): AcademicPeriodCatalog = AcademicPeriodCatalog(
    id = id,
    nombre = nombre
)

fun TipoIncidenteCatalogEntity.toDomain(): IncidentTypeCatalog = IncidentTypeCatalog(
    id = id,
    nombre = nombre
)

fun CursoCatalogRemoteDto.toEntity(): CursoCatalogEntity = CursoCatalogEntity(
    id = id,
    nombre = nombre,
    paralelo = paralelo.orEmpty(),
    anioLectivo = anioLectivo.orEmpty(),
    periodoAcademicoId = periodoAcademicoId
)

fun MateriaCatalogRemoteDto.toEntity(): MateriaCatalogEntity = MateriaCatalogEntity(
    id = id,
    nombre = nombre,
    cursoId = cursoId
)

fun TipoEvaluacionRemoteDto.toEntity(): TipoEvaluacionCatalogEntity = TipoEvaluacionCatalogEntity(
    id = id,
    nombre = nombre
)

fun PeriodoAcademicoRemoteDto.toEntity(): PeriodoAcademicoCatalogEntity = PeriodoAcademicoCatalogEntity(
    id = id,
    nombre = nombre,
    anioLectivo = anioLectivo.orEmpty()
)

fun TipoIncidenteRemoteDto.toEntity(): TipoIncidenteCatalogEntity = TipoIncidenteCatalogEntity(
    id = id,
    nombre = nombre
)

fun buildCourseDisplayName(nombre: String, paralelo: String, anio: String): String {
    val parts = listOfNotNull(
        nombre.takeIf { it.isNotBlank() },
        paralelo.takeIf { it.isNotBlank() }?.let { "Paralelo $it" },
        anio.takeIf { it.isNotBlank() }
    )
    return parts.joinToString(" · ").ifBlank { nombre }
}
