-- RLS + GRANT SELECT para catálogos (rol anon = app móvil)
-- El SQL Editor usa rol postgres y ve todo; la app usa anon y necesita GRANT + política.
-- Ejecutar completo en Supabase → SQL Editor → Run

-- ---------- GRANTS (imprescindibles para PostgREST / REST) ----------
GRANT SELECT ON cursos TO anon, authenticated;
GRANT SELECT ON materias TO anon, authenticated;
GRANT SELECT ON tipos_evaluacion TO anon, authenticated;
GRANT SELECT ON periodos_academicos TO anon, authenticated;
GRANT SELECT ON tipos_incidente TO anon, authenticated;
GRANT SELECT ON configuracion_telegram TO anon, authenticated;
GRANT SELECT ON estudiantes TO anon, authenticated;
GRANT SELECT ON estudiante_curso TO anon, authenticated;
GRANT SELECT ON niveles_academicos TO anon, authenticated;

-- ---------- RLS ----------
ALTER TABLE cursos ENABLE ROW LEVEL SECURITY;
ALTER TABLE materias ENABLE ROW LEVEL SECURITY;
ALTER TABLE tipos_evaluacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE periodos_academicos ENABLE ROW LEVEL SECURITY;
ALTER TABLE tipos_incidente ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuracion_telegram ENABLE ROW LEVEL SECURITY;
ALTER TABLE estudiante_curso ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS anon_select_cursos ON cursos;
CREATE POLICY anon_select_cursos ON cursos FOR SELECT TO anon
    USING (deleted_at IS NULL AND estado = 'activo');

DROP POLICY IF EXISTS anon_select_materias ON materias;
CREATE POLICY anon_select_materias ON materias FOR SELECT TO anon
    USING (deleted_at IS NULL AND estado = 'activo');

DROP POLICY IF EXISTS anon_select_tipos_evaluacion ON tipos_evaluacion;
CREATE POLICY anon_select_tipos_evaluacion ON tipos_evaluacion FOR SELECT TO anon
    USING (activo = true);

DROP POLICY IF EXISTS anon_select_periodos ON periodos_academicos;
CREATE POLICY anon_select_periodos ON periodos_academicos FOR SELECT TO anon
    USING (activo = true);

DROP POLICY IF EXISTS anon_select_tipos_incidente ON tipos_incidente;
CREATE POLICY anon_select_tipos_incidente ON tipos_incidente FOR SELECT TO anon
    USING (activo = true);

DROP POLICY IF EXISTS anon_select_config_telegram ON configuracion_telegram;
CREATE POLICY anon_select_config_telegram ON configuracion_telegram
    FOR SELECT TO anon USING (deleted_at IS NULL AND estado_integracion = 'activo');

DROP POLICY IF EXISTS anon_select_estudiante_curso ON estudiante_curso;
CREATE POLICY anon_select_estudiante_curso ON estudiante_curso
    FOR SELECT TO anon USING (estado = 'activo');

-- Verificación (como la app REST, no como postgres):
-- GET /rest/v1/estudiante_curso?select=estudiante_id,curso_id&estado=eq.activo
-- Debe devolver 28 filas si tienes 13+13+2 matrículas activas.
