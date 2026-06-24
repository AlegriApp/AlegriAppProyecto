# AlegriAPP — Plan de implementación Offline First

Documento maestro para llevar el proyecto desde su estado actual (Offline First parcial, ~60–70 %) a un Offline First completo: cola persistente, sincronización en background con WorkManager, resolución de conflictos por `updated_at`, soft-delete, manejo de estados de sincronización y UI consciente del modo offline.

**Relacionado con:** `PLAN_CORRECCION_FASES.md`, `PLAN_MODELOS_ASISTENCIAS_CALIFICACIONES.md`, `CONFIG_PENDIENTE_EQUIPO.md`, `AlegriAPP_Guia_Estructura_Proyecto.md`

**Fecha de elaboración:** 2026-05-30
**Última re-auditoría:** 2026-05-30 (3ª revisión — incorpora esquema real de Supabase y estrategia UUID definitiva)
**Estado:** Planificado — pendiente de ejecución por fases
**Autor del diagnóstico:** Análisis automatizado del proyecto

---

## 0. Historial de auditorías

| Fecha | Verdicto | Observaciones |
|-------|----------|---------------|
| 2026-05-30 (1ª) | Parcial (~60–70 %) | Auditoría inicial. Plan generado |
| 2026-05-30 (2ª) | Parcial (~60–70 %) — **sin cambios** | Re-auditoría tras edición. Cambios recientes no afectan al estado Offline First (ver §1.5) |
| 2026-05-30 (3ª) | Parcial (~60–70 %) — **plan reescrito** | Se incorporó el esquema real de Postgres (`alegriapp_create_tables.txt`, `alegriapp_descripcion_tablas.txt`). El servidor ya tiene `created_at`/`updated_at`/`deleted_at`/`auditoría`/`sincronizacion_pendiente`. UUID confirmado como estrategia. Ver §1.6 y §10 |
| 2026-05-30 (4ª) | **Implementado** — 13 fases ejecutadas | Plan completo aplicado. Build verde. Migración Room v5→v6 con backfill. WorkManager + SyncScheduler activos. Incidentes confirmados PULL-only. SQL adicional generado: `supabase_add_uuid_columns.sql`, `supabase_grant_select_incidentes.sql`. Ver `CHECKLIST_VALIDACION_OFFLINE_FIRST.md` |
| 2026-05-30 (5ª) | **F13 añadida** — auto-envío incidentes Telegram con notificación sistema | Al recuperar conexión, los incidentes locales con `sent=0` se envían automáticamente por Telegram desde `SyncWorker`. Notificación foreground del sistema con progreso "X/N enviados" + notificación de resultado fuera de la app. Permisos POST_NOTIFICATIONS + FOREGROUND_SERVICE_DATA_SYNC. Build verde |
| 2026-05-30 (6ª) | **F14 — Decisión revertida** | El equipo confirmó que mobile SÍ debe enviar incidentes a Supabase además de Telegram. Implementado: `upsertIncidente` (Supabase) + `IncidenteInsertDto` + mapper enum→catálogo + rama push en `SyncRepositoryImpl` + SQL `supabase_grant_insert_incidentes.sql` + BuildConfig `SUPABASE_DEFAULT_TIPO_INCIDENTE_ID`. Bloqueo defensivo: incidentes con `studentId<=0` (estudiante local) no se envían — quedan como ERROR explicado. Build verde |

---

## 1. Diagnóstico resumido

### 1.1 ¿El proyecto ya implementa Offline First?

**Parcialmente (~60–70 % de madurez).** Existe Room + clean architecture + sincronización con Supabase + detección de conexión, pero faltan piezas clave para considerarlo Offline First robusto. **La re-auditoría del 2026-05-30 confirma que ninguna de las fases planificadas ha sido implementada todavía.**

### 1.2 Qué SÍ existe (reutilizable)

| Capa | Evidencia | Archivo |
|------|-----------|---------|
| Persistencia local | Room v5 con 4 entidades | [AppDatabase.kt](app/src/main/java/com/example/myapplication/data/local/AppDatabase.kt) |
| Seed inicial | Inserta demo si DB está vacía | [DatabaseSeeder.kt](app/src/main/java/com/example/myapplication/data/local/DatabaseSeeder.kt) |
| Fuente única de verdad | Repos exponen `Flow` desde DAO | [AttendanceRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/AttendanceRepositoryImpl.kt), [GradeRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/GradeRepositoryImpl.kt), [IncidentRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/IncidentRepositoryImpl.kt), [StudentRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/StudentRepositoryImpl.kt) |
| Sincronización | Pull estudiantes + Push asistencias y calificaciones | [SyncRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/SyncRepositoryImpl.kt) |
| Resultado de sync | `sealed class` Success/Skipped/Failure | [SyncOutcome.kt](app/src/main/java/com/example/myapplication/domain/model/sync/SyncOutcome.kt) |
| Detección de red | `NetworkCallback` + `Flow<Boolean>` | [NetworkMonitor.kt](app/src/main/java/com/example/myapplication/core/network/NetworkMonitor.kt) |
| Estado pendiente | Columna `sincronizacion_pendiente` en 3 entidades | Entities + DAOs |
| UI consciente | `OfflineBanner` reutilizable | [OfflineBanner.kt](app/src/main/java/com/example/myapplication/presentation/common/OfflineBanner.kt) |
| Separación de capas | `data/local` ≠ `data/remote` ≠ `data/repository` ≠ `domain` ≠ `presentation` | Estructura del proyecto |
| Servicio offline auxiliar (nuevo) | Heurística pura local de transcripción texto→asistencia | [AttendanceTranscriptionService.kt](app/src/main/java/com/example/myapplication/domain/service/AttendanceTranscriptionService.kt) — añadido tras la 1ª auditoría |

### 1.3 Qué FALTA

| # | Brecha | Severidad |
|---|--------|-----------|
| 1 | Sin sincronización automática al recuperar conexión | Alta |
| 2 | Sin WorkManager ni cola persistente | Alta |
| 3 | Incidentes no se sincronizan con Supabase (solo Telegram) | Alta |
| 4 | IDs locales (`max+1`) chocan con IDs de Supabase | Alta |
| 5 | Sin soporte para eliminaciones offline (tombstones) | Media |
| 6 | Sin `updated_at` para resolver conflictos | Media |
| 7 | Estado de sync binario (`syncPending`), no distingue PENDING vs FAILED | Media |
| 8 | `fallbackToDestructiveMigration()` borra datos en cada cambio de esquema | Media |
| 9 | Sin DataStore para `last_successful_sync` ni preferencias | Baja |
| 10 | Sin paginación / `updated_since` en pulls | Baja |
| 11 | Archivo basura: `IncidentSyncRequest.Powershell.kt` (vacío, nombre erróneo) | Baja |

### 1.4 Riesgos al implementar

| Riesgo | Mitigación |
|--------|------------|
| Romper IDs locales/remotos | UUID o rango negativo para entidades creadas offline + remap tras sync |
| Duplicados por reintentos no idempotentes | Upsert por clave natural (estudiante+fecha) o `Prefer: resolution=merge-duplicates` |
| Migraciones destructivas borran datos pendientes | Reemplazar `fallbackToDestructiveMigration()` por `Migration` explícitas (crítico antes de cualquier cambio) |
| Conflictos last-write-wins ciegos | Comparar `updated_at` local vs remoto |
| WorkManager consume batería/datos | Constraints: `NetworkType.CONNECTED`, backoff exponencial |
| RLS de Supabase bloquea inserts | Validar `supabase_fix_insert_rls.sql` aplicado antes de robustecer push |
| Stubs vacíos en `presentation/components/` podrían colisionar con composables planeados (`SyncStatusBadge`, `PendingSyncCounter`) | Decidir en Fase 0 si esos archivos se completan, se renombran o se borran (ver §1.5) |
| Stubs vacíos en `presentation/ocr/` indican feature paralela en desarrollo | Coordinar con quien trabaja esa rama antes de tocar `presentation/` |

### 1.5 Cambios recientes detectados (re-auditoría 2026-05-30)

Comparando el árbol actual con la primera auditoría, los cambios encontrados **no avanzan ninguna fase del plan Offline First**:

**Añadidos funcionales (no Offline First persistente):**

| Archivo | Tipo | Relación con Offline First |
|---------|------|---------------------------|
| [domain/service/AttendanceTranscriptionService.kt](app/src/main/java/com/example/myapplication/domain/service/AttendanceTranscriptionService.kt) | Servicio puro local | Offline-capable (no usa red) pero no aporta infraestructura de persistencia/sync |
| `provideAttendanceTranscriptionService()` en [AppModule.kt:147](app/src/main/java/com/example/myapplication/core/di/AppModule.kt:147) | DI | Wire-up del anterior |
| [IncidentViewModel.kt](app/src/main/java/com/example/myapplication/presentation/incidents/IncidentViewModel.kt) creció ~240 líneas | Validaciones / manejo manual de estudiantes | Bug `nextLocalStudentId` línea 771 **persiste** (Fase 9) |

**Stubs vacíos (0 bytes) — posible WIP de otra rama:**

- `presentation/ocr/OcrAttendanceScreen.kt`
- `presentation/ocr/OcrUiState.kt`
- `presentation/ocr/OcrViewModel.kt`
- `presentation/components/AppScaffold.kt`
- `presentation/components/ErrorContent.kt`
- `presentation/components/LoadingContent.kt`
- `presentation/components/PrimaryActionButton.kt`
- `presentation/components/StudentCard.kt`

**Deuda no resuelta:**

- [IncidentSyncRequest.Powershell.kt](app/src/main/java/com/example/myapplication/data/remote/dto/IncidentSyncRequest.Powershell.kt) (0 bytes, basura) **sigue presente**
- `AppDatabase.version = 5` y `fallbackToDestructiveMigration()` **sin cambios**
- `SupabaseApiService` **sin endpoint de incidentes**
- `AlegriApp.kt` **sin observador `NetworkMonitor.isOnline`** para sync automático

**Conclusión de la re-auditoría:** el plan original sigue siendo válido al 100 %. Solo se ajusta la Fase 0 para incluir la decisión sobre los stubs vacíos.

### 1.6 Hallazgos del esquema real de Supabase (3ª auditoría — 2026-05-30)

Tras analizar [alegriapp_create_tables.txt](alegriapp_create_tables.txt) y [alegriapp_descripcion_tablas.txt](alegriapp_descripcion_tablas.txt), **el servidor está mucho más preparado para Offline First de lo asumido**.

**Lo que YA EXISTE en Postgres:**

| Característica | Estado |
|----------------|--------|
| `created_at`, `updated_at`, `deleted_at` (TIMESTAMPTZ) en tablas principales | ✅ Ya existen con `DEFAULT NOW()` |
| `created_by`, `updated_by` para auditoría | ✅ Ya existen |
| Soft-delete (`deleted_at` IS NOT NULL) | ✅ Patrón ya implementado |
| Tabla `sincronizacion_pendiente` server-side con `tabla_afectada`, `id_local`, `id_remoto`, `accion`, `payload`, `estado`, `intentos`, `mensaje_error`, `ultimo_intento` | ✅ Existe |
| Tabla `auditoria` con `valor_anterior`/`valor_nuevo` | ✅ Existe |
| Columna `sincronizacion_pendiente BOOLEAN` en `asistencias` con índice parcial | ✅ Existe |
| Patrón `ruta_local` + `url_remota` para evidencias | ✅ Existe con CHECK |
| Claves naturales únicas (`codigo_institucional`, `(estudiante,curso,fecha,materia)`, etc.) | ✅ Existen |
| Columna `uuid` en tablas | ❌ **No existe.** PKs son `SERIAL` (INTEGER) |

**Impacto:** el plan original asumía que había que **añadir** `updated_at`, `is_deleted`, etc. a Postgres. **Eso no es necesario.** Solo hay que añadir `uuid` y exponer las columnas existentes en los DTOs.

**Inconsistencias detectadas entre código mobile y esquema:**

| # | Tema | Esquema real | Código Android | Acción |
|---|------|--------------|----------------|--------|
| 1 | `incidentes.tipo_incidente_id` es FK a `tipos_incidente` | Catálogo normalizado | `IncidentEntity.type` es String del enum local | Mapper debe resolver `IncidentType → tipo_incidente_id` |
| 2 | `incidentes.nivel_gravedad` ∈ `('bajo','medio','alto','critico')` | CHECK | Local guarda `IncidentSeverity.name` (`MEDIUM`, etc.) | Traducir enum → valor remoto |
| 3 | `incidentes` requiere `tipo_incidente_id INTEGER NOT NULL` y `reportado_por_id` | FK obligatoria | Local no almacena estos IDs | Mapper con defaults (como `SUPABASE_DEFAULT_*`) |
| 4 | `calificaciones` sin UNIQUE multi-columna en servidor | Sin restricción | Room tiene UNIQUE local en `(estudiante, materia_nombre, periodo_nombre, descripcion)` por **nombre**, mientras Postgres trabajaría por **ID** | Riesgo de duplicados al sincronizar — resolver con `uuid` |
| 5 | `estudiantes.codigo_institucional UNIQUE NOT NULL` | Clave natural fuerte | Room no la guarda | Considerar añadirla a `StudentEntity` |

**Sobre la afirmación "los incidentes ya están llegando a la base":**

Tras re-verificar el código mobile en esta sesión:
- [SupabaseApiService.kt](app/src/main/java/com/example/myapplication/data/remote/api/SupabaseApiService.kt) **sigue sin endpoint** `insertIncidente`
- [SyncRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/SyncRepositoryImpl.kt) **no toca incidentes**
- [IncidentRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/IncidentRepositoryImpl.kt) solo escribe en Room
- [SendIncidentReportUseCase.kt](app/src/main/java/com/example/myapplication/domain/usecase/incidents/SendIncidentReportUseCase.kt) solo envía a Telegram

**Desde el código mobile actual no hay path que escriba incidentes en Supabase.** Si están llegando, es por:

1. Inserción manual (SQL Editor / dashboard de Supabase)
2. Otro cliente (panel admin web, script externo)
3. Webhook del bot de Telegram → INSERT server-side
4. La afirmación se refiere al **Room local** (también es "una base de datos")

> **Acción Fase 0 (nueva):** confirmar con el equipo cuál es el path real. Si server-side ya inserta, mobile solo necesita PULL. Si no, mobile debe implementar POST. Esto condiciona la Fase 4 del plan.

---

## 2. Objetivo del plan

Llevar la app a un Offline First robusto donde:

1. La UI **nunca depende de la red** para mostrar o aceptar datos.
2. Cada operación local (crear/editar/eliminar) queda **persistida y encolada** con su estado de sincronización.
3. WorkManager sincroniza en **background** cuando hay red, con reintentos exponenciales.
4. Al recuperar conexión, la sincronización se dispara **automáticamente**.
5. Los conflictos se resuelven de forma **determinista** por `updated_at`.
6. La UI muestra al usuario qué está **pendiente**, qué **falló**, y cuándo fue la **última sincronización exitosa**.

Cada fase es **desplegable por separado**, reversible y con criterios de aceptación claros.

---

## 3. Módulos / pantallas candidatos (orden de prioridad)

| Orden | Módulo | Razón |
|-------|--------|-------|
| 1 | **Incidentes** | Mayor brecha (no sincroniza con Supabase). Pantalla de captura de campo donde es más probable estar offline |
| 2 | **Asistencias** | Ya está cerca; falta sync automático + WorkManager |
| 3 | **Calificaciones** | Mismo patrón que asistencias; segundo en madurez |
| 4 | **Estudiantes** | Hoy solo pull; soportar también edición/creación local |

---

## 4. Arquitectura objetivo

```
┌──────────────────────────────────────────────────────────┐
│ UI (Compose) ← StateFlow ← ViewModel                     │
├──────────────────────────────────────────────────────────┤
│ Domain UseCases                                          │
├──────────────────────────────────────────────────────────┤
│ Repository (Single Source of Truth = Room)               │
│   ├─ escribe local con sync_status = PENDING             │
│   └─ devuelve Flow del DAO                               │
├──────────────────────────────────────────────────────────┤
│ SyncScheduler (WorkManager)                              │
│   ├─ OneTimeWorkRequest tras cada escritura local        │
│   ├─ PeriodicWorkRequest cada 15 min con NETWORK         │
│   └─ Observer en NetworkMonitor → enqueue inmediato      │
├──────────────────────────────────────────────────────────┤
│ SyncWorker → SyncRepository.syncAll()                    │
│   ├─ Pull (remote → local) por entidad con updated_at    │
│   ├─ Push (local PENDING → remote) idempotente           │
│   ├─ Resolución de conflictos por updated_at             │
│   └─ Marca local: SYNCED / FAILED + sync_error           │
├──────────────────────────────────────────────────────────┤
│ Supabase REST (idempotente con upsert)                   │
└──────────────────────────────────────────────────────────┘
```

Se conserva la arquitectura Clean actual y se añaden encima `SyncScheduler` + `SyncWorker` + tabla/columnas de cola.

---

## 5. Plan por fases

> **Nota:** No se modifica código en este documento. Cada fase tiene su propio set de archivos a crear/modificar y criterios de aceptación. Una fase no debe arrancar hasta que la anterior esté en `main` y verificada.

### FASE 0 — Limpieza y decisiones del equipo

**Objetivo:** Cerrar todas las decisiones pendientes con el equipo y dejar el repositorio limpio antes de tocar entidades, DAOs, repositorios o WorkManager.

**Estado:** Las decisiones se debatieron y aprobaron el 2026-05-30. Esta sección refleja la versión oficial de Fase 0 alineada al cronograma del proyecto.

---

#### 0.1 — Archivos a borrar (limpieza de basura)

| Archivo | Motivo | Estado |
|---------|--------|--------|
| `data/remote/dto/IncidentSyncRequest.Powershell.kt` | 0 bytes; nombre de redirección PowerShell mal escrito (basura) | ✅ Borrado en Fase 0 |

#### 0.2 — Archivos OCR vacíos a eliminar

| Archivo | Motivo | Estado |
|---------|--------|--------|
| `presentation/ocr/OcrAttendanceScreen.kt` | Stub vacío (0 bytes) | ✅ Borrado en Fase 0 |
| `presentation/ocr/OcrUiState.kt` | Stub vacío (0 bytes) | ✅ Borrado en Fase 0 |
| `presentation/ocr/OcrViewModel.kt` | Stub vacío (0 bytes) | ✅ Borrado en Fase 0 |
| `presentation/ocr/` (carpeta) | Quedó vacía tras borrar los 3 archivos | ✅ Eliminada |

> **Justificación del equipo:** la rama OCR (más allá del OCR de ML Kit ya integrado para asistencia) **no se continuará en esta fase**. Eliminar los stubs evita ruido y confusión. Cuando se retome OCR, se creará la estructura desde cero según las necesidades del momento.

#### 0.3 — Componentes compartidos: decisión de organización

Existen 5 archivos stub vacíos en `presentation/components/`:

- `AppScaffold.kt`
- `ErrorContent.kt`
- `LoadingContent.kt`
- `PrimaryActionButton.kt`
- `StudentCard.kt`

**Decisión del equipo:** **NO mover** `SyncStatusBadge` ni `PendingSyncCounter` a `presentation/components/`.

**Convención adoptada:**

- Los componentes generales de UI vivirán en `presentation/components/` (como están hoy).
- Los componentes específicos de sincronización vivirán en una **carpeta dedicada**:

  ```
  presentation/common/sync/
  ├── SyncStatusBadge.kt        ← se creará en Fase 11
  └── PendingSyncCounter.kt     ← se creará en Fase 11
  ```

- El `OfflineBanner` actual ([OfflineBanner.kt](app/src/main/java/com/example/myapplication/presentation/common/OfflineBanner.kt)) se queda en `presentation/common/` (raíz), o se mueve a `presentation/common/sync/` cuando se cree esa carpeta. Decisión menor a tomar al ejecutar Fase 11.

**Razón:** mantener separados los componentes de sincronización de los componentes generales de UI. Evita acoplar la jerarquía de componentes visuales con la jerarquía de sync.

> **No se crea la carpeta vacía ahora** (git no trackea carpetas vacías). Se creará al añadir el primer composable de sync en Fase 11.

#### 0.4 — Incidentes en Supabase: alcance confirmado

**Confirmación del equipo:** los incidentes **sí están llegando correctamente** a la base de datos remota. El mobile actual no implementa POST de incidentes (verificado en código: `SupabaseApiService` no expone `insertIncidente`).

**Decisión técnica para mobile:**

| Acción | ¿Implementar en mobile? |
|--------|--------------------------|
| **PULL** de incidentes desde Supabase → Room local | ✅ Sí (Fase 4) |
| **POST** de incidentes desde mobile → Supabase | ❌ **No en esta fase** |
| **Updates / deletes** de incidentes desde mobile | ❌ No por ahora |

**Alcance de Fase 4 para incidentes:**

- Mobile **solo lee** incidentes desde la base remota.
- Mobile **no duplica** la lógica de inserción que actualmente puebla la tabla `incidentes`.
- Mobile sí seguirá guardando incidentes localmente en Room (como hoy) para el envío por Telegram, pero **esos registros locales no se sincronizan hacia el servidor** en esta etapa.

**Documentación a producir (no implementación):**

- Anotar en `CONFIG_PENDIENTE_EQUIPO.md` o como sección adicional aquí: **qué proceso/cliente externo inserta hoy los incidentes** (manual via SQL Editor, panel admin web, webhook Telegram, script externo, etc.). Esta es documentación informativa, no condiciona el alcance de la Fase 4.

#### 0.5 — Estados de sincronización: alinear con CRONOGRAMA_PROYECTO_FINAL

**Cambio respecto al plan original:** se descarta el enum `NEVER_SYNCED / PENDING / SYNCING / SYNCED / FAILED` propuesto inicialmente. También se descarta tratar `sincronizacion_pendiente` como "alias derivado".

**Fuente de verdad:** [CRONOGRAMA_PROYECTO_FINAL.md](CRONOGRAMA_PROYECTO_FINAL.md), sección **"Modelado de Estado Inicial"**, define **dos sealed classes oficiales**:

| Sealed class | Estados | Propósito según cronograma |
|--------------|---------|-----------------------------|
| **`UiState`** (carga de datos) | `Loading`, `Success`, `Error` | Procesos de obtención/visualización de datos en pantalla |
| **`ActionState`** (envío vía API) | `Idle`, `Sending`, `Success`, `Error` | Acciones de envío al servidor o servicios externos |

**Decisión adoptada para Offline First:**

La sincronización es conceptualmente una **acción de envío al servidor**, por lo tanto, **reutilizamos la segunda sealed class** del cronograma como estado de sync por entidad:

```kotlin
// domain/model/sync/SyncState.kt  (se crea en Fase 1)
sealed class SyncState {
    data object Idle    : SyncState()              // nunca enviado o esperando próxima sync
    data object Sending : SyncState()              // en proceso de envío
    data object Success : SyncState()              // sincronizado correctamente
    data class  Error(val message: String) : SyncState()  // último intento falló
}
```

**Persistencia en Room** — columna `sync_status` (TEXT) con valores: `IDLE`, `SENDING`, `SUCCESS`, `ERROR`.

**Mapeo desde `sincronizacion_pendiente`** durante la migración (Fase 2):

| Valor antiguo | Valor nuevo `sync_status` |
|---------------|----------------------------|
| `sincronizacion_pendiente = 1` | `IDLE` (pendiente de subir) |
| `sincronizacion_pendiente = 0` | `SUCCESS` (ya sincronizado) |

**Compatibilidad:** la columna `sincronizacion_pendiente` **se conserva en Room** y **se conserva en Postgres** (no se borra). La lógica nueva trabaja con `sync_status`, pero `sincronizacion_pendiente` puede seguir siendo escrita en paralelo durante la transición para no romper la consulta `getPendingSyncAttendance()` existente. Una vez que toda la lógica use `sync_status`, se decidirá si retirar la columna antigua.

**Por qué este mapeo (4 estados, no 5):**

El plan original distinguía `NEVER_SYNCED` (recién creado) de `PENDING` (en cola). Para fines de UI y de la cola de sincronización, **ambos significan lo mismo**: "necesita ser sincronizado". Colapsarlos en `Idle` simplifica el modelo y se alinea al cronograma oficial sin perder funcionalidad.

#### 0.6 — Estrategia UUID y migración Postgres

Confirmadas en la 3ª auditoría:

- **UUID como columna adicional**, NO como reemplazo de PK. Ver §10 para detalle.
- **No es necesario añadir** `created_at`/`updated_at`/`deleted_at` a Postgres: ya existen.
- **Solo falta `uuid`** en las 4 tablas que mobile sincroniza (estudiantes, asistencias, calificaciones, incidentes).

**No se ejecuta la migración Postgres en Fase 0.** Se planifica y se valida con el equipo, pero la ejecución es parte del paso 2 del orden de implementación (§6).

#### 0.7 — Otras tareas

- Aplicar `supabase_fix_insert_rls.sql` si todavía no se hizo (recordatorio, no parte de Offline First en sí).
- Documentar matriz "entidad × operación × estrategia de sync" en `CONFIG_PENDIENTE_EQUIPO.md`.

---

#### Riesgos actualizados de Fase 0

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| Borrar archivos stub OCR podría romper imports en otro lado | Muy bajo | Verificado con `grep`: cero referencias. Los 4 archivos eran de 0 bytes |
| Decisión "solo PULL" para incidentes deja la app sin trazabilidad local del incidente en Supabase tras envío Telegram | Bajo | Aceptado por el equipo; se revisará si más adelante se necesita upstream |
| Reducir 5 estados de sync a 4 (`Idle/Sending/Success/Error`) podría limitar el detalle de "FAILED" | Bajo | `Error(message)` lleva el detalle textual; equivalente funcional |
| Cambiar el enum del plan original obliga a actualizar referencias en otras fases del MD (§10, Fase 1, Fase 3, Fase 5, Fase 8) | Medio | Esas actualizaciones se harán cuando se llegue a cada fase, no en Fase 0 |
| Cambio de nombre `SyncStatus` → `SyncState` (acorde a la sealed class del cronograma) | Bajo | Refactor textual, sin impacto funcional |

---

#### Checklist final de Fase 0

- [x] **0.1** Borrado `IncidentSyncRequest.Powershell.kt`
- [x] **0.2** Borrados los 3 stubs OCR (`OcrAttendanceScreen.kt`, `OcrUiState.kt`, `OcrViewModel.kt`)
- [x] **0.2** Eliminada la carpeta `presentation/ocr/` (vacía tras borrado)
- [x] **0.3** Decisión documentada: `SyncStatusBadge` y `PendingSyncCounter` irán en `presentation/common/sync/` (a crear en Fase 11)
- [x] **0.4** Confirmado: mobile solo hará **PULL** de incidentes; **no POST** en esta etapa
- [x] **0.5** Estados de sync alineados al cronograma: `SyncState { Idle, Sending, Success, Error }` (sealed class oficial del cronograma reutilizada)
- [x] **0.5** Mapeo de migración acordado: `sincronizacion_pendiente=1 → IDLE`, `=0 → SUCCESS`
- [x] **0.5** Compatibilidad confirmada: columna `sincronizacion_pendiente` se conserva durante la transición
- [x] **0.6** UUID confirmado como estrategia (§10); migración Postgres planificada para paso 2 del orden de implementación
- [ ] **0.4 (informativo)** Documentar en `CONFIG_PENDIENTE_EQUIPO.md` qué proceso externo inserta hoy los incidentes en Supabase (manual / panel / webhook / script)
- [ ] **0.7** Aplicar `supabase_fix_insert_rls.sql` si aún no se hizo
- [ ] **0.7** Crear sección "matriz entidad × operación × estrategia de sync" en `CONFIG_PENDIENTE_EQUIPO.md`

**Solo cuando los 12 ítems estén marcados se debe avanzar a Fase 1.**

> **Permitido en Fase 0:** únicamente limpieza de archivos vacíos/basura.
> **Prohibido en Fase 0:** modificar entidades, DAOs, repositorios, WorkManager, ViewModels, dependencias o esquema de Postgres. Eso pertenece a Fases 1+.

---

### FASE 1 — Definición de entidades locales

**Objetivo:** Enriquecer el modelo local para soportar estados de sync, conflictos por timestamp y soft-delete.

**Archivos a crear:**
- `domain/model/sync/SyncStatus.kt` — enum (`NEVER_SYNCED`, `PENDING`, `SYNCING`, `SYNCED`, `FAILED`).
- `data/local/Converters.kt` — `TypeConverter` para `Instant`/`LocalDateTime` si se usan.

**Archivos a modificar:**
- [AttendanceEntity.kt](app/src/main/java/com/example/myapplication/data/local/entity/AttendanceEntity.kt)
- [GradeEntity.kt](app/src/main/java/com/example/myapplication/data/local/entity/GradeEntity.kt)
- [IncidentEntity.kt](app/src/main/java/com/example/myapplication/data/local/entity/IncidentEntity.kt)
- [StudentEntity.kt](app/src/main/java/com/example/myapplication/data/local/entity/StudentEntity.kt)

**Columnas a añadir en cada entidad (revisado en 3ª auditoría):**

| Columna | Tipo | Default | Propósito |
|---------|------|---------|-----------|
| `uuid` | `String` | `UUID.randomUUID().toString()` | **Identificador cross-system.** Se genera en mobile al crear local; el servidor lo respeta (ver §10) |
| `remote_id` | `Long?` | `null` | SERIAL del servidor tras primer push. Para `PATCH/DELETE` por id en futuras updates |
| `sync_status` | `String` | `"NEVER_SYNCED"` | Estado: `NEVER_SYNCED`, `PENDING`, `SYNCING`, `SYNCED`, `FAILED` |
| `sync_error` | `String?` | `null` | Último mensaje de error |
| `last_sync_attempt` | `Long?` | `null` | Cuándo se intentó sincronizar por última vez (epoch ms) |
| `server_updated_at` | `Long?` | `null` | Espejo del `updated_at` server. Para resolución de conflictos en Fase 8 |
| `is_deleted` | `Boolean` | `false` | Soft-delete / tombstone local |

> **Cambio respecto al plan original (3ª auditoría):** ya **no se añade** `updated_at` ni `created_at` propios al Room. Esas columnas **viven en Postgres** y se reciben vía DTO. El mobile solo necesita `server_updated_at` como espejo para comparar.

**Índices a crear:**
- `UNIQUE INDEX` sobre `uuid` en cada tabla.
- Índice sobre `sync_status` para acelerar `getPendingSync()`.

**Conservar** la columna existente `sincronizacion_pendiente` como **alias derivado** (`sync_status IN ('PENDING','NEVER_SYNCED','FAILED')`) durante la transición, para no romper nada.

**Criterios de aceptación:**
- [ ] Entidades modificadas compilan.
- [ ] Tests existentes pasan.
- [ ] No se toca aún la versión de la DB (eso es la siguiente fase).

**Riesgo:** medio. Cambia el esquema; sin migración explícita en Fase 2, se pierden datos.

---

### FASE 2 — Migraciones reales en Room

**Objetivo:** Subir la versión de la DB de 5 → 6 con migración explícita, eliminando `fallbackToDestructiveMigration`.

**Archivos a crear:**
- `data/local/migrations/Migration_5_6.kt` — añade las 6 columnas nuevas a las 4 tablas con `ALTER TABLE`.

**Archivos a modificar:**
- [AppDatabase.kt](app/src/main/java/com/example/myapplication/data/local/AppDatabase.kt) — subir a `version = 6`.
- [AppModule.kt](app/src/main/java/com/example/myapplication/core/di/AppModule.kt) — reemplazar `fallbackToDestructiveMigration()` por `addMigrations(Migration_5_6)`.

**SQL de referencia (sketch, actualizado 3ª auditoría):**

```sql
-- Para cada tabla mobile (asistencias, calificaciones, incidentes, students):
ALTER TABLE asistencias ADD COLUMN uuid TEXT NOT NULL DEFAULT '';
ALTER TABLE asistencias ADD COLUMN remote_id INTEGER;
ALTER TABLE asistencias ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'NEVER_SYNCED';
ALTER TABLE asistencias ADD COLUMN sync_error TEXT;
ALTER TABLE asistencias ADD COLUMN last_sync_attempt INTEGER;
ALTER TABLE asistencias ADD COLUMN server_updated_at INTEGER;
ALTER TABLE asistencias ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX idx_asistencias_uuid ON asistencias(uuid);
CREATE INDEX idx_asistencias_sync_status ON asistencias(sync_status);
-- repetir para calificaciones, incidentes, students
```

**Backfill durante migración:**
- Registros con `sincronizacion_pendiente = 1` → `sync_status = 'PENDING'`.
- Registros con `sincronizacion_pendiente = 0` → `sync_status = 'SYNCED'`.
- Registros existentes sin `uuid`: generar UUID local (`hex(randomblob(16))` en SQLite o post-migration callback en Kotlin que asigne `UUID.randomUUID()`). Cuando se sincronicen por primera vez, el servidor hará upsert por clave natural (no por uuid) y registrará el UUID del mobile.

**Criterios de aceptación:**
- [ ] App arranca sobre DB de versión 5 preexistente y migra sin perder datos.
- [ ] Test de migración con `MigrationTestHelper`.
- [ ] No queda ninguna llamada a `fallbackToDestructiveMigration` en el código.

**Riesgo:** alto si se omite el test de migración. Bajo si se hace bien.

---

### FASE 3 — DAOs y consultas extendidas

**Objetivo:** Reemplazar las consultas basadas en `sincronizacion_pendiente` por consultas basadas en `sync_status`, y añadir operaciones para soft-delete y manejo de errores de sync.

**Archivos a modificar:**
- [AttendanceDao.kt](app/src/main/java/com/example/myapplication/data/local/dao/AttendanceDao.kt)
- [GradeDao.kt](app/src/main/java/com/example/myapplication/data/local/dao/GradeDao.kt)
- [IncidentDao.kt](app/src/main/java/com/example/myapplication/data/local/dao/IncidentDao.kt)
- [StudentDao.kt](app/src/main/java/com/example/myapplication/data/local/dao/StudentDao.kt)

**Métodos a añadir en cada DAO (cuando aplique):**

```kotlin
@Query("SELECT * FROM <tabla> WHERE sync_status IN ('PENDING','FAILED','NEVER_SYNCED') AND is_deleted = 0")
suspend fun getPendingSync(): List<XEntity>

@Query("SELECT * FROM <tabla> WHERE is_deleted = 1 AND sync_status != 'SYNCED'")
suspend fun getPendingDeletes(): List<XEntity>

@Query("UPDATE <tabla> SET sync_status = 'SYNCING', last_sync_attempt = :now WHERE id IN (:ids)")
suspend fun markAsSyncing(ids: List<Long>, now: Long)

@Query("UPDATE <tabla> SET sync_status = 'SYNCED', sync_error = NULL, remote_id = :remoteId WHERE id = :id")
suspend fun markAsSynced(id: Long, remoteId: Long?)

@Query("UPDATE <tabla> SET sync_status = 'FAILED', sync_error = :error, last_sync_attempt = :now WHERE id = :id")
suspend fun markAsFailed(id: Long, error: String, now: Long)

@Query("UPDATE <tabla> SET is_deleted = 1, sync_status = 'PENDING', updated_at = :now WHERE id = :id")
suspend fun softDelete(id: Long, now: Long)

@Query("SELECT COUNT(*) FROM <tabla> WHERE sync_status IN ('PENDING','FAILED')")
fun observePendingCount(): Flow<Int>
```

**Importante:** los `Flow<List<...>>` que ya consume la UI deben **filtrar `is_deleted = 0`** para que los soft-deletes desaparezcan visualmente al instante.

**Criterios de aceptación:**
- [ ] Todos los DAOs exponen `getPendingSync()`, `markAsSyncing()`, `markAsSynced()`, `markAsFailed()`, `softDelete()`.
- [ ] Tests unitarios con `Room.inMemoryDatabaseBuilder` cubriendo transiciones de estado.
- [ ] La UI sigue funcionando porque los Flows filtran `is_deleted = 0`.

**Riesgo:** bajo. Solo añade comportamiento; las queries previas siguen presentes.

---

### FASE 4 — Endpoints remotos completos

**Objetivo:** Cerrar el gap de incidentes con Supabase y añadir capacidad de pull incremental.

**Archivos a crear:**
- `data/remote/dto/IncidenteInsertDto.kt` — DTO para POST.
- `data/remote/dto/IncidenteRemoteResponseDto.kt` — DTO para respuesta.

**Archivos a modificar:**
- [SupabaseApiService.kt](app/src/main/java/com/example/myapplication/data/remote/api/SupabaseApiService.kt) — añadir:
  ```kotlin
  @POST(SupabaseConfig.INCIDENTES_TABLE)
  @Headers("Prefer: return=representation")
  suspend fun insertIncidente(@Body body: IncidenteInsertDto): List<IncidenteRemoteResponseDto>

  @GET(SupabaseConfig.ASISTENCIAS_TABLE)
  suspend fun getAsistenciasSince(@Query("updated_at") gte: String): List<AsistenciaRemoteResponseDto>
  // análogos para calificaciones e incidentes
  ```
- [SupabaseConfig.kt](app/src/main/java/com/example/myapplication/core/network/SupabaseConfig.kt) — añadir `INCIDENTES_TABLE`.
- [IncidentMapper.kt](app/src/main/java/com/example/myapplication/data/mapper/IncidentMapper.kt) — mapper local↔DTO.

**Criterios de aceptación:**
- [ ] Endpoint de incidentes funcionalmente verificado con curl/Postman contra Supabase staging.
- [ ] Mappers tienen tests unitarios.

**Riesgo:** medio. Depende de que el esquema de Supabase para `incidentes` esté listo (decisión de Fase 0).

---

### FASE 5 — Repositorios ajustados

**Objetivo:** Que cada escritura local marque `sync_status = PENDING`, registre `updated_at = now()`, y luego **encole** un sync en lugar de bloquear la UI.

**Archivos a modificar:**
- [AttendanceRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/AttendanceRepositoryImpl.kt)
- [GradeRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/GradeRepositoryImpl.kt)
- [IncidentRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/IncidentRepositoryImpl.kt)
- [StudentRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/StudentRepositoryImpl.kt)
- [SyncRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/SyncRepositoryImpl.kt) — añadir push de incidentes.

**Patrón unificado:**

```kotlin
override suspend fun upsertX(x: X) {
    val now = System.currentTimeMillis()
    val entity = x.toEntity().copy(
        updatedAt = now,
        syncStatus = SyncStatus.PENDING.name
    )
    dao.insertOrReplace(entity)
    syncScheduler.enqueueOneTime()  // no bloquea
}

override suspend fun deleteX(id: Long) {
    dao.softDelete(id, System.currentTimeMillis())
    syncScheduler.enqueueOneTime()
}
```

**Criterios de aceptación:**
- [ ] Ningún ViewModel llama directamente a `syncRepository.syncPendingRecords()` (hoy [AttendanceViewModel.kt:189](app/src/main/java/com/example/myapplication/presentation/attendance/AttendanceViewModel.kt:189) y [GradesViewModel.kt:269](app/src/main/java/com/example/myapplication/presentation/grades/GradesViewModel.kt:269) lo hacen).
- [ ] La sync ocurre vía WorkManager.
- [ ] Botón "Guardar" responde instantáneo (no espera red).

**Riesgo:** medio. Cambia el contrato de cuándo se sube algo. Se mitiga porque WorkManager dispara en segundos cuando hay red.

---

### FASE 6 — WorkManager + SyncScheduler

**Objetivo:** Persistir la cola de sincronización fuera del proceso de la app, con reintentos y constraints de red.

**Dependencia nueva en `app/build.gradle.kts`:**

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.1")  // o la última estable
```

**Archivos a crear:**
- `core/sync/SyncWorker.kt`:
  ```kotlin
  class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
      override suspend fun doWork(): Result {
          val outcome = AppModule.provideSyncRepository(applicationContext).syncAll()
          return when (outcome) {
              is SyncOutcome.Success -> Result.success()
              is SyncOutcome.Skipped -> Result.success()
              is SyncOutcome.Failure -> if (runAttemptCount < 5) Result.retry() else Result.failure()
          }
      }
  }
  ```
- `core/sync/SyncScheduler.kt`:
  ```kotlin
  class SyncScheduler(private val workManager: WorkManager) {
      fun enqueueOneTime() { /* OneTimeWorkRequest con NetworkType.CONNECTED */ }
      fun enqueuePeriodic() { /* PeriodicWorkRequest cada 15 min */ }
  }
  ```

**Archivos a modificar:**
- [AppModule.kt](app/src/main/java/com/example/myapplication/core/di/AppModule.kt) — `provideSyncScheduler(context)`.
- [AlegriApp.kt](app/src/main/java/com/example/myapplication/AlegriApp.kt) — al arrancar, `SyncScheduler.enqueuePeriodic()` y `enqueueOneTime()` en lugar de `syncAll()` directo.

**Constraints recomendados:**
- `NetworkType.CONNECTED`
- Backoff: `BackoffPolicy.EXPONENTIAL`, inicial `30s`
- `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` para sync inmediato

**Criterios de aceptación:**
- [ ] `WorkManagerTestInitHelper` cubre `SyncWorker`.
- [ ] Cerrar la app con datos pendientes y reabrirla: sync ocurre en background.
- [ ] Sin red, el worker queda encolado; al recuperar red, ejecuta.

**Riesgo:** medio. WorkManager tiene caveats con sync inmediato y procesos en background en Android 12+; mitigar con expedited work.

---

### FASE 7 — Sincronización automática al recuperar conexión

**Objetivo:** Que la app detecte la transición `offline → online` y dispare un sync sin que el usuario tenga que abrir la app.

**Archivos a modificar:**
- [AlegriApp.kt](app/src/main/java/com/example/myapplication/AlegriApp.kt) — añadir un `LaunchedEffect` que colecte `networkMonitor.isOnline.distinctUntilChanged().filter { it }` y llame `SyncScheduler.enqueueOneTime()`.

**Alternativa más robusta:** registrar un `BroadcastReceiver` o un `WorkManager` con `NetworkType.CONNECTED` y `setBackoffCriteria` para que se dispare cuando WorkManager detecte red.

**Criterios de aceptación:**
- [ ] Activar avión mode con la app en foreground → registrar datos → desactivar avión mode → sync ocurre en <30s.
- [ ] Mismo escenario con app en background: WorkManager dispara igualmente.

**Riesgo:** bajo si se hace a través de WorkManager. Mayor si se hace solo desde el Composable (muere con la UI).

---

### FASE 8 — Estrategia de conflictos

**Objetivo:** Resolver de forma determinista los choques entre cambios locales y remotos.

**Política base:**

| Caso | Resolución |
|------|------------|
| Local `updated_at` > Remoto `updated_at` y `sync_status = PENDING` | Local gana (push) |
| Remoto `updated_at` > Local `updated_at` y `sync_status = SYNCED` | Remoto gana (overwrite local) |
| Local `is_deleted = true` | Push delete → al confirmar, eliminar definitivamente local |
| Incidentes (registro inmutable de hechos) | Nunca sobrescribir; append-only |
| Estudiante manual local con `remote_id = null` | Crear en remoto; al recibir ID, actualizar `remote_id` |

**Archivos a modificar:**
- [SyncRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/SyncRepositoryImpl.kt) — implementar las reglas anteriores en `syncStudentsFromRemote()` y `syncPendingRecords()`.

**Criterios de aceptación:**
- [ ] Escenario "editar misma asistencia en dos dispositivos offline" se documenta y resuelve por `updated_at`.
- [ ] Tests del repositorio con `MockWebServer` cubren los 3–4 casos principales.

**Riesgo:** medio. Es la fase con más complejidad lógica; un bug aquí puede borrar datos.

---

### FASE 9 — IDs offline-safe (estrategia UUID confirmada en 3ª auditoría)

**Objetivo:** Corregir el bug actual donde estudiantes creados manualmente reciben IDs locales por `max+1` (ver [IncidentViewModel.kt:771](app/src/main/java/com/example/myapplication/presentation/incidents/IncidentViewModel.kt:771)), que chocarán con IDs de Supabase.

**Decisión definitiva (ver §10 para detalle completo):**

**UUID como columna adicional**, manteniendo `SERIAL` en Postgres y `autoGenerate Long` en Room como PKs internas. El `uuid` actúa como identificador cross-system y como clave de upsert idempotente.

**Por qué esta opción y no las descartadas:**

| Opción | Veredicto |
|--------|-----------|
| Reemplazar `SERIAL` por `UUID PRIMARY KEY` en Postgres | ❌ Rompe decenas de FKs y obliga a migrar todos los datos. Riesgo alto |
| IDs negativos en mobile + remap | ❌ Lógica compleja, FKs internas en Room requieren cascada al remapear |
| **UUID adicional + SERIAL/Long como PKs internas** | ✅ Sin cambios en FKs, sin migración destructiva, soporte offline puro |

**Archivos afectados:**
- Todas las entidades Room (`uuid` ya añadido en Fase 1).
- [IncidentViewModel.kt:771](app/src/main/java/com/example/myapplication/presentation/incidents/IncidentViewModel.kt:771) — eliminar `nextLocalStudentId()`. Reemplazar por `UUID.randomUUID().toString()` al crear `Student` local.
- Mappers de DTOs — propagar `uuid` en inserts.
- `SupabaseApiService` — POSTs usan `on_conflict=uuid` + `Prefer: resolution=merge-duplicates`.

**Criterios de aceptación:**
- [ ] Crear estudiante offline, sincronizar, verificar que existe en Supabase con su UUID propagado.
- [ ] Reintentar el mismo POST: Supabase debe responder upsert (no duplicar).
- [ ] Crear incidente offline asociado a ese estudiante, sincronizar, verificar FK correcta en Supabase.
- [ ] El bug `nextLocalStudentId` queda eliminado.

**Riesgo:** medio. Mitigado porque no se tocan PKs ni FKs server-side. Ver §10 para SQL exacto y backfill.

---

### FASE 10 — Detección de conexión y metadatos

**Objetivo:** Persistir cuándo fue la última sincronización exitosa para mostrarlo al usuario y para usarlo en pulls incrementales (`updated_since`).

**Dependencia nueva:**

```kotlin
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

**Archivos a crear:**
- `core/preferences/SyncPreferences.kt`:
  ```kotlin
  class SyncPreferences(context: Context) {
      val lastSuccessfulSync: Flow<Long>
      suspend fun setLastSuccessfulSync(epochMs: Long)
      val lastError: Flow<String?>
      suspend fun setLastError(error: String?)
  }
  ```

**Archivos a modificar:**
- [SyncRepositoryImpl.kt](app/src/main/java/com/example/myapplication/data/repository/SyncRepositoryImpl.kt) — escribir `lastSuccessfulSync` al final de un sync exitoso.

**Criterios de aceptación:**
- [ ] `lastSuccessfulSync` persiste entre reinicios.
- [ ] Pulls usan ese valor para filtrar por `updated_since`.

**Riesgo:** bajo.

---

### FASE 11 — UI consciente del estado de sincronización

**Objetivo:** Hacer visible al usuario qué está pendiente, qué falló y cuándo fue la última sincronización exitosa.

**Archivos a crear:**
- `presentation/common/SyncStatusBadge.kt` — composable que muestra icono "nube vacía" / "nube con check" / "nube con !" según `sync_status`.
- `presentation/common/PendingSyncCounter.kt` — chip "X pendientes" en top bars.

> ⚠️ **Nota re-auditoría 2026-05-30:** existen stubs vacíos en `presentation/components/` (ver §1.5). Si en Fase 0 se decide que esa carpeta será la oficial para componentes compartidos, los dos archivos anteriores deben crearse ahí y no en `presentation/common/`.

**Archivos a modificar:**
- [OfflineBanner.kt](app/src/main/java/com/example/myapplication/presentation/common/OfflineBanner.kt) — añadir línea "Última sincronización: hace 5 min".
- [AttendanceScreen.kt](app/src/main/java/com/example/myapplication/presentation/attendance/AttendanceScreen.kt) — ya tiene el banner; añadir `SyncStatusBadge` por fila.
- `GradesScreen.kt` e `IncidentsScreen.kt` — añadir `OfflineBanner` (hoy no lo tienen).
- ViewModels — exponer `pendingSyncCount: StateFlow<Int>` derivado de `dao.observePendingCount()`.

**No deshabilitar acciones de guardado** cuando se está offline: ese es el sentido del Offline First. Solo deshabilitar lo que requiere red sí o sí (envío Telegram, OCR si depende de cloud).

**Criterios de aceptación:**
- [ ] Las 3 pantallas (asistencias, calificaciones, incidentes) muestran el banner offline.
- [ ] Cada fila con `sync_status != SYNCED` tiene un badge visual.
- [ ] Top bar muestra contador de pendientes.
- [ ] Botón manual "Sincronizar ahora" disponible.

**Riesgo:** bajo. Solo UI.

---

### FASE 12 — Pruebas

**Objetivo:** Cubrir el flujo Offline First con tests automatizados antes de cerrar el proyecto.

**Tipos de prueba:**

| Tipo | Cobertura | Herramienta |
|------|-----------|-------------|
| Unit | DAOs: transiciones de `sync_status`, soft-delete, filtros de Flow | `Room.inMemoryDatabaseBuilder` |
| Unit | Mappers DTO↔Entity↔Domain | JUnit |
| Unit | Resolución de conflictos en `SyncRepositoryImpl` | `MockWebServer` |
| Migración | `Migration_5_6` preserva datos | `MigrationTestHelper` |
| WorkManager | `SyncWorker` retoma reintentos | `WorkManagerTestInitHelper` |
| Integración | Guardar offline → activar red → push exitoso | Test instrumentado |
| Manual UI | Avión mode toggle, crear/editar/eliminar offline, recuperar red | Checklist humano |

**Criterios de aceptación:**
- [ ] Cobertura mínima del 70 % en `data/local` y `data/repository`.
- [ ] Test de migración pasa.
- [ ] Test instrumentado verde.

**Riesgo:** bajo. Solo añade red de seguridad.

---

## 6. Orden recomendado de implementación (revisado 3ª auditoría)

Orden pensado para minimizar riesgo: primero deuda obvia, luego clarificar el path de incidentes (§1.6), luego migración Postgres no destructiva para UUID, luego protección de datos local, luego enriquecimiento del modelo de sync, y al final infraestructura (WorkManager) y UI.

| Paso | Fase | Por qué este orden |
|------|------|--------------------|
| 1 | Fase 0 | Limpieza barata; no toca producción. Resolver §1.6 antes de avanzar |
| 2 | **Migración Postgres** (§10.4) | Añadir `uuid` a 4 tablas. No destructivo. Habilita todo lo demás |
| 3 | Fase 1 + Fase 2 | Entidades Room + migración real (incluye backfill UUID §10.12) |
| 4 | Fase 3 | DAOs extendidos (con `getByUuid`, `markAsSynced(uuid,…)`) |
| 5 | Fase 4 (parcial o completa) | Endpoints Supabase: añadir `uuid` a todos los DTOs. Si §1.6 confirma que mobile debe hacer push de incidentes, añadir `upsertIncidente` |
| 6 | Fase 9 | IDs offline-safe — eliminar `nextLocalStudentId`, usar UUID al crear local |
| 7 | Fase 5 | Repositorios escriben con `sync_status` + propagan UUID |
| 8 | Fase 6 | WorkManager (la app funciona igual sin él; añadirlo es no destructivo) |
| 9 | Fase 7 | Sync automático al recuperar conexión |
| 10 | Fase 8 | Conflictos por `server_updated_at` (ya recibido vía DTOs) |
| 11 | Fase 10 | DataStore para metadatos |
| 12 | Fase 11 | UI con badges y banners |
| 13 | Fase 12 | Tests |

> Cada paso es desplegable de forma independiente y reversible. La migración Postgres del paso 2 es la única acción server-side y no rompe nada del esquema actual.

---

## 7. Archivos / carpetas afectados (resumen)

### A crear

```
app/src/main/java/com/example/myapplication/
├── core/
│   ├── sync/
│   │   ├── SyncWorker.kt
│   │   └── SyncScheduler.kt
│   └── preferences/
│       └── SyncPreferences.kt
├── data/
│   ├── local/
│   │   ├── Converters.kt
│   │   └── migrations/
│   │       └── Migration_5_6.kt
│   └── remote/
│       └── dto/
│           ├── IncidenteInsertDto.kt
│           └── IncidenteRemoteResponseDto.kt
├── domain/
│   └── model/
│       └── sync/
│           └── SyncStatus.kt
└── presentation/
    └── common/
        ├── SyncStatusBadge.kt
        └── PendingSyncCounter.kt
```

### A modificar

- [AppDatabase.kt](app/src/main/java/com/example/myapplication/data/local/AppDatabase.kt) — versión + migraciones
- [AppModule.kt](app/src/main/java/com/example/myapplication/core/di/AppModule.kt) — eliminar `fallbackToDestructiveMigration`, registrar `SyncScheduler`
- [AlegriApp.kt](app/src/main/java/com/example/myapplication/AlegriApp.kt) — usar `SyncScheduler` en vez de `syncAll()`
- Entidades: `AttendanceEntity.kt`, `GradeEntity.kt`, `IncidentEntity.kt`, `StudentEntity.kt`
- DAOs: los 4
- Repositorios: los 4 de dominio + `SyncRepositoryImpl`
- [SupabaseApiService.kt](app/src/main/java/com/example/myapplication/data/remote/api/SupabaseApiService.kt) — endpoint de incidentes + `updated_since`
- [SupabaseConfig.kt](app/src/main/java/com/example/myapplication/core/network/SupabaseConfig.kt) — tabla `incidentes`
- [IncidentMapper.kt](app/src/main/java/com/example/myapplication/data/mapper/IncidentMapper.kt)
- ViewModels — exponer `pendingSyncCount`, no llamar sync directo
- Screens — añadir `OfflineBanner` en grades e incidents

### A borrar

- [IncidentSyncRequest.Powershell.kt](app/src/main/java/com/example/myapplication/data/remote/dto/IncidentSyncRequest.Powershell.kt) (0 bytes, basura)

### A resolver (decisión en Fase 0)

Archivos stub de 0 bytes detectados en la re-auditoría 2026-05-30 — completar o borrar antes de empezar fases siguientes:

```
presentation/ocr/OcrAttendanceScreen.kt
presentation/ocr/OcrUiState.kt
presentation/ocr/OcrViewModel.kt
presentation/components/AppScaffold.kt
presentation/components/ErrorContent.kt
presentation/components/LoadingContent.kt
presentation/components/PrimaryActionButton.kt
presentation/components/StudentCard.kt
```

### Dependencias nuevas en `app/build.gradle.kts`

```kotlin
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

---

## 8. Resumen ejecutivo (checklist)

| Pregunta | Respuesta |
|----------|-----------|
| **¿Existe Offline First hoy?** | Parcial (~60–70 %) — **sin cambios estructurales en las 3 auditorías (2026-05-30)** |
| **Evidencia clave** | `AppDatabase` con 4 entidades · `SyncRepositoryImpl` · `NetworkMonitor` · `OfflineBanner` · columna `sincronizacion_pendiente` · Flows desde DAO en todos los repos · `AttendanceTranscriptionService` (servicio puro offline-capable) · **Postgres ya tiene `created_at`/`updated_at`/`deleted_at`/`sincronizacion_pendiente`/`auditoria`** (3ª auditoría) |
| **Brechas principales** | WorkManager · sync automático al recuperar conexión · push de incidentes a Supabase (path actual no claro) · UUID en Postgres y Room · soft-delete local · estado `FAILED` · migraciones reales · mapeo enum → catálogo `tipos_incidente` |
| **Estrategia IDs** | **UUID como columna adicional** (no reemplaza PK SERIAL). Detalle en §10 |
| **Primer paso recomendado** | Fase 0 — borrar `IncidentSyncRequest.Powershell.kt`, resolver los 8 archivos stub vacíos, confirmar **quién/cómo escribe incidentes a Supabase hoy**, planificar migración Postgres de §10 |
| **Riesgo más alto** | `fallbackToDestructiveMigration()` perdiendo datos pendientes al cambiar el esquema — atajar en Fase 2 antes que nada más |
| **Módulo candidato #1** | Incidentes (condicional a §1.6) |
| **Esfuerzo estimado total** | 5–7 fases de 1–2 días + tests |

---

## 9. Glosario

| Término | Significado |
|---------|-------------|
| **Offline First** | Patrón en el que la UI siempre lee/escribe local; la red es un detalle de sincronización en background |
| **Tombstone / soft-delete** | Marcar como eliminado sin borrar físicamente, para poder propagar el delete a remoto |
| **Last-Write-Wins (LWW)** | Estrategia de conflictos donde gana el cambio con `updated_at` más reciente |
| **Idempotencia** | Propiedad de una operación: ejecutarla N veces produce el mismo resultado que ejecutarla 1 vez |
| **Pull incremental** | Traer solo los registros modificados desde el último sync (`updated_since`) |
| **Backoff exponencial** | Reintentos con tiempo de espera que crece (30s, 60s, 120s, 240s, …) |
| **UUID (cross-system id)** | Identificador único de 128 bits que mobile y servidor comparten para reconocer el mismo registro sin depender del autoincrement del servidor |

---

## 10. Estrategia UUID (añadida en 3ª auditoría 2026-05-30)

### 10.1 Decisión

**UUID como columna adicional, no como reemplazo de PK.** PKs `SERIAL` en Postgres y `Long` autogenerado en Room **se mantienen tal cual**.

### 10.2 Justificación

| Opción | Pros | Contras | Veredicto |
|--------|------|---------|-----------|
| **A. UUID adicional, mantener SERIAL/Long PKs** | Sin cambios en FKs · Sin migración de datos · `gen_random_uuid()` autopobla · Mobile pre-genera offline · `ON CONFLICT (uuid)` upserts idempotentes | Dos identificadores por fila (overhead mínimo) | ✅ **Elegido** |
| B. Reemplazar PK por UUID | Identificador único universal | Recreación de decenas de FKs · Migración pesada · Joins más lentos · Riesgo de regresión muy alto | ❌ |
| C. IDs negativos en mobile + remap | No requiere cambios en Postgres | Lógica de remap compleja · FKs internas en Room exigen cascada · Más bugs probables | ❌ |

### 10.3 Tablas que reciben `uuid`

Solo las que el mobile crea/edita activamente:

| Tabla | uuid | Razón |
|-------|------|-------|
| `estudiantes` | ✅ | Mobile puede crear estudiante offline (bug actual `IncidentViewModel:771`) |
| `asistencias` | ✅ | Push frecuente desde mobile offline |
| `calificaciones` | ✅ | Push frecuente desde mobile offline |
| `incidentes` | ✅ | Mobile crea offline; condicional a §1.6 |
| `evidencias` | ✅ (futuro) | Cuando se implemente captura de fotos offline |
| Catálogos (`tipos_*`, `niveles_academicos`, `periodos_academicos`, `roles`) | ❌ | Solo pull. SERIAL es suficiente |
| `representantes`, `usuarios` | ⚠️ | Solo si mobile los crea offline (decidir en Fase 0) |
| `notificaciones`, `reportes`, `auditoria`, `sincronizacion_pendiente` | ❌ | Server-side puro |

### 10.4 Migración Postgres

```sql
-- Requiere pgcrypto (Supabase lo trae por defecto)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Por cada tabla mobile:
ALTER TABLE estudiantes
    ADD COLUMN uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE asistencias
    ADD COLUMN uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE calificaciones
    ADD COLUMN uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE incidentes
    ADD COLUMN uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid();

-- UNIQUE genera índice automáticamente, no hace falta CREATE INDEX
```

**Comportamiento esperado:**
- Postgres 11+ ejecuta el ADD COLUMN con DEFAULT **instantáneamente** (no reescribe la tabla).
- Filas existentes reciben un UUID generado al momento.
- Filas nuevas reciben uuid si el cliente no lo envía.

### 10.5 Cambios en Room

```kotlin
// AttendanceEntity (ejemplo, mismo patrón en todas las entidades mobile)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,                // PK Room (sin cambios)
    @ColumnInfo(name = "uuid") val uuid: String,                       // ← NUEVO
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,        // ← NUEVO (SERIAL Postgres tras sync)
    // ... resto de columnas existentes ...
    @ColumnInfo(name = "sync_status") val syncStatus: String = "PENDING",
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)

// Índices
@Entity(
    tableName = "asistencias",
    indices = [
        // ... índices existentes ...
        Index(value = ["uuid"], unique = true),
        Index(value = ["sync_status"])
    ]
)
```

### 10.6 Generación del UUID en mobile

```kotlin
// Al crear local
val entity = AttendanceEntity(
    uuid = java.util.UUID.randomUUID().toString(),
    studentId = studentId,
    // ...
)

// O extension helper:
fun newUuid(): String = java.util.UUID.randomUUID().toString()
```

### 10.7 Cambios en DTOs

```kotlin
// data/remote/dto/AsistenciaInsertDto.kt
data class AsistenciaInsertDto(
    val uuid: String,                       // ← NUEVO (cliente lo envía)
    @SerializedName("estudiante_id")
    val estudianteId: Long,
    // ... resto sin cambios ...
)

// data/remote/dto/AsistenciaRemoteResponseDto.kt
data class AsistenciaRemoteResponseDto(
    val id: Long,                           // SERIAL del servidor → guardar en remote_id
    val uuid: String,                       // ← NUEVO
    @SerializedName("updated_at")
    val updatedAt: String,                  // ← NUEVO (para conflictos)
    @SerializedName("deleted_at")
    val deletedAt: String?,                 // ← NUEVO (soft-delete)
    // ... resto ...
)
```

### 10.8 Cambios en SupabaseApiService

```kotlin
@POST(SupabaseConfig.ASISTENCIAS_TABLE)
@Headers(
    "Prefer: resolution=merge-duplicates,return=representation"
)
suspend fun upsertAsistencia(
    @Query("on_conflict") onConflict: String = "uuid",
    @Body body: AsistenciaInsertDto
): List<AsistenciaRemoteResponseDto>

// Pull incremental:
@GET(SupabaseConfig.ASISTENCIAS_TABLE)
suspend fun getAsistenciasSince(
    @Query("updated_at") gte: String,   // ej. "gte.2024-01-01T00:00:00Z"
    @Query("deleted_at") deleted: String = "is.null"
): List<AsistenciaRemoteResponseDto>
```

### 10.9 Cambios en Mappers

```kotlin
fun AttendanceEntity.toAsistenciaInsertDto(defaultCourseId: Long) = AsistenciaInsertDto(
    uuid = this.uuid,                       // ← propagar
    estudianteId = studentId,
    cursoId = courseId ?: defaultCourseId,
    // ...
)

fun AsistenciaRemoteResponseDto.toEntity(): AttendanceEntity = AttendanceEntity(
    uuid = uuid,
    remoteId = id,                          // SERIAL del servidor
    serverUpdatedAt = Instant.parse(updatedAt).toEpochMilli(),
    syncStatus = "SYNCED",
    isDeleted = (deletedAt != null),
    // ...
)
```

### 10.10 Cambios en DAOs

```kotlin
@Query("SELECT * FROM asistencias WHERE uuid = :uuid LIMIT 1")
suspend fun getByUuid(uuid: String): AttendanceEntity?

@Query("UPDATE asistencias SET sync_status = 'SYNCED', remote_id = :remoteId, server_updated_at = :serverTs, sync_error = NULL WHERE uuid = :uuid")
suspend fun markAsSynced(uuid: String, remoteId: Long, serverTs: Long)
```

### 10.11 Cambios en Repositorios

Patrón unificado:

```kotlin
override suspend fun upsertAttendance(attendance: Attendance) {
    val existing = attendanceDao.getByStudentAndDate(attendance.studentId, attendance.date)
    val entity = attendance.toEntity(existingId = existing?.id).copy(
        uuid = existing?.uuid ?: newUuid(),  // reusa UUID si ya existe local
        syncStatus = "PENDING"
    )
    attendanceDao.insertOrReplaceAttendance(entity)
    syncScheduler.enqueueOneTime()
}
```

### 10.12 Backfill de UUIDs en datos pre-existentes

**Postgres:** automático con `DEFAULT gen_random_uuid()`. Toda fila ya en tabla recibe un UUID al ejecutar el `ALTER TABLE`.

**Room:** la migración debe asignar UUIDs a filas existentes:

```kotlin
val Migration_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Añadir columna con default vacío
        db.execSQL("ALTER TABLE asistencias ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")

        // 2. Llenar con UUIDs (SQLite no tiene UUID nativo; se usa hex(randomblob(16)))
        db.execSQL("""
            UPDATE asistencias
            SET uuid = lower(hex(randomblob(4))) || '-' ||
                       lower(hex(randomblob(2))) || '-4' ||
                       substr(lower(hex(randomblob(2))), 2) || '-' ||
                       substr('89ab', abs(random()) % 4 + 1, 1) ||
                       substr(lower(hex(randomblob(2))), 2) || '-' ||
                       lower(hex(randomblob(6)))
            WHERE uuid = ''
        """.trimIndent())

        // 3. Crear índice único
        db.execSQL("CREATE UNIQUE INDEX idx_asistencias_uuid ON asistencias(uuid)")

        // (Repetir para calificaciones, incidentes, students)
    }
}
```

**Caso especial — reconciliar UUIDs distintos para mismo registro:**

Las filas pre-existentes en Postgres y en Room **van a recibir UUIDs distintos** porque cada lado genera el suyo. La primera sincronización tras el rollout debe reconciliar usando **clave natural**:

- `estudiantes`: por `codigo_institucional`.
- `asistencias`: por `(estudiante_id, curso_id, fecha, materia_id)`.
- `calificaciones`: por una clave natural a definir (decisión en Fase 0).
- `incidentes`: por `(estudiante_id, fecha_hora, tipo_incidente_id)` aproximadamente.

Estrategia:

```kotlin
// Pseudocódigo del primer sync tras el rollout
val remoteList = api.getEstudiantesActivos()
remoteList.forEach { remote ->
    val localByCodigo = studentDao.getByCodigoInstitucional(remote.codigoInstitucional)
    when {
        localByCodigo == null ->
            studentDao.insert(remote.toEntity())  // nuevo
        localByCodigo.uuid != remote.uuid ->
            // adoptar el UUID del servidor como fuente de verdad
            studentDao.replaceUuid(localByCodigo.id, remote.uuid, remote.id)
        else ->
            studentDao.markAsSynced(remote.uuid, remote.id, remote.updatedAt)
    }
}
```

### 10.13 Riesgos específicos del UUID

| Riesgo | Severidad | Mitigación |
|--------|-----------|------------|
| `ALTER TABLE` bloquea tabla durante backfill | Bajo | Postgres 11+ con `DEFAULT gen_random_uuid()` es instantáneo |
| Datos pre-existentes con UUIDs distintos local vs remoto | Medio | Reconciliación por clave natural en primer sync (§10.12) |
| `pgcrypto` no instalado | Bajo | `CREATE EXTENSION IF NOT EXISTS pgcrypto;` (Supabase lo trae por defecto) |
| Performance de joins por UUID vs INTEGER | Muy bajo | Los joins internos siguen usando SERIAL. UUID solo para lookup desde mobile |
| Cliente envía UUID que ya existe en otra tabla | Imposible | UUID v4 tiene 2^122 valores posibles, colisión efectivamente cero |
| Migración Room falla y deja UUIDs vacíos | Medio | Test de migración obligatorio con `MigrationTestHelper` antes del rollout |

### 10.14 Aprovechamiento de la tabla `sincronizacion_pendiente` server-side

Postgres ya tiene una tabla `sincronizacion_pendiente` con `tabla_afectada`, `id_local`, `id_remoto`, `accion`, `payload`, `estado`, `intentos`, `mensaje_error`. **Decisión a tomar en Fase 0:**

| Opción | Pros | Contras |
|--------|------|---------|
| **Mobile NO la usa** (cola solo local en Room) | Simple, autocontenida en Android | No hay visibilidad server-side de la cola |
| **Mobile usa ambas** (cola local + escribe en `sincronizacion_pendiente` al sincronizar) | Auditoría completa server-side, dashboard de fallos posible | Doble escritura, complejidad mayor |

**Recomendación:** **mobile no usa la tabla server-side por ahora**. La cola vive solo en Room. Si en el futuro se quiere monitor admin server-side, se considera entonces.
