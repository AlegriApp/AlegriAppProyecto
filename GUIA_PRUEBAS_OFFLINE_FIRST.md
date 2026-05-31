# Guía de pruebas Offline First — AlegriAPP

Documento operativo para probar la app en un dispositivo / emulador real, paso a paso.
Cada prueba indica **qué hacer**, **qué comando ejecutar** y **qué resultado esperar**.

> Si una prueba falla, anótala con la sección que falló y el resultado obtenido.

---

## 0. Preparación del entorno (una sola vez)

### 0.1 — Aplicar los SQL en Supabase

Entrar a Supabase → **SQL Editor** → **New query** → pegar y ejecutar **en este orden**:

| # | Archivo | ¿Qué hace? |
|---|---------|-----------|
| 1 | `supabase_fix_insert_rls.sql` | Habilita INSERT en `asistencias` y `calificaciones` |
| 2 | `supabase_add_uuid_columns.sql` | Añade columna `uuid` a las 4 tablas mobile |
| 3 | `supabase_grant_select_incidentes.sql` | Habilita PULL de incidentes (SELECT only) |

**Verificación rápida** (pegar en SQL Editor):

```sql
-- Confirmar columnas uuid
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE column_name = 'uuid'
  AND table_name IN ('estudiantes','asistencias','calificaciones','incidentes')
ORDER BY table_name;

-- Confirmar políticas RLS de incidentes (debe haber SELECT, NO INSERT)
SELECT tablename, policyname, cmd FROM pg_policies
WHERE tablename IN ('incidentes','tipos_incidente');
```

**Esperado:** 4 filas en la primera query, política `mobile_select_incidentes` con `cmd = SELECT` en la segunda. Si aparece `INSERT`, algo se aplicó mal.

### 0.2 — Configurar `local.properties`

En `local.properties` (raíz del proyecto):

```properties
SUPABASE_URL=https://TU_PROYECTO.supabase.co/rest/v1/
SUPABASE_KEY=eyJxxx...   # anon key
TELEGRAM_BOT_TOKEN=xxxx:yyyy
TELEGRAM_CHAT_ID=-100123456789
SUPABASE_DEFAULT_CURSO_ID=1
SUPABASE_DEFAULT_MATERIA_ID=1
SUPABASE_DEFAULT_TIPO_EVALUACION_ID=6
SUPABASE_DEFAULT_PERIODO_ID=1
```

### 0.3 — Compilar e instalar

```bash
cd C:\Users\aleja\Desktop\AlegriaAppMovil\AlegriAppProyecto
./gradlew assembleDebug
./gradlew installDebug
# o si prefieres adb directo:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Esperado:** `BUILD SUCCESSFUL` y app instalada. Abrir la app debe mostrar la pantalla Home sin crashes.

### 0.4 — Herramientas que vas a necesitar abiertas

| Herramienta | Para qué |
|-------------|----------|
| Android Studio → **App Inspection → Database Inspector** | Ver tablas de Room en vivo |
| Android Studio → **Logcat** filtro `SyncWorker|SyncRepository|NetworkMonitor` | Ver logs del sync |
| **Supabase Dashboard → Table Editor** | Ver filas que llegan al servidor |
| Terminal con `adb` accesible | Comandos rápidos |

---

## PRUEBA 1 — Migración Room v5 → v6 (no pierde datos)

> **Solo aplica si ya tenías una versión previa instalada con datos.** Si es instalación limpia, salta a Prueba 2.

### Pasos

1. **Antes de actualizar la app**, abrir el Database Inspector en la versión vieja y anotar:
   - 1 `id` de asistencia (ej: `id=42`).
   - 1 `id` de calificación.
   - 1 `id` de incidente.
   - Cuántas filas hay en total por tabla.
2. Instalar el build nuevo SIN desinstalar el anterior:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Abrir la app. Ir a Asistencias, Calificaciones, Incidentes. Verificar que los datos siguen ahí.
4. Volver a abrir Database Inspector → conectar a `alegriapp.db`.
5. Ejecutar en la consola SQL del inspector:
   ```sql
   SELECT id, uuid, sync_status, sincronizacion_pendiente FROM asistencias WHERE id = 42;
   SELECT COUNT(*) FROM asistencias;
   SELECT COUNT(*) FROM calificaciones;
   SELECT COUNT(*) FROM incidentes;
   ```

### Resultado esperado

- ✅ Los conteos coinciden con lo anotado antes de actualizar.
- ✅ La fila `id=42` ahora tiene un `uuid` no vacío (algo como `a1b2c3d4-e5f6-4789-ab01-234567890abc`).
- ✅ Si antes tenía `sincronizacion_pendiente=1`, ahora `sync_status='IDLE'`.
- ✅ Si antes tenía `sincronizacion_pendiente=0`, ahora `sync_status='SUCCESS'`.

### Si falla

→ La app probablemente crashea al arrancar con `IllegalStateException: A migration from 5 to 6 was required but not found`. Revisar que `Migration_5_6` esté en `addMigrations(...)` en `AppModule.kt`.

---

## PRUEBA 2 — Guardar asistencia sin internet

### Pasos

1. Activar **modo avión** en el dispositivo (o `adb shell svc wifi disable && adb shell svc data disable`).
2. Abrir la app → pantalla **Asistencia**.
3. Verificar que aparece el banner amarillo "Sin conexión. Mostrando datos guardados localmente."
4. Marcar a todos los estudiantes con cualquier estado (Presente/Atrasado/etc.).
5. Pulsar **Guardar asistencia**.
6. Esperado: mensaje verde "Asistencia guardada correctamente." sin error.
7. Cerrar la app completamente desde el conmutador de tareas (no solo minimizar).
8. Reabrir la app (todavía sin red). Ir a Asistencia → los datos siguen ahí.

### Verificar en Database Inspector

```sql
SELECT uuid, estudiante_id, fecha, estado, sync_status, sincronizacion_pendiente
FROM asistencias
WHERE fecha = date('now')
ORDER BY id DESC LIMIT 10;
```

### Resultado esperado

- ✅ Cada asistencia recién guardada tiene `sync_status = 'IDLE'`.
- ✅ Cada una tiene un `uuid` único (UUID v4 con guiones).
- ✅ `sincronizacion_pendiente = 1` (compatibilidad).
- ✅ El banner offline mostraba "Sin conexión...".

---

## PRUEBA 3 — Recuperar conexión → sync automático

### Pasos

1. **Estando con datos pendientes de la Prueba 2**, desactivar modo avión.
2. Esperar **30–60 segundos** sin tocar nada.
3. Filtrar Logcat por `SyncWorker`:
   ```bash
   adb logcat | grep -E "SyncWorker|SyncRepository"
   ```
   Esperado ver líneas tipo `enqueued`, `running`, `SUCCESS`.

### Verificar en Database Inspector (mobile)

```sql
SELECT uuid, sync_status, remote_id, server_updated_at
FROM asistencias
WHERE fecha = date('now') AND sync_status != 'SUCCESS';
```

**Esperado:** 0 filas (todas pasaron a `SUCCESS`).

```sql
SELECT uuid, sync_status, remote_id
FROM asistencias
WHERE fecha = date('now')
ORDER BY id DESC LIMIT 10;
```

**Esperado:** todas con `sync_status = 'SUCCESS'` y `remote_id` poblado (un número entero).

### Verificar en Supabase Dashboard

Tabla `asistencias` → filtrar por fecha de hoy → buscar los `uuid` que viste localmente.

**Esperado:**
- ✅ Filas llegaron con el mismo `uuid` que se generó en mobile.
- ✅ El `id` SERIAL coincide con `remote_id` local.
- ✅ `updated_at` está poblado por Postgres.

---

## PRUEBA 4 — Idempotencia del upsert (sin duplicados)

### Pasos

1. Reabrir la app con red activa.
2. Ir a Asistencia → modificar el estado de **el mismo estudiante** de hoy (de Presente a Atrasado por ejemplo).
3. Pulsar Guardar.
4. Esperar sync (~30s).

### Verificar en Supabase

```sql
SELECT uuid, estudiante_id, fecha, estado, updated_at
FROM asistencias
WHERE estudiante_id = 1 AND fecha = CURRENT_DATE;
```

### Resultado esperado

- ✅ **1 sola fila** (no 2). El UPSERT por `uuid` evitó el duplicado.
- ✅ El `estado` refleja el último cambio ("atrasado").
- ✅ `updated_at` se actualizó.

### Si falla

→ Aparecen 2 filas con el mismo `(estudiante_id, fecha)` → el `on_conflict=uuid` no se está enviando. Revisar `SupabaseApiService.upsertAsistencia`.

---

## PRUEBA 5 — PULL de incidentes desde Supabase

### Pasos

1. En Supabase SQL Editor, insertar un incidente manualmente:
   ```sql
   INSERT INTO incidentes (estudiante_id, tipo_incidente_id, descripcion, fecha_hora, nivel_gravedad, estado)
   VALUES (1, 1, 'Incidente de prueba PULL desde mobile', NOW(), 'medio', 'abierto')
   RETURNING id, uuid;
   ```
   Anotar el `uuid` que devuelve.
2. En la app (con red): pantalla **Incidentes**.
3. Esperar 30–60s al periodic sync, o forzar el sync cerrando y reabriendo la app.

### Verificar en Database Inspector

```sql
SELECT uuid, descripcion, sync_status, local_only, server_updated_at
FROM incidentes
WHERE descripcion LIKE '%PULL%';
```

### Resultado esperado

- ✅ El incidente apareció en la pantalla Incidentes de la app.
- ✅ En Room: `local_only = 0`, `sync_status = 'SUCCESS'`, `server_updated_at` poblado.
- ✅ El `uuid` local coincide con el que devolvió Postgres.

### Repetir el sync (verificar que no duplica)

Forzar otro sync (cerrar/abrir app). Repetir la query anterior.

**Esperado:** sigue siendo 1 sola fila local con ese uuid (no se duplicó).

---

## PRUEBA 6 — Confirmación: mobile NO escribe incidentes

### Pasos

1. Activar modo avión.
2. Ir a **Incidentes** → tocar **+ Registrar manualmente**.
3. Crear un incidente nuevo con estudiante manual: "Juan Prueba PULL".
4. Pulsar Guardar.
5. Desactivar modo avión y esperar 60s al sync.

### Verificar en Supabase

```sql
SELECT * FROM incidentes
WHERE descripcion LIKE '%[poner aquí la descripción que usaste]%'
ORDER BY created_at DESC LIMIT 5;
```

### Resultado esperado

- ✅ **NO aparece ninguna fila** en Supabase. El incidente quedó solo local.

### Verificar en Database Inspector (mobile)

```sql
SELECT uuid, descripcion, local_only, sync_status
FROM incidentes
WHERE descripcion LIKE '%[tu descripción]%';
```

- ✅ Existe localmente con `local_only = 1`.

### Verificación adicional (estática)

```bash
cd app/src/main/java/com/example/myapplication
grep -rn "insertIncidente\|upsertIncidente" .
```

**Esperado:** 0 matches. Confirma que mobile no tiene método para escribir incidentes a Supabase.

---

## PRUEBA 7 — IDs offline-safe (estudiantes manuales)

### Pasos

1. Modo avión activado.
2. Pantalla Incidentes → registrar incidente con estudiante manual: nombre "Ana Test Negativos".
3. Guardar.

### Verificar en Database Inspector

```sql
SELECT id, uuid, fullName FROM students WHERE fullName LIKE '%Ana Test%';
```

### Resultado esperado

- ✅ El `id` es un número **negativo grande** (algo como `-1730384821000`).
- ✅ El `uuid` está poblado.
- ✅ No colisiona con ningún estudiante demo (que tienen `id` 1–8).

---

## PRUEBA 8 — Resolución de conflictos LWW (incidentes)

### Pasos

1. Tomar el `uuid` del incidente de la Prueba 5.
2. En Supabase, modificarlo:
   ```sql
   UPDATE incidentes
   SET descripcion = 'PULL editado desde Postgres'
   WHERE uuid = '[pega aquí el uuid]';
   ```
3. En la app: forzar sync (cerrar/abrir).

### Verificar en mobile

Ir a Incidentes → buscar el incidente. Debería mostrar la descripción nueva.

```sql
SELECT uuid, descripcion, server_updated_at
FROM incidentes
WHERE uuid = '[el uuid]';
```

### Resultado esperado

- ✅ La descripción local cambió a "PULL editado desde Postgres".
- ✅ `server_updated_at` se actualizó al nuevo valor.

### Caso adverso: incidente local NO debe sobrescribirse

1. El incidente local creado en Prueba 6 (`local_only=1`) **no debe ser tocado** por ningún PULL.
2. Verificar que su descripción y datos siguen iguales tras el sync.

---

## PRUEBA 9 — Estado "ERROR" tras fallo de servidor

### Pasos

1. Editar temporalmente `local.properties` para **romper** Supabase:
   ```properties
   SUPABASE_KEY=BROKEN_KEY
   ```
2. Recompilar:
   ```bash
   ./gradlew installDebug
   ```
3. Modo avión OFF. Guardar una asistencia nueva.
4. Esperar 30–60s.

### Verificar en Database Inspector

```sql
SELECT uuid, sync_status, sync_error, last_sync_attempt
FROM asistencias
WHERE sync_status = 'ERROR';
```

### Resultado esperado

- ✅ La fila quedó con `sync_status = 'ERROR'`.
- ✅ `sync_error` tiene un mensaje (probablemente 401/403).
- ✅ `last_sync_attempt` tiene un timestamp reciente.

### Recuperación

1. Restaurar el `SUPABASE_KEY` correcto.
2. Recompilar e instalar.
3. Abrir app, esperar sync.

**Esperado:** la fila pasa de `ERROR` → `SUCCESS` y `sync_error` se limpia.

---

## PRUEBA 10 — Sync periódico cada 15 min (background)

### Pasos

1. Cerrar la app por completo.
2. Esperar ~15 minutos (o forzar con adb).
3. Forzar manualmente que WorkManager dispare ya:
   ```bash
   adb shell cmd jobscheduler run -f com.example.myapplication 999
   ```
   (El número de job ID varía; alternativa: usar el WorkManager Inspector de Android Studio.)
4. Filtrar Logcat:
   ```bash
   adb logcat | grep -E "SyncWorker|alegriapp_sync"
   ```

### Resultado esperado

- ✅ Ver líneas tipo `SyncWorker doWork` o `WM-WorkerWrapper`.
- ✅ El sync se ejecutó sin que la app esté abierta.

### Verificar con WorkManager Inspector

Android Studio → **App Inspection → Background Task Inspector** → seleccionar el proceso de la app.

- ✅ Debe haber un PeriodicWorkRequest con tag `alegriapp_sync_periodic` en estado ENQUEUED.

---

## PRUEBA 11 — "Última sincronización: hace N min"

### Pasos

1. Tener un sync exitoso reciente.
2. Activar modo avión.
3. Abrir la app.

### Resultado esperado

- ✅ El banner offline muestra: `Sin conexión. Mostrando datos guardados localmente.` y debajo `Última sincronización: hace X min`.
- ✅ El timestamp persiste tras reiniciar la app.

### Verificar en DataStore (alternativa)

```bash
adb shell run-as com.example.myapplication \
  cat /data/data/com.example.myapplication/files/datastore/alegriapp_sync_prefs.preferences_pb
```

(El archivo es binario protobuf, pero se ve el long del epoch.)

---

## PRUEBA 12 — Calificaciones offline + sync

Análogo a Pruebas 2–3 pero en pantalla Calificaciones:

1. Modo avión ON.
2. Editar 2 calificaciones en pantalla Calificaciones.
3. Guardar.
4. Modo avión OFF, esperar 60s.
5. Verificar en Supabase tabla `calificaciones` que las filas llegaron con su `uuid`.

---

## PRUEBA 13 — Telegram sigue funcionando (no romper nada existente)

1. Con red activa, ir a Asistencia → marcar todos → **Enviar reporte**.
2. Verificar que llega mensaje al Telegram configurado.
3. Repetir con Calificaciones → **Enviar boletín**.
4. Repetir con Incidentes → **Enviar reporte**.

**Esperado:** los 3 envíos funcionan como antes de Offline First.

---

## PRUEBA 14 — Sin pérdida de datos al reinstalar (data extraction)

1. Crear varios registros offline.
2. Desinstalar app:
   ```bash
   adb uninstall com.example.myapplication
   ```
3. Reinstalar.

**Esperado** (comportamiento normal de Android): los datos locales se pierden porque la desinstalación borra `/data/data/`. Esta prueba NO valida persistencia tras desinstalación, valida que **una actualización (install -r)** no pierde nada — eso es la Prueba 1.

---

## PRUEBA 15 — Comportamiento sin Supabase configurado

1. En `local.properties`, dejar `SUPABASE_URL` y `SUPABASE_KEY` vacíos.
2. Recompilar e instalar.
3. Usar la app normalmente.

**Esperado:**
- ✅ La app arranca, muestra datos demo locales.
- ✅ Guardar funciona (Room local).
- ✅ Los syncs son `SyncOutcome.Skipped("Supabase no configurado...")` — no crashea.
- ✅ Logcat muestra mensajes de "Skipped" en lugar de errores.

---

## Plantilla de reporte de bugs

Si una prueba falla, copiar esta plantilla al PR / issue:

```
**Prueba que falló:** [número y nombre]
**Dispositivo:** [modelo Android + versión]
**Pasos exactos:**
1.
2.
3.

**Esperado:**

**Obtenido:**

**Logcat relevante (filtro SyncWorker|SyncRepository):**
```
[pegar últimas 20 líneas]
```

**Query Database Inspector relevante:**
```sql
[la query y su resultado]
```

**Estado en Supabase:**
[captura o query resultante]
```

---

## Atajos / comandos útiles

```bash
# Limpiar logcat antes de probar
adb logcat -c

# Ver solo logs de la app
adb logcat | grep com.example.myapplication

# Filtrar por componentes de sync
adb logcat | grep -E "SyncWorker|SyncRepository|NetworkMonitor|WM-"

# Forzar sin red
adb shell svc wifi disable && adb shell svc data disable

# Restaurar red
adb shell svc wifi enable && adb shell svc data enable

# Ver tablas de Room sin Inspector (necesita root o emulator):
adb shell run-as com.example.myapplication \
  sqlite3 /data/data/com.example.myapplication/databases/alegriapp.db \
  "SELECT uuid, sync_status FROM asistencias LIMIT 10;"

# Borrar datos de la app sin desinstalar (resetear estado limpio)
adb shell pm clear com.example.myapplication

# Verificar tareas de WorkManager
adb shell dumpsys jobscheduler | grep -A 3 alegriapp
```

---

## Resumen de aprobación

Para considerar Offline First **aprobado en QA**, deben pasar:

- [ ] Prueba 1 (migración no pierde datos) — si aplica
- [ ] Prueba 2 (guardar offline)
- [ ] Prueba 3 (sync auto al recuperar red)
- [ ] Prueba 4 (idempotencia upsert)
- [ ] Prueba 5 (PULL incidentes funciona)
- [ ] Prueba 6 (NO se hace POST de incidentes desde mobile) — **crítico**
- [ ] Prueba 7 (IDs negativos para estudiantes manuales)
- [ ] Prueba 8 (LWW conflictos)
- [ ] Prueba 9 (estado ERROR + recuperación)
- [ ] Prueba 10 (WorkManager periódico)
- [ ] Prueba 11 (timestamp última sync persistente)
- [ ] Prueba 12 (calificaciones offline)
- [ ] Prueba 13 (Telegram no se rompió)
- [ ] Prueba 15 (sin Supabase configurado no crashea)

Mínimo aceptable: las 14 anteriores deben pasar antes de mergear a `main`.
