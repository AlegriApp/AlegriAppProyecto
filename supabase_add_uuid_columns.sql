-- =============================================================================
-- FASE 4 / FASE 9 — Añadir columna `uuid` para Offline First
-- Mantener `SERIAL id` como PK; `uuid` solo es identificador cross-system
-- =============================================================================
-- Ejecutar en Supabase → SQL Editor → Run
-- Impacto: BAJO. Postgres 11+ con DEFAULT gen_random_uuid() es instantáneo.
--          No reescribe la tabla, no rompe FKs, no cambia PKs.
-- =============================================================================

-- pgcrypto trae gen_random_uuid() — Supabase ya lo incluye por defecto
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- ESTUDIANTES
-- ---------------------------------------------------------------------------
ALTER TABLE estudiantes
    ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();

-- ---------------------------------------------------------------------------
-- ASISTENCIAS
-- ---------------------------------------------------------------------------
ALTER TABLE asistencias
    ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();

-- ---------------------------------------------------------------------------
-- CALIFICACIONES
-- ---------------------------------------------------------------------------
ALTER TABLE calificaciones
    ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();

-- ---------------------------------------------------------------------------
-- INCIDENTES (mobile solo PULL — el uuid sirve para idempotencia en PULL)
-- ---------------------------------------------------------------------------
ALTER TABLE incidentes
    ADD COLUMN IF NOT EXISTS uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();

-- ---------------------------------------------------------------------------
-- VERIFICACIÓN
-- ---------------------------------------------------------------------------
SELECT
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE column_name = 'uuid'
  AND table_name IN ('estudiantes', 'asistencias', 'calificaciones', 'incidentes')
ORDER BY table_name;
