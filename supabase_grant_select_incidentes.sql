-- =============================================================================
-- FASE 4 — Habilitar PULL de incidentes desde mobile
-- Mobile solo lee incidentes; NO inserta. Otro proceso externo es quien escribe.
-- =============================================================================
-- Ejecutar en Supabase → SQL Editor → Run
-- =============================================================================

-- Permisos de tabla (SOLO SELECT — sin INSERT)
GRANT SELECT ON incidentes TO anon, authenticated;
GRANT SELECT ON tipos_incidente TO anon, authenticated;

ALTER TABLE incidentes ENABLE ROW LEVEL SECURITY;
ALTER TABLE tipos_incidente ENABLE ROW LEVEL SECURITY;

-- Quitar políticas anteriores si existían
DROP POLICY IF EXISTS mobile_select_incidentes ON incidentes;
DROP POLICY IF EXISTS mobile_select_tipos_incidente ON tipos_incidente;

-- Política de SELECT amplia (desarrollo). NO se crea política de INSERT
-- a propósito: la decisión del equipo es que mobile NO escribe incidentes.
CREATE POLICY mobile_select_incidentes ON incidentes
  FOR SELECT TO anon, authenticated
  USING (deleted_at IS NULL);

CREATE POLICY mobile_select_tipos_incidente ON tipos_incidente
  FOR SELECT TO anon, authenticated
  USING (activo = TRUE);

-- Verificación rápida
SELECT tablename, policyname, cmd, roles
FROM pg_policies
WHERE tablename IN ('incidentes', 'tipos_incidente')
ORDER BY tablename, policyname;
