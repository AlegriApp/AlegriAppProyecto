# Checklist de validación — Offline First

Documento de pruebas manuales tras la implementación de las 13 fases del plan Offline First.

**Pre-requisitos antes de validar:**
- Aplicar `supabase_add_uuid_columns.sql` en Supabase SQL Editor.
- Aplicar `supabase_grant_select_incidentes.sql` en Supabase SQL Editor.
- Aplicar (si no estaba) `supabase_fix_insert_rls.sql`.
- Configurar `SUPABASE_URL`, `SUPABASE_KEY`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` en `local.properties`.
- Instalar build debug de la app (`./gradlew assembleDebug`).

---

## 1. Migración Room v5 → v6

- [ ] Instalar primero un build previo (versión 5) con datos: 2-3 asistencias, 2-3 calificaciones, 1 incidente.
- [ ] Actualizar al build nuevo (versión 6) sin desinstalar.
- [ ] Verificar que **no se pierden datos** existentes.
- [ ] Verificar que cada fila preexistente recibió un `uuid` único (consulta DB Inspector o adb pull).
- [ ] Verificar que `sincronizacion_pendiente=1` → `sync_status='IDLE'`, y `=0` → `'SUCCESS'`.

## 2. Guardar sin internet

- [ ] Activar modo avión.
- [ ] Pantalla **Asistencia**: marcar a todos los estudiantes y pulsar Guardar.
- [ ] Confirmar que aparece banner "Sin conexión".
- [ ] Confirmar que el guardado tuvo éxito (mensaje verde).
- [ ] Cerrar la app completamente.
- [ ] Reabrir la app sin red. Los datos guardados siguen visibles.
- [ ] Pantalla **Calificaciones**: editar 2-3 notas y guardar offline.
- [ ] Pantalla **Incidentes**: registrar un incidente offline (con estudiante manual).
- [ ] Confirmar que se generan IDs locales **negativos** (no chocan con SERIAL servidor).

## 3. Recuperar conexión → sync automático

- [ ] Con datos pendientes de los pasos anteriores, desactivar modo avión.
- [ ] Esperar 5–30 segundos.
- [ ] Verificar en Supabase Dashboard → `asistencias`: los registros llegaron con el `uuid` generado en mobile.
- [ ] Verificar en Supabase Dashboard → `calificaciones`: idem.
- [ ] Verificar en Supabase Dashboard → `incidentes`: **NO debe haber filas nuevas insertadas por mobile** (decisión PULL only).
- [ ] Verificar que `sync_status` en Room cambió a `SUCCESS`.
- [ ] Verificar que `remote_id` está poblado con el `id` SERIAL del servidor.

## 4. Idempotencia de upsert

- [ ] Editar la misma asistencia y guardar de nuevo.
- [ ] Verificar que en Supabase **NO se crea un duplicado** — la fila existente se actualiza (UPSERT vía `on_conflict=uuid`).
- [ ] Verificar que `updated_at` en servidor cambia.

## 5. PULL de incidentes desde Supabase

- [ ] Insertar manualmente un incidente nuevo en Supabase SQL Editor:
      ```sql
      INSERT INTO incidentes (estudiante_id, tipo_incidente_id, descripcion, fecha_hora, nivel_gravedad, estado)
      VALUES (1, 1, 'Incidente prueba PULL', NOW(), 'medio', 'abierto');
      ```
- [ ] Abrir la app con red activa o pulsar "Sincronizar".
- [ ] Esperar al PeriodicWorker o forzar nuevo sync.
- [ ] Verificar que el incidente aparece en la pantalla Incidentes.
- [ ] Verificar que tiene `localOnly = false` en Room (DB Inspector).
- [ ] Re-sincronizar: confirmar que **no se duplica** (idempotencia por `uuid`).

## 6. Conflictos LWW

- [ ] Editar en Supabase un incidente existente (cambiar descripción).
- [ ] Forzar sync en mobile.
- [ ] El incidente local debe actualizarse (LWW: `server_updated_at` mayor gana).
- [ ] Crear un nuevo incidente local (queda con `localOnly=true`).
- [ ] Forzar sync. El incidente local **NO debe ser sobrescrito** por el PULL (porque tiene UUID distinto al servidor y `localOnly=true`).

## 7. Soft delete

- [ ] (Opcional — futuro) Eliminar una asistencia con `softDelete(uuid)`.
- [ ] Verificar que `is_deleted=1` en Room.
- [ ] Verificar que la fila **no aparece** en la lista de la UI.
- [ ] Verificar que sigue contando como pendiente de sync hasta confirmar push del DELETE.

## 8. Estado de sincronización en UI

- [ ] Crear asistencias offline → la UI debe poder mostrar un badge "Pendiente" amarillo (cuando se integre `SyncStatusBadge` en las filas).
- [ ] Tras sync exitoso, badge cambia a "Sincronizado" verde.
- [ ] Forzar error (apagar Supabase): badge "Error" rojo con tooltip de mensaje.

## 9. Última sincronización exitosa

- [ ] Tras un `syncAll()` exitoso, abrir la app sin red.
- [ ] `OfflineBanner` debe mostrar "Última sincronización: hace N min".
- [ ] Verificar que el valor persiste tras reiniciar la app.

## 10. WorkManager

- [ ] Verificar con `adb shell dumpsys jobscheduler | grep alegriapp` (o el inspector de WorkManager de Android Studio) que hay un PeriodicWorkRequest activo cada 15 min.
- [ ] Cerrar la app completamente. Activar red. Esperar 15 min. Verificar que el sync ocurrió en background (ver logs de Supabase o Logcat).
- [ ] Forzar un error de red (apagar wifi a mitad del worker). Verificar que `runAttemptCount` sube y se aplica backoff exponencial.

## 11. Decisión del equipo: incidentes PULL only

- [ ] Confirmar grep en código: `SupabaseApiService.kt` **NO** tiene método `insertIncidente` ni `upsertIncidente`. ✅ verificado: solo `getIncidentes` y `getIncidentesUpdatedSince`.
- [ ] Confirmar que `SyncRepositoryImpl.syncPendingRecords()` **NO** itera sobre incidentes pendientes para push. ✅ verificado: comentario explícito "Incidentes intencionalmente OMITIDOS aquí".
- [ ] Crear un incidente local. Esperar sync. Confirmar que **NO** llega a Supabase.
- [ ] El incidente queda en Room con `localOnly = true`, `sync_status = IDLE` (o el que tenga por defecto), pero nunca se intenta push.

## 12. Sin pérdida de datos al migrar

- [ ] Antes de migrar: anotar 3 IDs específicos de Room (asistencia, calificación, incidente).
- [ ] Migrar a v6.
- [ ] Verificar que esos 3 IDs siguen existiendo con sus datos intactos.
- [ ] Verificar que ahora tienen `uuid` rellenado.
- [ ] Verificar que `sync_status` corresponde al valor anterior de `sincronizacion_pendiente`.

## 13. UUID en mobile (estudiantes manuales)

- [ ] Crear un incidente con un estudiante nuevo manual (vía formulario manual).
- [ ] Verificar en DB Inspector que el estudiante recibió:
      - `id` Long **negativo** (basado en timestamp).
      - `uuid` String generado por `UUID.randomUUID()`.
- [ ] Verificar que el incidente referencia ese `studentId` negativo correctamente.

## 14. Compatibilidad con código viejo

- [ ] Verificar que pantallas Asistencias y Calificaciones siguen funcionando como antes.
- [ ] Verificar que el envío por Telegram sigue funcionando.
- [ ] Verificar que el OCR ML Kit sigue funcionando.
- [ ] Verificar que el seed inicial (8 estudiantes demo) sigue arrancando si la DB está vacía.

## 15. Build sano

- [x] `./gradlew assembleDebug` completa sin errores. ✅ verificado al cierre de Fase 12.
- [ ] `./gradlew test` (si aplica) pasa.

---

## Comandos útiles

```bash
# Forzar un sync inmediato desde adb (requiere debug build):
adb shell am broadcast -a com.example.myapplication.SYNC_NOW

# Ver el estado de WorkManager:
adb shell dumpsys jobscheduler

# Inspector DB:
# Android Studio → View → Tool Windows → Database Inspector

# Logs de sincronización:
adb logcat | grep -E "(SyncWorker|SyncRepository|NetworkMonitor)"
```

## Plan de rollback (si algo sale mal)

1. La migración Room v5→v6 es **no destructiva**: añade columnas, no las borra. Si hay problemas, se puede crear Migration_6_5 que las elimine.
2. Las nuevas columnas en Postgres (`uuid`) son **no destructivas**: pueden eliminarse sin afectar PKs ni FKs:
   ```sql
   ALTER TABLE estudiantes DROP COLUMN IF EXISTS uuid;
   -- etc.
   ```
3. Los archivos eliminados en Fase 0 (`IncidentSyncRequest.Powershell.kt` y 3 stubs OCR vacíos) estaban vacíos: cero impacto al borrarse.
4. WorkManager puede cancelarse: `SyncScheduler.cancelAll()`.
