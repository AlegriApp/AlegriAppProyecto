-- IDs actuales en tu proyecto Supabase (consulta de referencia)
-- Ejecuta para ver qué poner en local.properties

SELECT 'tipos_evaluacion' AS tabla, id, nombre FROM tipos_evaluacion ORDER BY id;
SELECT 'periodos_academicos' AS tabla, id, nombre FROM periodos_academicos ORDER BY id;
SELECT 'materias_curso_1' AS tabla, id, nombre, curso_id FROM materias WHERE curso_id = 1 ORDER BY id;
SELECT 'cursos' AS tabla, id, nombre FROM cursos ORDER BY id;

-- Valores recomendados para local.properties (según tu BD):
-- SUPABASE_DEFAULT_CURSO_ID=1
-- SUPABASE_DEFAULT_MATERIA_ID=1
-- SUPABASE_DEFAULT_TIPO_EVALUACION_ID=6   -- "Examen parcial" (NO usar 1)
-- SUPABASE_DEFAULT_PERIODO_ID=1           -- "Periodo 1 - 2025"
    