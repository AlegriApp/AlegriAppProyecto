-- =============================================================================
-- AlegriApp — RLS para API móvil (rol anon) + datos de prueba
-- Ejecutar en: Supabase Dashboard → SQL Editor → Run
-- =============================================================================
-- Problema que resuelve:
--   El SQL Editor (postgres) ve todos los datos.
--   La app usa REST + clave publishable/anon → RLS sin políticas = respuesta [].
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) POLÍTICAS RLS (idempotentes: borra y recrea)
-- -----------------------------------------------------------------------------

-- Catálogos / lectura
DO $$ DECLARE t TEXT; BEGIN
  FOREACH t IN ARRAY ARRAY[
    'niveles_academicos','periodos_academicos','cursos','materias',
    'tipos_evaluacion','estudiantes','estudiante_curso'
  ] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('DROP POLICY IF EXISTS anon_select_%I ON %I', t, t);
    EXECUTE format(
      'CREATE POLICY anon_select_%I ON %I FOR SELECT TO anon USING (true)',
      t, t
    );
  END LOOP;
END $$;

-- Escritura desde la app (GRANT + RLS para anon y authenticated)
GRANT SELECT, INSERT ON asistencias TO anon, authenticated;
GRANT SELECT, INSERT ON calificaciones TO anon, authenticated;

ALTER TABLE asistencias ENABLE ROW LEVEL SECURITY;
ALTER TABLE calificaciones ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS anon_insert_asistencias ON asistencias;
DROP POLICY IF EXISTS anon_select_asistencias ON asistencias;
DROP POLICY IF EXISTS mobile_insert_asistencias ON asistencias;
DROP POLICY IF EXISTS mobile_select_asistencias ON asistencias;
DROP POLICY IF EXISTS anon_insert_calificaciones ON calificaciones;
DROP POLICY IF EXISTS anon_select_calificaciones ON calificaciones;
DROP POLICY IF EXISTS mobile_insert_calificaciones ON calificaciones;
DROP POLICY IF EXISTS mobile_select_calificaciones ON calificaciones;

CREATE POLICY mobile_insert_asistencias ON asistencias
  FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY mobile_select_asistencias ON asistencias
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY mobile_insert_calificaciones ON calificaciones
  FOR INSERT TO anon, authenticated WITH CHECK (true);
CREATE POLICY mobile_select_calificaciones ON calificaciones
  FOR SELECT TO anon, authenticated USING (true);

-- -----------------------------------------------------------------------------
-- 2) CATÁLOGOS MÍNIMOS (solo si faltan)
-- -----------------------------------------------------------------------------

INSERT INTO tipos_evaluacion (nombre, descripcion, activo)
VALUES ('General', 'Evaluación general', true)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO periodos_academicos (nombre, anio_lectivo, fecha_inicio, fecha_fin, activo)
SELECT 'Primer trimestre', '2024-2025', '2024-09-01'::timestamptz, '2024-12-20'::timestamptz, true
WHERE NOT EXISTS (SELECT 1 FROM periodos_academicos WHERE id = 1);

INSERT INTO niveles_academicos (nombre, descripcion, orden, activo)
SELECT v.nombre, v.descripcion, v.orden, true
FROM (VALUES
  ('Primaria', 'Nivel primaria', 1),
  ('Secundaria', 'Nivel secundaria', 2)
) AS v(nombre, descripcion, orden)
WHERE NOT EXISTS (SELECT 1 FROM niveles_academicos LIMIT 1);

-- -----------------------------------------------------------------------------
-- 3) ESTUDIANTES DE PRUEBA (8 alumnos, como el seed de la app)
-- -----------------------------------------------------------------------------

INSERT INTO estudiantes (codigo_institucional, nombre, apellido, estado, genero)
VALUES
  ('EST-001', 'María',   'González Pérez',   'activo', 'F'),
  ('EST-002', 'Carlos',  'Rodríguez López',  'activo', 'M'),
  ('EST-003', 'Valentina','Martínez Ruiz',  'activo', 'F'),
  ('EST-004', 'Diego',   'Hernández Silva', 'activo', 'M'),
  ('EST-005', 'Sofía',   'Pérez Morales',   'activo', 'F'),
  ('EST-006', 'Andrés',  'Jiménez Castro',  'activo', 'M'),
  ('EST-007', 'Isabella','Torres Vega',     'activo', 'F'),
  ('EST-008', 'Mateo',   'Ramírez Díaz',    'activo', 'M')
ON CONFLICT (codigo_institucional) DO UPDATE SET
  nombre = EXCLUDED.nombre,
  apellido = EXCLUDED.apellido,
  estado = 'activo',
  deleted_at = NULL;

-- -----------------------------------------------------------------------------
-- 4) INSCRIPCIÓN AL CURSO 1 (1ro Primaria - Paralelo A)
--    Ajusta curso_id si tu curso principal tiene otro id.
-- -----------------------------------------------------------------------------

INSERT INTO estudiante_curso (estudiante_id, curso_id, estado)
SELECT e.id, 1, 'activo'
FROM estudiantes e
WHERE e.codigo_institucional LIKE 'EST-%'
  AND e.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM estudiante_curso ec
    WHERE ec.estudiante_id = e.id AND ec.curso_id = 1
  );

-- -----------------------------------------------------------------------------
-- 5) MATERIA DE PRUEBA EN CURSO 1 (para calificaciones / asistencias)
-- -----------------------------------------------------------------------------

INSERT INTO materias (nombre, descripcion, curso_id, estado)
SELECT 'General', 'Materia general del curso', 1, 'activo'
WHERE EXISTS (SELECT 1 FROM cursos WHERE id = 1)
  AND NOT EXISTS (SELECT 1 FROM materias WHERE nombre = 'General' AND curso_id = 1);

COMMIT;

-- -----------------------------------------------------------------------------
-- 6) VERIFICACIÓN (debe devolver filas > 0)
-- -----------------------------------------------------------------------------

SELECT 'cursos' AS tabla, COUNT(*) AS total FROM cursos;
SELECT 'estudiantes_activos' AS tabla, COUNT(*) AS total
FROM estudiantes WHERE estado = 'activo' AND deleted_at IS NULL;
SELECT 'estudiante_curso_activo' AS tabla, COUNT(*) AS total
FROM estudiante_curso WHERE estado = 'activo';
SELECT 'materias_curso_1' AS tabla, COUNT(*) AS total
FROM materias WHERE curso_id = 1;

-- Prueba equivalente a la app (PostgREST):
-- GET /rest/v1/estudiantes?estado=eq.activo&deleted_at=is.null
