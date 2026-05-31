package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.CursoCatalogEntity
import com.example.myapplication.data.local.entity.MateriaCatalogEntity
import com.example.myapplication.data.local.entity.PeriodoAcademicoCatalogEntity
import com.example.myapplication.data.local.entity.StudentCourseEntity
import com.example.myapplication.data.local.entity.StudentRepresentativeEntity
import com.example.myapplication.data.local.entity.TelegramConfigEntity
import com.example.myapplication.data.local.entity.TipoEvaluacionCatalogEntity
import com.example.myapplication.data.local.entity.TipoIncidenteCatalogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query("SELECT * FROM catalog_cursos ORDER BY nombre ASC, paralelo ASC")
    fun observeCursos(): Flow<List<CursoCatalogEntity>>

    @Query("SELECT * FROM catalog_materias WHERE curso_id = :courseId ORDER BY nombre ASC")
    fun observeMateriasByCourse(courseId: Long): Flow<List<MateriaCatalogEntity>>

    @Query("SELECT * FROM catalog_tipos_evaluacion ORDER BY nombre ASC")
    fun observeTiposEvaluacion(): Flow<List<TipoEvaluacionCatalogEntity>>

    @Query("SELECT * FROM catalog_periodos ORDER BY nombre ASC")
    fun observePeriodos(): Flow<List<PeriodoAcademicoCatalogEntity>>

    @Query("SELECT * FROM catalog_tipos_incidente ORDER BY nombre ASC")
    fun observeTiposIncidente(): Flow<List<TipoIncidenteCatalogEntity>>

    @Query(
        "SELECT sr.representante_id FROM student_representatives sr " +
            "WHERE sr.student_id = :studentId " +
            "ORDER BY sr.es_principal DESC LIMIT 1"
    )
    suspend fun getPrincipalRepresentanteId(studentId: Long): Long?

    @Query(
        "SELECT * FROM telegram_configs " +
            "WHERE representante_id = :representanteId AND estado_integracion = 'activo' " +
            "LIMIT 1"
    )
    suspend fun getTelegramConfigForRepresentante(representanteId: Long): TelegramConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceCursos(items: List<CursoCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceMaterias(items: List<MateriaCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceTiposEvaluacion(items: List<TipoEvaluacionCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replacePeriodos(items: List<PeriodoAcademicoCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceTiposIncidente(items: List<TipoIncidenteCatalogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceStudentCourses(items: List<StudentCourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceStudentRepresentatives(items: List<StudentRepresentativeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceTelegramConfigs(items: List<TelegramConfigEntity>)

    @Query("DELETE FROM student_courses")
    suspend fun clearStudentCourses()

    @Query("DELETE FROM student_representatives")
    suspend fun clearStudentRepresentatives()

    @Query("SELECT COUNT(*) FROM catalog_cursos")
    suspend fun countCursos(): Int
}
