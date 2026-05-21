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

Pendiente grande:
- En el repo actual no esta implementado el modulo completo.

Falta construir:
- `IncidentEntity`, `IncidentDao`, repositorio, casos de uso, ViewModel, UI state/event.
- Persistencia y consulta de `incidentes` + `seguimiento_incidente`.
- Integracion de envio por Telegram para incidentes.
- Estados y severidad segun SQL (`abierto/en_seguimiento/cerrado/archivado`, `bajo/medio/alto/critico`).

## 8. Checklist rapido antes de demo

- [ ] `TELEGRAM_BOT_TOKEN` y `TELEGRAM_CHAT_ID` configurados localmente.
- [ ] Probar envio real de reporte de asistencia.
- [ ] Probar envio real de boletin de calificaciones.
- [ ] Verificar que no haya datos quemados en pantallas principales.
- [ ] Confirmar que OCR no guarda automaticamente sin revision humana.
- [ ] Validar build debug en otra maquina del equipo.
- [ ] Confirmar que no se suben secretos a git.

## 9. Recomendacion para repartir trabajo (equipo)

- Persona 1: Catalogos SQL -> Room/API (`materias`, `periodos`, `tipos_evaluacion`, `cursos`).
- Persona 2: Permisos por rol + restricciones UI en Calificaciones.
- Persona 3: Incidentes end-to-end (data/domain/presentation).
- Persona 4: Telegram avanzado (`configuracion_telegram` + `notificaciones` + reintentos).
- Persona 5: OCR avanzado (parser de actas, validacion, CameraX, registro ML).

