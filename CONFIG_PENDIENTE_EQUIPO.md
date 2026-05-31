# AlegriAPP - Configuracion pendiente para el equipo

Este documento resume lo que falta configurar y cerrar para completar la fase actual en los modulos de Asistencias, Calificaciones, OCR (ML Kit) y Telegram.

## 1. Variables locales obligatorias

Completar en `local.properties` (no subir a git):

```properties
TELEGRAM_BOT_TOKEN=pegar_token_real
TELEGRAM_CHAT_ID=pegar_chat_id_real
```

Notas:
- Si estas variables estan vacias, el envio de reportes por Telegram falla con mensaje de configuracion.
- No exponer token en codigo, commits o capturas.

## 2. Telegram - pendientes funcionales

Estado actual:
- Ya existe integracion base con `TelegramApiService`, `TelegramRepository`, `SendTelegramMessageUseCase` y builders de mensaje.
- Asistencias y Calificaciones ya intentan enviar reporte/boletin.

Falta por cerrar:
- Reemplazar `TELEGRAM_CHAT_ID` global por enrutamiento real por destinatario usando tabla `configuracion_telegram`.
- Implementar seleccion de chat por estudiante/representante (segun relacion en DB).
- Guardar trazabilidad de envios en tabla `notificaciones` (`estado`, `fecha_envio`, `intentos`, `mensaje_error`, `respuesta_telegram`).
- Manejar reintentos para estado `fallido`.

## 3. SQL vs Android - alineacion pendiente

Se avanzo en Asistencias/Calificaciones, pero todavia faltan tablas catalogo para eliminar campos temporales de apoyo.

Pendiente principal:
- Crear entidades/DAO/repos para catalogos y relaciones:
  - `materias`
  - `periodos_academicos`
  - `tipos_evaluacion`
  - `cursos`
  - `estudiante_curso`
- Cambiar filtros actuales en calificaciones de nombre (`materia_nombre`, `periodo_nombre`) a IDs reales:
  - `materia_id`
  - `periodo_academico_id`

## 4. Asistencias - cierre de fase

Estado actual:
- Carga y guardado real en Room.
- Estados reales (`presente`, `atrasado`, `ausente`, `justificado`).
- Validacion de estudiantes sin marcar.

Falta:
- Conectar `curso_id`, `materia_id`, `docente_id` reales desde flujo autenticado/sesion.
- Definir si `justificado` se marca manualmente desde UI (hoy se soporta en dominio/mapeo pero no hay boton dedicado).
- Sincronizacion remota real con backend institucional (si endpoint ya disponible).

## 5. Calificaciones - cierre de fase

Estado actual:
- Consulta y guardado local real.
- OCR integrado para lectura de imagen (sin auto-guardado).
- Envio de boletin por Telegram conectado.

Falta:
- Implementar permisos por rol:
  - Padre/representante: solo lectura.
  - Docente/autoridad: edicion segun `permisos_rol`.
- Usar `tipo_evaluacion_id` real desde catalogo (no texto libre).
- Publicacion por estado real SQL (`registrado`, `revisado`, `publicado`, `anulado`).

## 6. OCR (Google ML Kit) - mejoras pendientes

Estado actual:
- OCR funcional por seleccion de imagen.
- Texto detectado visible en UI.
- En Asistencias hay aplicacion manual de sugerencias.

Falta:
- Parser robusto por filas/columnas para actas reales.
- Modo camara en vivo con CameraX (actualmente flujo por galeria).
- Registro de resultados ML en tabla `registros_analisis_ml`.
- Pantalla de revision para confirmar coincidencias ambiguas antes de guardar.

## 7. Modulo de Incidentes/Reportes

**Estado actual (post-3ª auditoría Offline First):**
- `IncidentEntity`, `IncidentDao`, `IncidentRepositoryImpl`, casos de uso y UI ya estan implementados localmente.
- Persistencia local en Room funciona.
- Envio por Telegram funciona vía `SendIncidentReportUseCase`.

**Decisión del equipo (Offline First — Fase 14, 6ª iteración):**

> ⚠️ **REVERTIDO:** la decisión anterior (Fase 0) de "PULL only" fue revertida.
> Mobile ahora **SÍ envía** incidentes a Supabase **además** de Telegram.

- ✅ **Mobile envía POST** de incidentes a Supabase (`upsertIncidente` por `uuid`).
- ✅ **Mobile hace PULL** de incidentes desde Supabase (sin cambio).
- ✅ **Mobile envía por Telegram** los incidentes con `enviado=0` (Fase 13).

**Limitación conocida:** si un incidente referencia un estudiante creado offline
(id local negativo), el push a Supabase se omite y queda como `sync_status=ERROR`
con mensaje "Estudiante local sin sincronizar...". Hay que asociar el incidente
a un estudiante que ya esté en el catálogo remoto.

**SQL relacionado:**
- `supabase_grant_select_incidentes.sql` — PULL SELECT (sin cambio).
- `supabase_grant_insert_incidentes.sql` (**nuevo Fase 14**) — habilita INSERT/UPDATE
  con `RLS WITH CHECK (true)` para `anon, authenticated`.

## 8. Offline First — matriz entidad × operación × estrategia de sync

| Entidad | CREATE local | UPDATE local | DELETE local | PUSH a remoto | PULL desde remoto | Notas |
|---------|--------------|--------------|--------------|----------------|---------------------|-------|
| `estudiantes` | ✅ (UUID local) | ✅ | Soft (`is_deleted=1`) | ✅ upsert por `uuid` | ✅ incremental por `updated_at` | Mobile puede crear offline para flujo de incidente con estudiante nuevo |
| `asistencias` | ✅ (UUID local) | ✅ | Soft | ✅ upsert por `uuid` | ✅ incremental | Clave natural UNIQUE en Postgres: `(estudiante, curso, fecha, materia)` |
| `calificaciones` | ✅ (UUID local) | ✅ | Soft | ✅ upsert por `uuid` | ✅ incremental | Sin UNIQUE natural en Postgres → `uuid` resuelve idempotencia |
| `incidentes` | ✅ (UUID local) | ✅ | Soft | ✅ upsert por `uuid` (Fase 14) | ✅ incremental por `updated_at` | **Bloqueo defensivo:** push omite incidentes con `studentId<=0` (estudiante local sin remote_id) |
| `tipos_incidente`, `tipos_evaluacion`, `niveles_academicos`, `periodos_academicos`, `cursos`, `materias` | ❌ | ❌ | ❌ | ❌ | ✅ pull periódico (catálogos) | Solo lectura desde mobile |
| `evidencias` (futuro) | ✅ (uuid + ruta_local) | ✅ | Soft | ✅ subir blob + actualizar `url_remota` | ✅ | Patrón nativo del esquema (`ruta_local` + `url_remota`) |
| `notificaciones`, `reportes`, `auditoria`, `sincronizacion_pendiente` (server-side) | ❌ | ❌ | ❌ | ❌ | ❌ | Server-side puro, mobile no toca |

**Estados de sincronización** (alineados a `CRONOGRAMA_PROYECTO_FINAL.md` → sealed class `ActionState`):

| Valor en Room (`sync_status`) | Significado | Mapea a `SyncState` (sealed) |
|--------------------------------|-------------|-------------------------------|
| `IDLE` | Recién creado / pendiente / aún no enviado | `SyncState.Idle` |
| `SENDING` | En proceso de envío al servidor | `SyncState.Sending` |
| `SUCCESS` | Sincronizado correctamente | `SyncState.Success` |
| `ERROR` | Último intento falló (ver `sync_error`) | `SyncState.Error(message)` |

**Compatibilidad:** la columna existente `sincronizacion_pendiente BOOLEAN` se conserva durante la transición. La regla de mapeo en la migración es:
- `sincronizacion_pendiente = 1` → `sync_status = 'IDLE'`
- `sincronizacion_pendiente = 0` → `sync_status = 'SUCCESS'`

## 9. Offline First — SQL adicional generado

| Archivo | Cuándo aplicar | Notas |
|---------|----------------|-------|
| `supabase_fix_insert_rls.sql` (preexistente) | Ya aplicado | Habilita INSERT en `asistencias` y `calificaciones` |
| `supabase_add_uuid_columns.sql` (nuevo, Fase 4) | Antes de Fase 4 | Añade columna `uuid UUID UNIQUE DEFAULT gen_random_uuid()` a `estudiantes`, `asistencias`, `calificaciones`, `incidentes`. Compatible con Postgres 11+. **No reescribe la tabla.** |
| `supabase_grant_select_incidentes.sql` (nuevo, Fase 4) | Antes de Fase 4 | Habilita PULL de `incidentes` y `tipos_incidente`. |
| `supabase_grant_insert_incidentes.sql` (**nuevo, Fase 14**) | Antes de usar push de incidentes desde mobile | Añade `GRANT INSERT/UPDATE` + policies `mobile_insert_incidentes` y `mobile_update_incidentes` |

**Verificación post-aplicación:**

```sql
-- 1. Comprobar columnas uuid
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE column_name = 'uuid'
  AND table_name IN ('estudiantes','asistencias','calificaciones','incidentes');

-- 2. Comprobar políticas RLS de incidentes (debe haber SELECT, no INSERT)
SELECT tablename, policyname, cmd
FROM pg_policies
WHERE tablename IN ('incidentes','tipos_incidente');

-- 3. Comprobar que las filas existentes recibieron UUID
SELECT COUNT(*) AS total, COUNT(uuid) AS con_uuid
FROM asistencias;
```

## 10. Checklist rapido antes de demo

- [ ] `TELEGRAM_BOT_TOKEN` y `TELEGRAM_CHAT_ID` configurados localmente.
- [ ] Probar envio real de reporte de asistencia.
- [ ] Probar envio real de boletin de calificaciones.
- [ ] Verificar que no haya datos quemados en pantallas principales.
- [ ] Confirmar que OCR no guarda automaticamente sin revision humana.
- [ ] Validar build debug en otra maquina del equipo.
- [ ] Confirmar que no se suben secretos a git.
- [ ] Aplicar `supabase_add_uuid_columns.sql` antes de Fase 4.
- [ ] Aplicar `supabase_grant_select_incidentes.sql` antes de Fase 4.
- [ ] **Aplicar `supabase_grant_insert_incidentes.sql` antes de Fase 14** (nuevo).
- [ ] Confirmar matriz §8.
- [ ] Ajustar en `local.properties` el `SUPABASE_DEFAULT_TIPO_INCIDENTE_ID` si el valor `1` no aplica al proyecto.

## 11. Recomendacion para repartir trabajo (equipo)

- Persona 1: Catalogos SQL -> Room/API (`materias`, `periodos`, `tipos_evaluacion`, `cursos`).
- Persona 2: Permisos por rol + restricciones UI en Calificaciones.
- Persona 3: Incidentes end-to-end (data/domain/presentation).
- Persona 4: Telegram avanzado (`configuracion_telegram` + `notificaciones` + reintentos).
- Persona 5: OCR avanzado (parser de actas, validacion, CameraX, registro ML).
- Persona 6: Offline First (Room migrations, WorkManager, SyncScheduler, UUID).

