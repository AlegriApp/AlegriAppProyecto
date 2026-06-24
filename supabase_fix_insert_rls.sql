-- =============================================================================
-- FIX: HTTP 401 al guardar asistencia (sync)
-- Causa real: RLS sin permiso de INSERT en asistencias/calificaciones
-- Mensaje típico: "new row violates row-level security policy"
-- =============================================================================
-- Ejecutar en Supabase → SQL Editor → Run
-- =============================================================================

-- Permisos de tabla (además de las políticas RLS)
GRANT SELECT, INSERT ON asistencias TO anon, authenticated;
GRANT SELECT, INSERT ON calificaciones TO anon, authenticated;

ALTER TABLE asistencias ENABLE ROW LEVEL SECURITY;
ALTER TABLE calificaciones ENABLE ROW LEVEL SECURITY;

-- Quitar políticas anteriores si existían
DROP POLICY IF EXISTS anon_insert_asistencias ON asistencias;
DROP POLICY IF EXISTS anon_select_asistencias ON asistencias;
DROP POLICY IF EXISTS mobile_insert_asistencias ON asistencias;
DROP POLICY IF EXISTS mobile_select_asistencias ON asistencias;

DROP POLICY IF EXISTS anon_insert_calificaciones ON calificaciones;
DROP POLICY IF EXISTS anon_select_calificaciones ON calificaciones;
DROP POLICY IF EXISTS mobile_insert_calificaciones ON calificaciones;
DROP POLICY IF EXISTS mobile_select_calificaciones ON calificaciones;

-- Políticas amplias para la app móvil (desarrollo)
CREATE POLICY mobile_insert_asistencias ON asistencias
  FOR INSERT TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY mobile_select_asistencias ON asistencias
  FOR SELECT TO anon, authenticated
  USING (true);

CREATE POLICY mobile_insert_calificaciones ON calificaciones
  FOR INSERT TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY mobile_select_calificaciones ON calificaciones
  FOR SELECT TO anon, authenticated
  USING (true);

-- Verificación rápida
SELECT tablename, policyname, cmd, roles
FROM pg_policies
WHERE tablename IN ('asistencias', 'calificaciones')
ORDER BY tablename, policyname;
