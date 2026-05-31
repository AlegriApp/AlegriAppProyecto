-- =============================================================================
-- FASE 14 — Habilitar INSERT de incidentes desde mobile
-- =============================================================================
-- IMPORTANTE: este SQL REVIERTE la decisión inicial de Fase 0 ("incidentes
-- son PULL only"). El equipo confirmó (5ª iteración) que mobile SÍ debe enviar
-- incidentes a Supabase además de Telegram.
-- =============================================================================
-- Ejecutar en Supabase → SQL Editor → Run.
-- Aplicar DESPUÉS de:
--   1. supabase_add_uuid_columns.sql
--   2. supabase_grant_select_incidentes.sql
-- =============================================================================

GRANT SELECT, INSERT, UPDATE ON incidentes TO anon, authenticated;

ALTER TABLE incidentes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS mobile_insert_incidentes ON incidentes;
DROP POLICY IF EXISTS mobile_update_incidentes ON incidentes;

CREATE POLICY mobile_insert_incidentes ON incidentes
  FOR INSERT TO anon, authenticated
  WITH CHECK (true);

CREATE POLICY mobile_update_incidentes ON incidentes
  FOR UPDATE TO anon, authenticated
  USING (true)
  WITH CHECK (true);

-- Verificación: debe haber 3 políticas para incidentes (SELECT, INSERT, UPDATE)
SELECT tablename, policyname, cmd, roles
FROM pg_policies
WHERE tablename = 'incidentes'
ORDER BY policyname;
