package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.CatalogDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.StudentCourseEntity
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.data.mapper.toSyncBundle
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.remote.dto.DocenteCursoRemoteDto
import com.example.myapplication.domain.model.sync.SyncOutcome

class TeacherDataSyncer(
    private val supabaseApi: SupabaseApiService?,
    private val catalogDao: CatalogDao,
    private val studentDao: StudentDao
) {

    suspend fun hasCachedTeacherData(teacherId: Long): Boolean =
        catalogDao.countTeacherCoursesForTeacher(teacherId) > 0

    suspend fun syncForTeacher(teacherId: Long): SyncOutcome {
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")

        return runCatching {
            val teacherCourses = fetchTeacherCourses(api, teacherId)
                .filter { it.deletedAt == null && !it.estado.equals("inactivo", ignoreCase = true) }

            catalogDao.deleteTeacherCoursesForTeacher(teacherId)
            if (teacherCourses.isEmpty()) {
                return SyncOutcome.Success("Docente sin cursos activos asignados.")
            }

            val courseIds = teacherCourses.map { it.cursoId }.filter { it > 0L }.distinct()
            val coursesFromRelation = teacherCourses.mapNotNull { it.cursos?.toEntity() }
            val courses = coursesFromRelation.ifEmpty {
                api.getCursosActivosByIds(idFilter = courseIds.toInFilter()).map { it.toEntity() }
            }
            catalogDao.replaceCursos(courses)
            catalogDao.replaceTeacherCourses(teacherCourses.map { it.toEntity() })

            val enrollments = if (courseIds.isEmpty()) {
                emptyList()
            } else {
                api.getEstudianteCursosActivosByCourses(courseFilter = courseIds.toInFilter())
                    .filter { it.deletedAt == null && it.estudianteId > 0L && it.cursoId in courseIds }
            }
            val studentIds = enrollments.map { it.estudianteId }.distinct()
            val remoteStudents = if (studentIds.isEmpty()) {
                emptyList()
            } else {
                api.getEstudiantesActivosByIds(idFilter = studentIds.toInFilter())
            }

            val bundles = remoteStudents.map { it.toSyncBundle() }
            if (bundles.isNotEmpty()) {
                studentDao.insertOrReplaceStudents(bundles.map { it.student })
            }

            catalogDao.deleteStudentCoursesForCourses(courseIds)
            val linksFromEnrollmentTable = enrollments.map { row ->
                StudentCourseEntity(studentId = row.estudianteId, courseId = row.cursoId)
            }
            val linksFromEmbed = bundles.flatMap { bundle ->
                bundle.courseLinks.filter { it.courseId in courseIds }
            }
            val links = (linksFromEnrollmentTable + linksFromEmbed)
                .distinctBy { "${it.studentId}_${it.courseId}" }
            if (links.isNotEmpty()) {
                catalogDao.replaceStudentCourses(links)
            }

            val representatives = bundles.flatMap { it.representatives }
            if (representatives.isNotEmpty()) {
                catalogDao.replaceStudentRepresentatives(representatives)
            }
            val telegram = bundles.flatMap { it.telegramConfigs }
            if (telegram.isNotEmpty()) {
                catalogDao.replaceTelegramConfigs(telegram)
            }

            SyncOutcome.Success(
                "Docente: ${courses.size} curso(s), ${bundles.size} estudiante(s), " +
                    "${links.size} matricula(s) cacheadas."
            )
        }.getOrElse { error ->
            SyncOutcome.Failure(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudieron sincronizar cursos y estudiantes del docente."
            )
        }
    }

    private suspend fun fetchTeacherCourses(
        api: SupabaseApiService,
        teacherId: Long
    ): List<DocenteCursoRemoteDto> =
        runCatching {
            api.getDocenteCursosActivos(docenteFilter = "eq.$teacherId")
        }.getOrElse {
            api.getDocenteCursosActivos(
                select = "docente_id,curso_id,materia_id,es_tutor,estado,deleted_at",
                docenteFilter = "eq.$teacherId"
            )
        }

    private fun List<Long>.toInFilter(): String =
        joinToString(prefix = "in.(", postfix = ")")
}
