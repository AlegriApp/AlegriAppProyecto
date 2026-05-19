# Plan de modelos para Asistencias y Calificaciones

## 1. Contexto del cambio

AlegriAPP ya cuenta con interfaces visuales para los módulos de Asistencias y Calificaciones. La siguiente etapa debe completar la base de datos, modelos, estados y contratos que ya existen en el proyecto para que esas pantallas puedan consumir datos reales, persistidos o simulados desde una fuente externa a los Composables.

Este plan divide el trabajo en dos Pull Request independientes:

| PR | Módulo | Archivos existentes a completar | Objetivo | Riesgo |
|----|--------|--------------------------------|----------|--------|
| PR 1 | Asistencias | `Student.kt`, `Attendance.kt`, `AttendanceStatus.kt`, `AttendanceUiState.kt`, `AttendanceEvent.kt`, `AttendanceViewModel.kt`, `StudentEntity.kt`, `AttendanceEntity.kt`, `StudentDao.kt`, `AttendanceDao.kt`, `StudentMapper.kt`, `AttendanceMapper.kt`, `AttendanceSyncRequest.kt`, contratos/repositorios existentes | Dejar la estructura de datos de asistencia lista para UI, Room, sincronización futura y reporte institucional | Medio: existe un `AttendanceStatus` en presentación que no debe duplicarse con el de dominio sin plan de migración |
| PR 2 | Calificaciones | `Grade.kt`, `GradesUiState.kt`, `GradesEvent.kt`, `GradeDetailUiState.kt`, `GradesViewModel.kt`, `GradeEntity.kt`, `GradeDao.kt`, `GradeMapper.kt`, `GradeSyncRequest.kt`, contratos/repositorios existentes | Dejar la estructura de datos de calificaciones lista para UI, promedios, boletines y sincronización futura | Medio: hoy los mocks y modelos visuales viven en `presentation/grades/components` |

## 2. Alcance general

- Completar únicamente archivos Kotlin ya existentes.
- Crear únicamente este documento de planificación.
- No crear carpetas nuevas.
- No crear archivos nuevos de Models, Entities, DTOs, DAOs, Mappers, UiState, Events, Actions, Repositories ni UseCases.
- No rediseñar pantallas.
- No conectar todavía Room, Retrofit ni Telegram con lógica real si eso exige ampliar estructura fuera de lo existente.
- Mantener la separación UI / Domain / Data descrita en `AlegriAPP_Guia_Estructura_Proyecto.md`.

## 3. Principios técnicos obligatorios

- Usar `data class` para modelos inmutables.
- Usar `enum class` o `sealed class` para estados cerrados.
- Usar `sealed class` o `sealed interface` para eventos y estados de carga, éxito y error.
- Reutilizar `core/common/ResultState.kt` cuando sea completado; no crear otro `Resource`, `NetworkResult` o `UiState` global.
- Reutilizar `states/UiState.kt` solo si el equipo decide mantener ese paquete legado; evitar duplicar la responsabilidad con `core/common/ResultState.kt`.
- Evitar lógica de negocio pesada dentro de Composables.
- Mover datos simulados fuera de pantallas hacia estructuras existentes cuando sea posible.
- Preservar nombres de Composables, rutas de navegación, colores, paddings, TopBars, Cards y tema.
- Mantener soporte conceptual para Room, Retrofit/API REST, Telegram Bot API, StateFlow, MVVM y uso offline.

## 4. Estructura real encontrada en el proyecto

La estructura real ya contiene las capas recomendadas:

```text
app/src/main/java/com/example/myapplication/
├── core/
│   ├── common/
│   │   ├── Constants.kt
│   │   ├── DateUtils.kt
│   │   └── ResultState.kt
│   ├── navigation/
│   ├── network/
│   └── permissions/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   └── entity/
│   ├── mapper/
│   ├── remote/
│   │   ├── api/
│   │   └── dto/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── attendance/
│   ├── grades/
│   ├── incidents/
│   ├── ocr/
│   ├── components/
│   └── home/
├── services/
└── states/
```

Hallazgo importante: muchas clases estructurales existen pero están vacías. Esto favorece completar la arquitectura sin crear archivos nuevos.

## 5. Comparación entre estructura real y guía del MD

| Elemento | Guía del MD | Estado real | Decisión |
|----------|-------------|-------------|----------|
| `domain/model` | Contiene modelos principales | Existe, pero `Student.kt`, `Attendance.kt`, `AttendanceStatus.kt` y `Grade.kt` están vacíos | Completar archivos existentes |
| `data/local/entity` | Contiene entidades Room | Existe, pero entidades principales están vacías | Completar archivos existentes |
| `data/local/dao` | Contiene DAOs Room | Existe, pero DAOs principales están vacíos | Completar archivos existentes |
| `data/remote/dto` | Contiene DTOs de sincronización | Existe, pero DTOs principales están vacíos | Completar archivos existentes |
| `data/mapper` | Contiene mappers dominio/data | Existe, pero mappers principales están vacíos | Completar archivos existentes |
| `presentation/attendance` | Pantalla, estado, evento y ViewModel | Pantalla y UiState existen; Event y ViewModel están vacíos | Completar estado/evento sin tocar diseño |
| `presentation/grades` | Pantalla, estado, evento y ViewModel | Pantallas, estado y eventos existen; ViewModel vacío | Completar modelos de estado y contratos sin tocar diseño |
| `core/common/ResultState.kt` | Estado genérico recomendado | Existe vacío | Completar y reutilizar; no duplicar |
| `states/UiState.kt` | No aparece como capa final principal | Existe con sealed class genérica antigua | Reutilizar solo si ya hay dependencias; preferir `ResultState` para repositorios |

## 6. Archivos existentes que se deben completar

| Archivo existente | Estado actual | Acción propuesta | Carpeta actual | Justificación |
|------------------|---------------|-----------------|----------------|---------------|
| `Student.kt` | Vacío | Definir `data class Student` con id, nombre completo, grado/sección y datos de representante/Telegram | `domain/model` | Modelo compartido por asistencia y calificaciones |
| `AttendanceStatus.kt` | Vacío | Definir `enum class AttendanceStatus { PRESENT, LATE, ABSENT, UNMARKED }` | `domain/model` | Estado cerrado del dominio de asistencia |
| `Attendance.kt` | Vacío | Definir `data class Attendance` con `studentId`, `date`, `status`, `synced` | `domain/model` | Registro diario offline/sincronizable |
| `Grade.kt` | Vacío | Definir `data class Grade`; si no existen `Subject`/`Period`/`Activity`, usar campos existentes temporales como texto o ids simples | `domain/model` | Registro base para notas |
| `ResultState.kt` | Vacío | Definir sealed class genérica `Loading`, `Success`, `Error` | `core/common` | Evita duplicar `Resource` o `NetworkResult` |
| `AttendanceUiState.kt` | Parcial | Alinear con modelos de dominio gradualmente; retirar duplicación de `AttendanceStatus` cuando UI migre al dominio | `presentation/attendance` | Hoy contiene mock UI y enum local |
| `AttendanceEvent.kt` | Vacío | Definir eventos de marcado, guardado, reporte y fecha si aplica | `presentation/attendance` | Contrato MVVM para pantalla existente |
| `AttendanceViewModel.kt` | Vacío | Preparar `StateFlow<AttendanceUiState>` y manejo de eventos mínimo, sin lógica compleja | `presentation/attendance` | Conectar UI a datos en fase posterior |
| `StudentEntity.kt` | Vacío | Definir entidad Room de estudiante | `data/local/entity` | Persistencia local compartida |
| `AttendanceEntity.kt` | Vacío | Definir entidad Room con `status` serializado y `synced` | `data/local/entity` | Soporte offline |
| `StudentDao.kt` | Vacío | Definir consultas básicas de estudiantes | `data/local/dao` | Fuente local para UI |
| `AttendanceDao.kt` | Vacío | Definir observación por fecha, guardado y pendientes de sync | `data/local/dao` | Persistencia de asistencia |
| `StudentMapper.kt` | Vacío | Convertir `StudentEntity` ↔ `Student` | `data/mapper` | Separación Data/Domain |
| `AttendanceMapper.kt` | Vacío | Convertir `AttendanceEntity` ↔ `Attendance` y validar status | `data/mapper` | Evita lógica de conversión en UI |
| `AttendanceSyncRequest.kt` | Vacío | Definir DTO mínimo para sincronización futura | `data/remote/dto` | Preparación Retrofit/API REST |
| `GradeEntity.kt` | Vacío | Definir entidad Room para notas | `data/local/entity` | Persistencia local de calificaciones |
| `GradeDao.kt` | Vacío | Definir consultas por estudiante, materia, período y pendientes | `data/local/dao` | Base para promedios y boletín |
| `GradeMapper.kt` | Vacío | Convertir `GradeEntity` ↔ `Grade` | `data/mapper` | Separación Data/Domain |
| `GradeSyncRequest.kt` | Vacío | Definir DTO mínimo para sincronización futura | `data/remote/dto` | Preparación Retrofit/API REST |
| `GradesUiState.kt` | Parcial | Cambiar gradualmente de mocks visuales a modelos de dominio o adaptadores existentes | `presentation/grades` | Hoy depende de `GradeStudentMock` |
| `GradesEvent.kt` | Parcial | Agregar eventos de actualización/guardado de nota si la UI los necesita | `presentation/grades` | Contrato MVVM |
| `GradeDetailUiState.kt` | Parcial | Mantener sealed interface; reemplazar modelos mock por dominio cuando exista adaptación | `presentation/grades` | Ya modela Loading/Empty/Offline/Error/Success |
| `GradesViewModel.kt` | Vacío | Preparar `StateFlow<GradesUiState>` y eventos básicos | `presentation/grades` | Conectar pantalla a datos en fase posterior |

## 7. Archivos existentes que se deben reutilizar

| Archivo | Uso recomendado | Motivo |
|---------|-----------------|--------|
| `AlegriAPP_Guia_Estructura_Proyecto.md` | Fuente principal de arquitectura esperada | Define UI / Domain / Data, MVVM, Room, Retrofit, Telegram y estados |
| `core/common/ResultState.kt` | Resultado genérico para repositorios/usecases | Ya existe; no crear duplicado |
| `states/UiState.kt` | Revisar antes de tocar; posible legado | Ya existe otro estado genérico |
| `presentation/attendance/AttendanceUiState.kt` | Estado temporal de pantalla | Ya contiene mock y `Saver` usados por UI |
| `presentation/grades/GradesUiState.kt` | Estado temporal de pantalla | Ya alimenta filtros y mock visual |
| `presentation/grades/GradeDetailUiState.kt` | Estado de detalle con sealed interface | Ya representa carga, vacío, offline, error y éxito |
| `presentation/grades/components/GradesMockData.kt` | Fuente temporal de mocks de calificaciones | Evita hardcodear datos directamente en `GradesScreen` |
| `presentation/grades/components/GradeDetailMockData.kt` | Fuente temporal de detalle académico | Puede asumir `GradeReport`/boletín temporalmente |
| `presentation/attendance/attendanceMockUiState()` | Fuente temporal de mocks de asistencia | Ya existe dentro de `AttendanceUiState.kt`; no crear archivo mock nuevo |
| `data/repository/*RepositoryImpl.kt` | Implementaciones futuras | Existen vacías; se completan solo si el PR lo requiere |
| `domain/repository/*Repository.kt` | Contratos futuros | Existen vacíos; no crear contratos paralelos |
| `domain/usecase/attendance/*` y `domain/usecase/grades/*` | Casos de uso futuros | Existen vacíos; completar solo si se requiere para compilar integración |

## 8. Archivos que no se deben modificar

| Archivo o carpeta | Motivo |
|-------------------|--------|
| `presentation/attendance/AttendanceScreen.kt` | Es interfaz gráfica ya construida; no cambiar diseño ni estructura visual |
| `presentation/attendance/components/AttendanceListCard.kt` | Componente visual; solo tocar en PR futuro si se cambia el contrato de datos de forma mínima |
| `presentation/attendance/components/AttendanceStudentItem.kt` | Componente visual; no cambiar colores, paddings ni botones |
| `presentation/grades/GradesScreen.kt` | Es interfaz gráfica ya construida; no cambiar diseño |
| `presentation/grades/GradeDetailScreen.kt` | Es interfaz gráfica de detalle; no cambiar diseño |
| `presentation/grades/components/*Card.kt` | Componentes visuales; evitar cambios durante PR de modelos |
| `presentation/grades/components/GradeFilterSection.kt` | Filtros visuales; no cambiar comportamiento salvo adaptación mínima |
| `ui/theme/*` y `res/values/*` | Tema y recursos visuales fuera del alcance |
| `core/navigation/*` | No cambiar rutas ni navegación |
| `MainActivity.kt` y `AlegriApp.kt` | Entrada de app fuera del alcance |
| `presentation/incidents/*` | Otro módulo, fuera de estos PR salvo modelos compartidos ya existentes |
| `presentation/ocr/*` | OCR queda preparado conceptualmente, no se implementa en estos PR |

## 9. Archivos que NO se crearán aunque serían recomendables

| Archivo recomendado | Existe actualmente | ¿Crear ahora? | Motivo |
|--------------------|-------------------|--------------|--------|
| `Representative.kt` | No | No | Usar campos en `Student.kt` temporalmente; separar representante en fase futura |
| `AttendanceSummary.kt` | No | No | Usar temporalmente propiedades calculadas en `AttendanceUiState` o funciones existentes |
| `AttendanceReport.kt` | No | No | Preparar datos de reporte desde `Attendance.kt`, `Student.kt` y usecase existente |
| `AttendanceRequestDto.kt` | No | No | Existe `AttendanceSyncRequest.kt`; usarlo para sincronización futura |
| `AttendanceResponseDto.kt` | No | No | Registrar como deuda técnica; no hay archivo actual |
| `AttendanceReportDto.kt` | No | No | Registrar como deuda técnica; el reporte puede usar `AttendanceSyncRequest.kt` temporalmente |
| `AttendanceAction.kt` | No | No | Usar `AttendanceEvent.kt` existente |
| `Subject.kt` | No | No | Usar campos de `Grade.kt` o listas existentes en `GradesMockData.kt` temporalmente |
| `AcademicPeriod.kt` | No | No | Usar campo `period` en `Grade.kt` y `gradesPeriods` temporalmente |
| `GradeActivity.kt` | No | No | Usar campo `activity` o `activityId` dentro de `Grade.kt` si se decide |
| `GradeSummary.kt` | No | No | Usar cálculo en `GradesUiState`/ViewModel existente |
| `GradeReport.kt` | No | No | Usar `GradeDetailUiState` y `GradeDetailMockData.kt` temporalmente |
| `GradeStatus.kt` | No | No | Usar `GradeVisualStatus` temporalmente; migrar a dominio en fase futura si se crea archivo |
| `SubjectEntity.kt` | No | No | No crear entidad por ahora; guardar materia en `GradeEntity` como texto/id simple |
| `AcademicPeriodEntity.kt` | No | No | No crear entidad por ahora; guardar período en `GradeEntity` |
| `GradeActivityEntity.kt` | No | No | No crear entidad por ahora; guardar actividad en `GradeEntity` |
| `GradeRequestDto.kt` | No | No | Existe `GradeSyncRequest.kt`; usarlo |
| `GradeResponseDto.kt` | No | No | Registrar como deuda técnica |
| `GradeReportDto.kt` | No | No | Registrar como deuda técnica |
| `GradeBulletinDto.kt` | No | No | Registrar como deuda técnica; boletín futuro por Telegram |
| `GradesAction.kt` | No | No | Usar `GradesEvent.kt` existente |

## 10. Fase 1 - PR de Asistencias

### Objetivo

Completar los modelos y estructuras existentes para que Asistencias pueda representar estudiantes, registros diarios, estados, persistencia local y sincronización futura sin depender de Compose ni de datos hardcodeados en la pantalla.

### Archivos a completar en PR 1

| Archivo | Acción técnica |
|---------|----------------|
| `domain/model/Student.kt` | Crear `data class Student` con `id`, `fullName`, `grade`, `section`, `representativeName`, `telegramChatId` |
| `domain/model/AttendanceStatus.kt` | Crear enum de dominio `PRESENT`, `LATE`, `ABSENT`, `UNMARKED` |
| `domain/model/Attendance.kt` | Crear `data class Attendance(id, studentId, date, status, synced)` |
| `presentation/attendance/AttendanceUiState.kt` | Mantener compatibilidad con UI; planear migración del enum local hacia `domain.model.AttendanceStatus` |
| `presentation/attendance/AttendanceEvent.kt` | Agregar eventos `MarkStudent`, `SaveAttendance`, `SendReport`, opcionalmente `DateChanged` |
| `presentation/attendance/AttendanceViewModel.kt` | Preparar StateFlow y reducer mínimo si se integra sin tocar UI visual |
| `data/local/entity/StudentEntity.kt` | Crear entidad Room de estudiantes |
| `data/local/entity/AttendanceEntity.kt` | Crear entidad Room de asistencia con `status: String` y `synced` |
| `data/local/dao/StudentDao.kt` | Agregar observación/listado e inserción/reemplazo |
| `data/local/dao/AttendanceDao.kt` | Agregar consulta por fecha, guardado y pendientes de sync |
| `data/mapper/StudentMapper.kt` | Mapear entidad ↔ dominio |
| `data/mapper/AttendanceMapper.kt` | Mapear entidad ↔ dominio, cuidando `AttendanceStatus.valueOf` |
| `data/remote/dto/AttendanceSyncRequest.kt` | DTO mínimo para envío futuro de registros |
| `core/common/ResultState.kt` | Completar si se usa en repositorios/usecases |

### Reglas de modelo

- Un estudiante puede tener un estado diario: `PRESENT`, `LATE`, `ABSENT` o `UNMARKED`.
- Cada asistencia debe asociarse a fecha y estudiante.
- Cada registro debe conocer si fue sincronizado (`synced`).
- El resumen debe calcular presentes, atrasados, ausentes y sin marcar, sin meter ese cálculo en Composables.
- El reporte debe poder prepararse para inspección o autoridades.
- El modelo no debe importar Compose.
- El soporte offline debe quedar reflejado en entidades y DAOs.

### Datos simulados actuales

Hoy asistencia usa `attendanceMockUiState()` dentro de `AttendanceUiState.kt`. Como ese archivo ya existe, puede asumir temporalmente la fuente mock. En una fase posterior, el mock debe reemplazarse por `StudentRepository`, `AttendanceRepository` o ViewModel, todos ya existentes pero vacíos.

## 11. Fase 2 - PR de Calificaciones

### Objetivo

Completar las estructuras existentes para que Calificaciones pueda representar notas por estudiante, materia, período y actividad, calcular promedios básicos, identificar estados académicos y preparar boletines sin rediseñar la UI.

### Archivos a completar en PR 2

| Archivo | Acción técnica |
|---------|----------------|
| `domain/model/Grade.kt` | Crear `data class Grade` con id, estudiante, materia/período/actividad y `score` |
| `presentation/grades/GradesUiState.kt` | Alinear con modelos de dominio sin romper componentes que usan `GradeStudentMock` |
| `presentation/grades/GradesEvent.kt` | Mantener eventos existentes y agregar actualización/guardado si la UI lo requiere |
| `presentation/grades/GradeDetailUiState.kt` | Conservar sealed interface; migrar `GradeDetailStudent` a dominio solo si no exige archivos nuevos |
| `presentation/grades/GradesViewModel.kt` | Preparar StateFlow y eventos básicos |
| `data/local/entity/GradeEntity.kt` | Crear entidad Room con `studentId`, `subject`, `period`, `activity`, `score`, `synced` |
| `data/local/dao/GradeDao.kt` | Consultas por estudiante, materia, período y pendientes de sync |
| `data/mapper/GradeMapper.kt` | Mapear entidad ↔ dominio |
| `data/remote/dto/GradeSyncRequest.kt` | DTO mínimo para sincronización futura |
| `core/common/ResultState.kt` | Reutilizar si ya fue completado en PR 1 |

### Reglas de modelo

- Una calificación pertenece a estudiante, materia, período y actividad.
- La nota debe validarse en rango, por ejemplo 0.0 a 20.0 si la UI trabaja sobre 20.
- El sistema debe calcular promedio por estudiante, por materia y promedio general de sección.
- Debe identificar aprobados, en riesgo o sin nota.
- Debe preparar información para boletín académico y envío futuro por Telegram.
- Debe soportar sincronización futura y trabajo offline.
- No debe depender de Compose.
- No debe mezclar lógica de UI con lógica de datos.

### Datos simulados actuales

Calificaciones usa `GradesMockData.kt` y `GradeDetailMockData.kt` en `presentation/grades/components`. Como esos archivos ya existen, pueden mantenerse como fuente temporal. No crear `GradeSummary.kt`, `GradeReport.kt`, `Subject.kt` ni `AcademicPeriod.kt`; registrar la deuda técnica y usar `Grade.kt`/`GradesUiState.kt` mientras tanto.

## 12. Checklist por PR

### PR 1 - Asistencias

- [ ] Revisar estructura real del proyecto.
- [ ] Revisar `AlegriAPP_Guia_Estructura_Proyecto.md`.
- [ ] Identificar archivos existentes relacionados con asistencia.
- [ ] Completar modelos de dominio existentes.
- [ ] Completar enum de asistencia existente.
- [ ] Completar `AttendanceUiState.kt` sin romper `Saver` ni UI.
- [ ] Completar `AttendanceEvent.kt`.
- [ ] Completar entidades Room existentes.
- [ ] Completar DAOs existentes.
- [ ] Completar mappers existentes.
- [ ] Completar DTO existente de sincronización futura.
- [ ] No crear archivos nuevos.
- [ ] No crear carpetas nuevas.
- [ ] No duplicar modelos.
- [ ] Verificar que la pantalla compila sin cambios visuales.
- [ ] No tocar Calificaciones salvo `Student.kt` o `ResultState.kt` compartidos.

### PR 2 - Calificaciones

- [ ] Revisar estructura real del proyecto.
- [ ] Revisar `AlegriAPP_Guia_Estructura_Proyecto.md`.
- [ ] Identificar archivos existentes relacionados con calificaciones.
- [ ] Completar modelo de dominio `Grade.kt`.
- [ ] Completar `GradesUiState.kt` y `GradesEvent.kt` si hace falta.
- [ ] Completar entidades Room existentes.
- [ ] Completar DAO existente.
- [ ] Completar mapper existente.
- [ ] Completar DTO existente de sincronización futura.
- [ ] No crear archivos nuevos.
- [ ] No crear carpetas nuevas.
- [ ] No duplicar modelos.
- [ ] Verificar cálculo básico de promedios en ViewModel o estado, no en Composable.
- [ ] No tocar Asistencias salvo modelos compartidos ya existentes.

## 13. Criterios de aceptación

- El proyecto compila.
- No se rompe la navegación existente.
- No se modifica el diseño visual de pantallas.
- No se crean archivos nuevos de código.
- No se crean carpetas nuevas.
- No se duplican modelos.
- Los modelos existentes quedan completos y consistentes.
- Los nombres respetan paquetes y estructura actual.
- No hay lógica de datos quemada dentro de Composables si puede vivir en una estructura existente.
- Cada PR puede revisarse de forma independiente.
- Los modelos quedan listos para integrarse luego con ViewModel, Room, Retrofit y Telegram.
- Los modelos compartidos existentes se reutilizan.
- Los modelos faltantes recomendados quedan documentados como fase futura.

## 14. Recomendaciones para no romper la UI existente

- No cambiar nombres de Composables existentes.
- No cambiar rutas de navegación.
- No cambiar colores, TopBars, Cards, paddings ni tema global.
- No introducir dependencias nuevas sin necesidad.
- No conectar APIs reales todavía si no hay implementación estable.
- Mantener mocks en archivos existentes hasta que ViewModel/repositorios estén listos.
- No mover clases visuales de `components` si eso obliga a tocar muchas pantallas.
- Si se reemplaza `AttendanceStatus` local por el de dominio, hacerlo con imports y compatibilidad mínima.
- Si se reemplaza `GradeVisualStatus`, evitar crear `GradeStatus.kt`; usar el enum existente o documentar la deuda.
- Hacer commits pequeños y revisables.

## 15. Orden sugerido de commits

### PR 1 - Asistencias

1. `docs: add model completion plan for attendance and grades`
2. `refactor(attendance): complete existing attendance domain models`
3. `refactor(attendance): complete existing attendance ui state and events`
4. `refactor(attendance): complete existing local data structures`
5. `refactor(attendance): align mock data with existing structure`

### PR 2 - Calificaciones

1. `refactor(grades): complete existing grade domain models`
2. `refactor(grades): complete existing grade ui state and events`
3. `refactor(grades): complete existing local data structures`
4. `refactor(grades): complete existing summary and report structures`
5. `refactor(grades): align mock data with existing structure`

## 16. Notas de deuda técnica

- `ResultState.kt` está vacío y debe completarse antes de crear cualquier estado equivalente.
- `states/UiState.kt` existe con una sealed class genérica antigua; revisar uso antes de eliminar o reemplazar.
- `AttendanceStatus` existe conceptualmente duplicado: archivo de dominio vacío y enum local en `AttendanceUiState.kt`. La migración debe evitar romper imports de los componentes.
- `GradeVisualStatus` existe en `GradesMockData.kt` y cumple temporalmente la función de estado visual. No crear `GradeStatus.kt` en esta fase.
- Los repositorios, usecases, DAOs, entidades, mappers y DTOs ya existen pero están vacíos. Esto permite avanzar sin crear archivos nuevos.
- `IncidentSyncRequest.Powershell.kt` tiene un nombre sospechoso, pero no pertenece al alcance de estos PR.
