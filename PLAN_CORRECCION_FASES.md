# AlegriAPP — Plan de corrección por fases

Documento maestro para corregir el flujo de datos, persistencia local, comunicación con Telegram y dependencias desconectadas.

**Relacionado con:** `CONFIG_PENDIENTE_EQUIPO.md`, `PLAN_MODELOS_ASISTENCIAS_CALIFICACIONES.md`, `AlegriAPP_Guia_Estructura_Proyecto.md`

**Fecha de elaboración:** 2026-05-21  
**Estado:** Planificado — pendiente de ejecución por fases

---

## 1. Diagnóstico resumido

### Síntomas reportados

- Las pantallas de Asistencias y Calificaciones no muestran datos.
- No se pueden enviar reportes por Telegram desde la app.
- Las dependencias (Room, Retrofit/Supabase, Telegram) parecen no comunicarse correctamente.

### Causa raíz principal

La base de datos Room se crea **vacía** y **nunca se insertan estudiantes**. Toda la UI depende de `studentDao.observeStudents()`, por lo que las listas quedan en blanco aunque Telegram y la arquitectura MVVM estén cableados.

### Problemas secundarios confirmados

| Área | Problema | Impacto |
|------|----------|---------|
| Estudiantes | `insertOrReplaceStudents()` existe pero no tiene callers | DB vacía |
| Asistencias | No hay botón "Guardar" en UI; `SaveAttendance` no se dispara | Datos solo en memoria |
| Calificaciones | `saveGrades()` es stub; no hay UI para editar notas | Boletín siempre vacío |
| Telegram | No se valida `ok` en respuesta API; `parse_mode: Markdown` sin escapar | Fallos silenciosos o 400 |
| Telegram | Error genérico culpa siempre a `local.properties` | Diagnóstico engañoso |
| GradesViewModel | `Grade.copy(score = average)` puede violar validación | Crash / UI congelada |
| Supabase | `RetrofitClient.kt` definido pero sin uso | Sin sync remoto |
| Red | `NetworkMonitor.kt` vacío | `isOffline` nunca se usa |
| Detalle calificaciones | Usa mocks (IDs 1–6), no Room | Pantalla vacía con IDs reales |
| Incidentes | `SendReportClicked` no implementado | Módulo incompleto |

### Lo que SÍ funciona hoy

- Navegación Home → Asistencias / Calificaciones / Incidentes.
- Creación de Room (`alegriapp.db`) vía `AppModule`.
- Lectura reactiva (Flow) de DAOs cuando hay datos.
- Integración base Telegram: `TelegramApiService` → `TelegramRepositoryImpl` → `SendTelegramMessageUseCase`.
- Token y chat ID desde `local.properties` → `BuildConfig` (requiere rebuild).
- Permiso `INTERNET` en manifest.

---

## 2. Objetivo del plan

Entregar un flujo **funcional de punta a punta** en fases incrementales:

1. Ver datos en UI (estudiantes, asistencias, calificaciones).
2. Persistir cambios locales correctamente.
3. Enviar reportes por Telegram de forma confiable.
4. Conectar sync remoto y funcionalidades avanzadas.

Cada fase debe ser **desplegable por separado**, con criterios de aceptación y checklist de prueba.

---

## 3. Mapa de fases

| Fase | Nombre | Objetivo | Prioridad | Dependencias |
|------|--------|----------|-----------|--------------|
| **0** | Preparación | Alinear entorno y baseline | Crítica | Ninguna |
| **1** | Datos base en Room | Poblar estudiantes y calificaciones demo | Crítica | Fase 0 |
| **2** | Asistencias operativas | Guardar y mostrar asistencia real | Alta | Fase 1 |
| **3** | Calificaciones operativas | Registrar, persistir y listar notas | Alta | Fase 1 |
| **4** | Telegram confiable | Envío robusto con errores reales | Alta | Fases 2–3 |
| **5** | Red y sync remoto | Supabase + conectividad | Media | Fase 1 |
| **6** | Cierre funcional | Detalle, incidentes, catálogos, trazabilidad | Media–Baja | Fases 1–5 |

---

## 4. Fase 0 — Preparación y baseline

**Objetivo:** Tener un punto de partida reproducible antes de tocar código.

### Tareas

- [ ] Confirmar `local.properties` con `TELEGRAM_BOT_TOKEN` y `TELEGRAM_CHAT_ID`.
- [ ] Ejecutar **Rebuild Project** tras cualquier cambio en `local.properties`.
- [ ] Documentar emulador/dispositivo de prueba (ej. `Pixel_8_API_36`).
- [ ] Crear rama de trabajo: `fix/flujo-datos-fase-N`.
- [ ] Registrar estado inicial: capturas de pantallas vacías + Logcat baseline.

### Archivos de referencia

- `local.properties`
- `app/build.gradle.kts` (líneas BuildConfig Telegram)
- `CONFIG_PENDIENTE_EQUIPO.md` §1

### Criterios de aceptación

- Build debug compila sin errores.
- App abre en emulador/dispositivo.
- Se confirma que `BuildConfig.TELEGRAM_BOT_TOKEN` y `TELEGRAM_DEFAULT_CHAT_ID` no están vacíos (sin commitear secretos).

### Prueba manual

```bash
./gradlew.bat assembleDebug
adb devices
adb shell am start -n com.example.myapplication/.MainActivity
```

---

## 5. Fase 1 — Datos base en Room (desbloqueo de UI)

**Objetivo:** Que Asistencias y Calificaciones muestren estudiantes al abrir la app.

**Problema que resuelve:** Tabla `students` vacía; `StudentRepository` solo lectura.

### Tareas de desarrollo

#### 1.1 Seed inicial de estudiantes

- [ ] Crear callback de Room (`RoomDatabase.Callback`) o inicializador en capa data.
- [ ] Insertar 5–10 `StudentEntity` demo al crear DB por primera vez.
- [ ] Incluir campos: `id`, `fullName`, `grade`, `section`, `representativeName`, `telegramChatId` (opcional).
- [ ] Evitar reinsertar en cada apertura (comprobar `COUNT(*)` o flag de seed).

**Archivos a modificar / crear:**

| Archivo | Acción |
|---------|--------|
| `data/local/AppDatabase.kt` | Registrar callback o exportar versión |
| `core/di/AppModule.kt` | Conectar callback al `databaseBuilder` |
| `data/local/DatabaseSeeder.kt` *(nuevo, recomendado)* | Lógica de seed demo |
| `data/local/entity/StudentEntity.kt` | Referencia de campos |

#### 1.2 Escritura de estudiantes en repositorio

- [ ] Agregar `suspend fun upsertStudents(students: List<Student>)` en `StudentRepository`.
- [ ] Implementar en `StudentRepositoryImpl` usando `insertOrReplaceStudents`.
- [ ] Crear `UpsertStudentsUseCase` *(opcional en esta fase si seed basta)*.

**Archivos:**

- `domain/repository/StudentRepository.kt`
- `data/repository/StudentRepositoryImpl.kt`
- `core/di/AppModule.kt`

#### 1.3 Seed de calificaciones demo (opcional pero recomendado)

- [ ] Insertar 1–2 calificaciones por estudiante con `materia_nombre = "General"` y `periodo_nombre = "Actual"` (filtros por defecto del ViewModel).
- [ ] Facilita prueba de boletín en Fase 4 sin esperar Fase 3 completa.

**Archivos:**

- `data/local/DatabaseSeeder.kt`
- `data/local/dao/GradeDao.kt`

### Criterios de aceptación

- Al instalar app limpia (desinstalar + reinstalar), Asistencias muestra lista de estudiantes.
- Calificaciones muestra tarjetas de estudiantes (aunque notas estén vacías o demo).
- `courseName` deja de decir "Curso no asignado" cuando hay seed.
- No se pierden datos en reinicio de app (persistencia Room).

### Prueba manual

1. Desinstalar app del emulador.
2. `./gradlew.bat installDebug`
3. Abrir Asistencias → ver N estudiantes.
4. Abrir Calificaciones → ver mismos estudiantes.
5. Cerrar y reabrir app → datos siguen visibles.

### Riesgos

- `fallbackToDestructiveMigration()` borra seed si sube `version` de DB sin migración → documentar incremento de versión y re-seed.

---

## 6. Fase 2 — Asistencias operativas

**Objetivo:** Marcar, guardar y recargar asistencia por fecha.

**Problema que resuelve:** UI sin botón guardar; envío usa memoria; upsert por PK puede duplicar filas.

### Tareas de desarrollo

#### 2.1 Botón Guardar en UI

- [ ] Agregar botón "Guardar asistencia" en `AttendanceScreen.kt`.
- [ ] Disparar `AttendanceEvent.SaveAttendance`.
- [ ] Deshabilitar durante `isSaving` / `isLoading`.
- [ ] Mostrar feedback de éxito/error existente en Snackbar.

**Archivos:**

- `presentation/attendance/AttendanceScreen.kt`
- `presentation/attendance/AttendanceUiState.kt` (si falta `isSaving` visible)

#### 2.2 Corregir persistencia upsert

- [ ] Revisar `AttendanceEntity`: hoy `@PrimaryKey(autoGenerate = true)` con `id = 0L` crea filas nuevas en cada guardado.
- [ ] Opción A: `@Index(unique = true)` en `(estudiante_id, fecha)` + query `getByStudentAndDate` + update.
- [ ] Opción B: buscar registro existente antes de insertar y reutilizar `id`.
- [ ] Alinear `AttendanceMapper`: no mapear `UNMARKED` a `"ausente"` al guardar.

**Archivos:**

- `data/local/entity/AttendanceEntity.kt`
- `data/local/dao/AttendanceDao.kt`
- `data/mapper/AttendanceMapper.kt`
- `data/repository/AttendanceRepositoryImpl.kt`
- `presentation/attendance/AttendanceViewModel.kt` (`saveAttendance()`)

#### 2.3 Sincronizar UI con DB tras guardar

- [ ] Tras guardar, el Flow de `observeAttendanceByDate` debe reflejar estados persistidos.
- [ ] Evitar que recolección del Flow sobrescriba marcas en memoria sin criterio claro.

#### 2.4 Botón Justificado (opcional en fase)

- [ ] Conectar `AttendanceStatus.JUSTIFIED` en UI (hoy se mapea a `MarkAbsent`).

### Criterios de aceptación

- Marcar estudiantes → Guardar → cambiar fecha → volver → estados persistidos.
- Lista no vacía (depende Fase 1).
- Guardar con estudiantes sin marcar muestra error de validación existente.
- No se crean filas duplicadas por estudiante/fecha en DB.

### Prueba manual

1. Abrir Asistencias, marcar todos Presente.
2. Guardar → mensaje de éxito.
3. Cambiar fecha y regresar → verificar persistencia.
4. Consultar DB (Database Inspector) → 1 fila por estudiante/fecha.

---

## 7. Fase 3 — Calificaciones operativas

**Objetivo:** Registrar notas desde UI, persistirlas y mostrarlas en lista y resumen.

**Problema que resuelve:** `saveGrades()` stub; sin entrada de datos; boletín sin registros.

### Tareas de desarrollo

#### 3.1 UI mínima de edición

- [ ] Agregar campo o diálogo para ingresar nota por estudiante (0–maxScore).
- [ ] Disparar `GradesEvent.EditGrade` desde `GradesScreen` / `GradeStudentCard`.
- [ ] Botón "Guardar calificaciones" conectado a `saveGrades()`.

**Archivos:**

- `presentation/grades/GradesScreen.kt`
- `presentation/grades/components/GradeStudentCard.kt`
- `presentation/grades/GradesEvent.kt`
- `presentation/grades/GradesViewModel.kt`

#### 3.2 Implementar `saveGrades()` real

- [ ] Persistir cada `Grade` vía `SaveGradeUseCase` / `GradeRepositoryImpl`.
- [ ] Usar filtros actuales: `selectedSubject`, `selectedPeriod`.
- [ ] Corregir upsert de grades (mismo problema de PK autoGenerate que asistencias).

**Archivos:**

- `presentation/grades/GradesViewModel.kt`
- `data/repository/GradeRepositoryImpl.kt`
- `data/local/dao/GradeDao.kt`

#### 3.3 Aplicar OCR a notas (mejora incremental)

- [ ] Tras OCR, ofrecer "Aplicar sugerencias" similar a Asistencias.
- [ ] No auto-guardar sin confirmación humana (requisito del doc de equipo).

#### 3.4 Corregir cálculo para boletín (preventivo para Fase 4)

- [ ] No usar `Grade.copy(score = average)` si viola `score in 0.0..maxScore`.
- [ ] Calcular promedio en builder o DTO de reporte, no mutando entidad de dominio.

**Archivos:**

- `presentation/grades/GradesViewModel.kt` (`sendBulletin()`)
- `services/telegram/TelegramMessageBuilder.kt`

### Criterios de aceptación

- Ingresar nota → Guardar → nota visible al reabrir pantalla.
- Resumen (promedio, aprobados, en riesgo) se actualiza.
- Filtros "General" / "Actual" muestran datos guardados con esos nombres.
- OCR no guarda automáticamente.

### Prueba manual

1. Abrir Calificaciones con seed de Fase 1.
2. Editar nota de 2 estudiantes → Guardar.
3. Salir y volver → notas persistidas.
4. Verificar resumen numérico actualizado.

---

## 8. Fase 4 — Telegram confiable

**Objetivo:** Envío de reportes estable, con diagnóstico correcto y mensajes seguros.

**Problema que resuelve:** Falsos positivos/negativos, Markdown roto, errores genéricos.

**Prerequisito:** Fases 1–3 (datos y persistencia mínima).

### Tareas de desarrollo

#### 4.1 Modelo de respuesta Telegram

- [ ] Crear `TelegramResponse` DTO: `ok`, `error_code`, `description`, `result`.
- [ ] Cambiar `TelegramApiService.sendMessage` para retornar `TelegramResponse`.
- [ ] `TelegramRepositoryImpl` retorna `Result<Unit>` o sealed class con error descriptivo.

**Archivos:**

- `data/remote/dto/TelegramResponse.kt` *(nuevo)*
- `data/remote/api/TelegramApiService.kt`
- `data/repository/TelegramRepositoryImpl.kt`
- `domain/repository/TelegramRepository.kt`
- `domain/usecase/telegram/SendTelegramMessageUseCase.kt`

#### 4.2 Mensajes seguros (Markdown)

- [ ] Opción recomendada: quitar `parse_mode` por defecto **o** escapar caracteres especiales en `TelegramMessageBuilder`.
- [ ] Probar nombres con `_`, `*`, `(`, `[`.

**Archivos:**

- `data/remote/dto/TelegramMessageRequest.kt`
- `services/telegram/TelegramMessageBuilder.kt`

#### 4.3 Errores claros en ViewModels

- [ ] Reemplazar mensaje fijo de `local.properties` por error real del repositorio.
- [ ] Diferenciar: config vacía / red / parse error / chat not found / sin datos.

**Archivos:**

- `presentation/attendance/AttendanceViewModel.kt`
- `presentation/grades/GradesViewModel.kt`

#### 4.4 Ajustes de reporte

- [ ] Corregir conteo: `JUSTIFIED` no debe sumarse como ausente en resumen si se etiqueta aparte.
- [ ] Validar longitud máxima 4096 caracteres (truncar o dividir).
- [ ] Deshabilitar botón enviar en Grades durante `isSending`.

**Archivos:**

- `services/telegram/TelegramMessageBuilder.kt`
- `presentation/grades/GradesScreen.kt`

#### 4.5 Seguridad de logs

- [ ] Bajar `HttpLoggingInterceptor` a `HEADERS` o redactar token en debug.

**Archivos:**

- `core/di/AppModule.kt`

### Criterios de aceptación

- Enviar reporte de asistencia con todos marcados → mensaje llega a Telegram.
- Enviar boletín con calificaciones guardadas → mensaje llega.
- Nombre con `José_Luis` no rompe envío.
- Error de red muestra mensaje distinto a error de token vacío.
- No se reporta éxito cuando Telegram responde `"ok": false`.

### Prueba manual

1. Asistencias: marcar todos → enviar → verificar chat Telegram.
2. Calificaciones: con notas guardadas → enviar boletín.
3. Probar estudiante con underscore en nombre.
4. Probar con token inválido temporal → ver error específico.

---

## 9. Fase 5 — Red y sync remoto (Supabase)

**Objetivo:** Conectar `RetrofitClient` y preparar sincronización institucional.

**Problema que resuelve:** Backend definido pero no usado; app 100% local sin refresh.

### Tareas de desarrollo

#### 5.1 Implementar `NetworkMonitor`

- [ ] Detectar conectividad con `ConnectivityManager` + Flow/StateFlow.
- [ ] Exponer en `AppModule` o inyectar en ViewModels.
- [ ] Actualizar `isOffline` en UI states antes de save/sync/send.

**Archivos:**

- `core/network/NetworkMonitor.kt`
- `core/di/AppModule.kt`
- `presentation/attendance/AttendanceUiState.kt`
- `presentation/grades/GradesUiState.kt`

#### 5.2 API Supabase — estudiantes

- [ ] Definir `SupabaseApiService` (o reutilizar endpoints REST) para tabla `estudiantes`.
- [ ] Mover API key de `RetrofitClient.kt` a `local.properties` (`SUPABASE_URL`, `SUPABASE_KEY`).
- [ ] Repositorio remoto + estrategia: sync al inicio si online, fallback offline a Room.

**Archivos:**

- `core/network/RetrofitClient.kt`
- `data/remote/api/*` *(nuevo servicio)*
- `data/repository/StudentRepositoryImpl.kt`
- `app/build.gradle.kts` (BuildConfig opcional)

#### 5.3 Sync asistencias y calificaciones

- [ ] Usar DTOs existentes: `AttendanceSyncRequest.kt`, `GradeSyncRequest.kt`.
- [ ] Conectar queries `getPendingSync*` en DAOs (si existen) con worker o sync manual.
- [ ] Marcar `syncPending = false` tras éxito.

### Criterios de aceptación

- Con internet: app puede traer estudiantes remotos a Room.
- Sin internet: app sigue mostrando cache local.
- UI indica estado offline.
- Registros pendientes se marcan para sync.

### Prueba manual

1. Online → abrir app → estudiantes remotos visibles.
2. Modo avión → app usable con cache.
3. Guardar asistencia offline → reconectar → sync (si implementado en fase).

### Riesgos

- Credenciales Supabase no deben commitearse.
- Alinear nombres de tablas/columnas con schema SQL institucional.

---

## 10. Fase 6 — Cierre funcional y deuda técnica

**Objetivo:** Completar módulos pendientes documentados en `CONFIG_PENDIENTE_EQUIPO.md`.

### Tareas de desarrollo

#### 6.1 GradeDetail con datos reales

- [ ] Reemplazar mocks de `GradeDetailScreen` por consulta Room por `studentId`.
- [ ] Eliminar dependencia de IDs 1–6 hardcodeados.

#### 6.2 Catálogos SQL → Room

- [ ] Entidades: `materias`, `periodos_academicos`, `tipos_evaluacion`, `cursos`, `estudiante_curso`.
- [ ] Migrar filtros de calificaciones de nombre a ID.

#### 6.3 Telegram avanzado

- [ ] Tabla `configuracion_telegram` + enrutamiento por `Student.telegramChatId`.
- [ ] Tabla `notificaciones` + reintentos.

#### 6.4 Incidentes end-to-end

- [ ] Entidades, DAO, repositorio, ViewModel real.
- [ ] Conectar envío Telegram.

#### 6.5 Permisos por rol, OCR avanzado, migraciones Room

- [ ] Según prioridad del equipo (ver `CONFIG_PENDIENTE_EQUIPO.md` §5–7).

### Criterios de aceptación

- Detalle de calificaciones funciona con estudiantes reales de DB.
- Catálogos eliminan strings temporales "General"/"Actual" donde aplique.
- Incidentes envía reporte real.
- Plan alineado con reparto de trabajo del doc de equipo.

---

## 11. Orden de ejecución recomendado

```text
Fase 0 (prep)
    ↓
Fase 1 (seed + estudiantes)  ← DESBLOQUEADOR CRÍTICO
    ↓
Fase 2 (asistencias) ──┐
    ↓                  │
Fase 3 (calificaciones)┘
    ↓
Fase 4 (telegram)
    ↓
Fase 5 (sync remoto)  ← puede iniciarse parcialmente tras Fase 1
    ↓
Fase 6 (cierre)
```

**MVP demo-able:** Fases 0 → 1 → 2 → 4 (asistencia + telegram).  
**MVP completo local:** Fases 0 → 1 → 2 → 3 → 4.

---

## 12. Matriz de archivos por fase

| Archivo | F0 | F1 | F2 | F3 | F4 | F5 | F6 |
|---------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| `AppModule.kt` | ✓ | ✓ | | | ✓ | ✓ | |
| `AppDatabase.kt` | | ✓ | ✓ | ✓ | | | ✓ |
| `DatabaseSeeder.kt` *(nuevo)* | | ✓ | | ✓ | | | |
| `StudentRepository*.kt` | | ✓ | | | | ✓ | |
| `AttendanceScreen/ViewModel` | | | ✓ | | ✓ | ✓ | |
| `GradesScreen/ViewModel` | | | | ✓ | ✓ | ✓ | |
| `TelegramRepositoryImpl.kt` | | | | | ✓ | | ✓ |
| `TelegramMessageBuilder.kt` | | | | ✓ | ✓ | | |
| `RetrofitClient.kt` | | | | | | ✓ | |
| `NetworkMonitor.kt` | | | | | | ✓ | |
| `GradeDetailScreen.kt` | | | | | | | ✓ |

---

## 13. Checklist global pre-demo

- [ ] Fase 1 completada — estudiantes visibles.
- [ ] Fase 2 completada — asistencia guardable.
- [ ] Fase 3 completada — calificaciones editables y persistentes.
- [ ] Fase 4 completada — reportes Telegram confiables.
- [ ] `local.properties` configurado en cada máquina del equipo.
- [ ] Ningún secreto en commits.
- [ ] Build debug verificado en al menos 2 dispositivos/emuladores.
- [ ] Documentación actualizada al cerrar cada fase.

---

## 14. Reparto sugerido del equipo

| Persona | Fases | Entregable |
|---------|-------|------------|
| Dev A | 1 + 5.2 | Estudiantes en Room + fetch remoto |
| Dev B | 2 + 4 (asistencias) | Asistencias + reporte Telegram |
| Dev C | 3 + 4 (calificaciones) | Notas + boletín Telegram |
| Dev D | 5.1 + 5.3 | NetworkMonitor + sync pendientes |
| Dev E | 6 | Catálogos, incidentes, detalle real |

---

## 15. Registro de avance

Completar al cerrar cada fase:

| Fase | Responsable | Fecha inicio | Fecha fin | PR / commit | Notas |
|------|-------------|--------------|-----------|-------------|-------|
| 0 | | | | | |
| 1 | | | | | |
| 2 | | | | | |
| 3 | | | | | |
| 4 | | | | | |
| 5 | | | | | |
| 6 | | | | | |

---

## 16. Próximo paso inmediato

**Iniciar Fase 1:** crear `DatabaseSeeder.kt`, registrar callback en `AppModule` / `AppDatabase`, insertar estudiantes demo y verificar listas en Asistencias y Calificaciones.

Cuando el equipo confirme, desarrollar fase por fase siguiendo este documento como checklist maestro.
