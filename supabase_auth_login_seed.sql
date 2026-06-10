-- =============================================================================
-- AlegriApp - Login mobile (RLS + usuario de prueba)
-- Ejecutar en: Supabase Dashboard -> SQL Editor -> Run
-- =============================================================================
-- NOTA:
-- Este proyecto valida `password_hash` desde la app mobile, por lo que esta
-- politica expone lectura de `usuarios.password_hash` al rol anon/authenticated.
-- Es util para pruebas y prototipo, pero no es el enfoque ideal para produccion.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1) Rol base para el usuario de prueba
-- -----------------------------------------------------------------------------

INSERT INTO roles (nombre, descripcion, activo)
VALUES ('Docente', 'Usuario docente para acceso movil', true)
ON CONFLICT (nombre) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2) Permisos y RLS para lectura desde la app
-- -----------------------------------------------------------------------------

GRANT SELECT ON roles TO anon, authenticated;
GRANT SELECT ON usuarios TO anon, authenticated;

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS anon_select_roles ON roles;
DROP POLICY IF EXISTS anon_select_usuarios ON usuarios;
DROP POLICY IF EXISTS mobile_select_roles ON roles;
DROP POLICY IF EXISTS mobile_select_usuarios ON usuarios;

CREATE POLICY mobile_select_roles ON roles
  FOR SELECT TO anon, authenticated
  USING (activo = true);

CREATE POLICY mobile_select_usuarios ON usuarios
  FOR SELECT TO anon, authenticated
  USING (estado = 'activo' AND deleted_at IS NULL);

-- -----------------------------------------------------------------------------
-- 3) Usuario de prueba para el login
-- -----------------------------------------------------------------------------

INSERT INTO usuarios (
  nombre,
  apellido,
  email,
  password_hash,
  telefono,
  rol_id,
  estado
)
SELECT
  'Kelvin',
  'Docente',
  'docente@alegriapp.com',
  '1234',
  '0999999999',
  r.id,
  'activo'
FROM roles r
WHERE r.nombre = 'Docente'
ON CONFLICT (email) DO UPDATE SET
  nombre = EXCLUDED.nombre,
  apellido = EXCLUDED.apellido,
  password_hash = EXCLUDED.password_hash,
  telefono = EXCLUDED.telefono,
  rol_id = EXCLUDED.rol_id,
  estado = EXCLUDED.estado,
  deleted_at = NULL,
  updated_at = NOW();

COMMIT;

-- -----------------------------------------------------------------------------
-- 4) Verificacion
-- -----------------------------------------------------------------------------

SELECT id, nombre, apellido, email, rol_id, estado
FROM usuarios
WHERE email = 'docente@alegriapp.com';
