# AlegriApp

> Aplicación móvil Android para la gestión escolar de **Fe y Alegría**: registro de
> asistencia, calificaciones e incidentes, con funcionamiento **offline-first** y
> sincronización en la nube.

AlegriApp es una aplicación nativa de Android (Kotlin + Jetpack Compose) construida sobre
una arquitectura **MVVM + Clean Architecture**. Está pensada para que los/as docentes de
los centros educativos de Fe y Alegría puedan registrar la información del aula —asistencia
diaria, calificaciones e incidentes de convivencia— **incluso sin conexión a internet**,
y que esos datos se **sincronicen automáticamente** con el backend cuando el dispositivo
recupera la red.

### El problema que resuelve

En muchos centros la conectividad es intermitente o inexistente dentro del aula. AlegriApp
adopta un enfoque **offline-first**: todo se guarda primero en la base de datos local
([Room](https://developer.android.com/training/data-storage/room)) y un proceso en segundo
plano ([WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager))
se encarga de subir los cambios pendientes a **Supabase** (vía su API REST PostgREST) en
cuanto hay internet. Además, los reportes relevantes (asistencia, calificaciones e
incidentes) pueden enviarse automáticamente a los/as representantes mediante un **bot de
Telegram**.

---

## 2. Características principales

| Módulo | Descripción |
|---|---|
| 📋 **Asistencia** | Registro diario de asistencia por curso (presente / ausente). Incluye lectura asistida de hojas de asistencia mediante **OCR** (ML Kit) para agilizar la captura. |
| 📝 **Calificaciones** | Registro y consulta de calificaciones filtradas por curso, materia, tipo de evaluación y periodo académico. Estados de calificación: registrado, revisado, publicado, anulado. |
| ⚠️ **Incidentes** | Registro de incidentes de convivencia con tipo, nivel de gravedad (bajo, medio, alto, crítico) y estado de seguimiento. Los catálogos de incidentes se sincronizan desde el servidor (modo *pull*). |
| 📲 **Notificaciones por Telegram** | Envío de reportes a representantes a través de un bot de Telegram (Telegram Bot API), usando el `chat_id` configurado por estudiante/representante. |
| 🔌 **Modo offline (offline-first)** | Todo se persiste localmente en Room. Un `SyncWorker` (WorkManager) sincroniza los cambios pendientes con Supabase al recuperar conexión, con notificaciones del sistema sobre el estado de sincronización. |
| 🔐 **Autenticación** | Inicio de sesión de docentes contra la tabla de usuarios de Supabase, con preferencias de sesión persistidas en DataStore. |

---

## 3. Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | [Kotlin](https://kotlinlang.org/) | `2.0.21` |
| UI | [Jetpack Compose](https://developer.android.com/jetpack/compose) (BOM) + Material 3 | BOM `2024.12.01` |
| Arquitectura | MVVM + Clean Architecture (capas `presentation` / `domain` / `data` / `core`) | — |
| Inyección de dependencias | DI **manual** mediante `core/di/AppModule` (sin Hilt/Dagger) | — |
| Navegación | Navigation Compose | `2.8.5` |
| Base de datos local | [Room](https://developer.android.com/training/data-storage/room) (esquema en versión `10`) | `2.6.1` |
| Backend remoto | [Supabase](https://supabase.com/) consumido vía su API REST **PostgREST** | — |
| Cliente de red | [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) + Gson | `2.11.0` / `4.12.0` / `2.11.0` |
| Sincronización en background | [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | `2.9.1` |
| Preferencias / sesión | [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) | `1.1.1` |
| Mensajería | [Telegram Bot API](https://core.telegram.org/bots/api) consumida vía Retrofit | — |
| OCR | [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition) | `16.0.1` |
| Asincronía | Kotlin Coroutines + kotlinx.serialization | `1.9.0` / `1.7.3` |
| Build | Android Gradle Plugin + KSP + Gradle Wrapper | AGP `8.7.3` · KSP `2.0.21-1.0.27` · Gradle `8.9` |

> ℹ️ **Nota importante sobre Supabase y Telegram:** el proyecto **no** utiliza el SDK
> oficial de Supabase ni un SDK de Telegram. Ambos servicios se consumen **directamente como
> APIs REST mediante Retrofit/OkHttp** (ver `data/remote/api/SupabaseApiService.kt` y
> `data/remote/api/TelegramApiService.kt`). Por lo tanto, no aparecen como dependencias
> Gradle de terceros.

---

## 4. Requisitos previos

- **Android Studio** Ladybug (2024.2.1) o superior — necesario para AGP `8.7.3` y Gradle `8.9`.
- **JDK 21** (el proyecto compila con `sourceCompatibility`/`targetCompatibility` = `VERSION_21` y `jvmTarget = "21"`). Android Studio incluye un JBR 21 compatible.
- **Android SDK**
  - `compileSdk` / `targetSdk`: **36**
  - `minSdk`: **26** (Android 8.0 *Oreo*)
- Un **emulador** o **dispositivo físico** con Android 8.0 (API 26) o superior.
- Acceso a un proyecto de **Supabase** (URL + API key) y, opcionalmente, un **bot de Telegram** (token + `chat_id`) para las notificaciones.
- Conexión a internet para la primera descarga de dependencias Gradle.

---

## 5. Instalación paso a paso

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/AlegriApp/AlegriAppProyecto.git
   cd AlegriAppProyecto
   ```

2. **Abrir el proyecto en Android Studio:**
   `File → Open…` y selecciona la carpeta raíz del proyecto (`AlegriAppProyecto`).

3. **Verificar el JDK del proyecto:**
   `Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK` debe apuntar a un **JDK 21** (por ejemplo el JBR que incluye Android Studio).

4. **Instalar los componentes del SDK** que solicite Android Studio (SDK Platform 36 y Build-Tools).

5. **Crear el archivo `local.properties`** en la raíz del proyecto con tus credenciales
   (ver [sección 6](#6-variables-de-entorno--configuración)). Este archivo **no se versiona**
   (está en `.gitignore`).

6. **Sincronizar Gradle:**
   `File → Sync Project with Gradle Files` (o el botón del elefante 🐘). En la primera
   sincronización se descargarán todas las dependencias.

7. **Compilar el proyecto** para confirmar que todo está correcto:
   ```bash
   ./gradlew assembleDebug
   ```

8. **Ejecutar la app** en un emulador o dispositivo (ver [sección 7](#7-cómo-ejecutar-el-proyecto-localmente)).

---

## 6. Variables de entorno / configuración

La configuración sensible se inyecta en tiempo de compilación desde `local.properties`
(en la raíz del proyecto, **no versionado**) hacia el `BuildConfig` de la app, según se
define en [`app/build.gradle.kts`](app/build.gradle.kts).

> ⚠️ **Nunca** subas `local.properties` ni credenciales reales al repositorio. Los valores
> de abajo son **ejemplos**.

### Cómo crear el archivo `local.properties` paso a paso

1. **Ubícate en la raíz del proyecto** (la carpeta `AlegriAppProyecto/`, donde están
   `settings.gradle.kts` y `gradlew`). El archivo debe quedar **al mismo nivel** que esos
   archivos, **no** dentro de `app/`.

2. **Crea el archivo.** Tienes dos opciones:

   - **Opción A — Android Studio:** si ya abriste el proyecto, normalmente Android Studio crea
     `local.properties` automáticamente con la línea `sdk.dir` al sincronizar Gradle. Si no
     existe, usa `File → New → File`, nómbralo `local.properties` y colócalo en la raíz.

   - **Opción B — Terminal:** desde la raíz del proyecto ejecuta:
     ```bash
     # macOS / Linux
     touch local.properties

     # Windows (PowerShell)
     New-Item local.properties
     ```

3. **Añade la ruta del SDK de Android** (si Android Studio no la puso ya). Es la carpeta donde
   tienes instalado el SDK; la encuentras en `Settings → Languages & Frameworks → Android SDK`:
   - macOS: `/Users/TU_USUARIO/Library/Android/sdk`
   - Windows: `C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk`
   - Linux: `/home/TU_USUARIO/Android/Sdk`

4. **Pega las variables del proyecto** (Supabase, Telegram e IDs por defecto) usando la
   plantilla de abajo y **reemplaza** cada `TU_..._AQUI` por tus valores reales.

5. **Verifica que NO se versiona.** El archivo ya está listado en `.gitignore`; confírmalo con:
   ```bash
   git check-ignore local.properties
   ```
   Si el comando imprime `local.properties`, está correctamente ignorado. **Nunca** lo subas
   al repositorio.

6. **Sincroniza Gradle** (`File → Sync Project with Gradle Files`) para que los valores se
   inyecten en el `BuildConfig` y la app pueda leerlos.

> 💡 Si dejas vacía u omites alguna variable, la app usará los valores por defecto definidos en
> [`app/build.gradle.kts`](app/build.gradle.kts) (ver la tabla y la nota de seguridad más abajo).

### Plantilla de `local.properties`

Crea el archivo con el siguiente contenido (sustituyendo los valores de ejemplo por los tuyos):

```properties
# --- SDK de Android (lo genera Android Studio automáticamente) ---
sdk.dir=/ruta/a/tu/Android/sdk

# --- Supabase ---
SUPABASE_URL=TU_SUPABASE_URL_AQUI
SUPABASE_KEY=TU_SUPABASE_API_KEY_AQUI

# --- Telegram Bot ---
TELEGRAM_BOT_TOKEN=TU_TELEGRAM_BOT_TOKEN_AQUI
TELEGRAM_CHAT_ID=TU_TELEGRAM_CHAT_ID_AQUI

# --- IDs por defecto del backend (opcionales, tienen valor por defecto) ---
SUPABASE_DEFAULT_CURSO_ID=1
SUPABASE_DEFAULT_MATERIA_ID=1
SUPABASE_DEFAULT_TIPO_EVALUACION_ID=6
SUPABASE_DEFAULT_PERIODO_ID=1
SUPABASE_DEFAULT_TIPO_INCIDENTE_ID=1
SUPABASE_DEFAULT_REPORTADO_POR_ID=0
```

### Detalle de cada variable

| Variable | ¿Qué es? | ¿Dónde se coloca? | ¿Obligatoria? |
|---|---|---|---|
| `SUPABASE_URL` | URL base de la API REST (PostgREST) de tu proyecto Supabase, p. ej. `https://<proyecto>.supabase.co/rest/v1/`. | `local.properties` | Recomendada *(ver nota de seguridad)* |
| `SUPABASE_KEY` | API key de Supabase (anon/service key) usada en las cabeceras de autenticación. | `local.properties` | **Sí** |
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram (lo entrega *@BotFather*) para enviar notificaciones. | `local.properties` | Sí, si se usa Telegram |
| `TELEGRAM_CHAT_ID` | `chat_id` por defecto al que se envían los mensajes de prueba/fallback. | `local.properties` | Opcional |
| `SUPABASE_DEFAULT_CURSO_ID` | ID de curso por defecto en el backend. | `local.properties` | Opcional (def. `1`) |
| `SUPABASE_DEFAULT_MATERIA_ID` | ID de materia por defecto. | `local.properties` | Opcional (def. `1`) |
| `SUPABASE_DEFAULT_TIPO_EVALUACION_ID` | ID de tipo de evaluación por defecto. | `local.properties` | Opcional (def. `6`) |
| `SUPABASE_DEFAULT_PERIODO_ID` | ID de periodo académico por defecto. | `local.properties` | Opcional (def. `1`) |
| `SUPABASE_DEFAULT_TIPO_INCIDENTE_ID` | ID de tipo de incidente por defecto. | `local.properties` | Opcional (def. `1`) |
| `SUPABASE_DEFAULT_REPORTADO_POR_ID` | ID del usuario que reporta por defecto. | `local.properties` | Opcional (def. `0`) |

> 🔒 **Aviso de seguridad:** actualmente `app/build.gradle.kts` define una **URL de Supabase
> por defecto embebida** que se usa si `SUPABASE_URL` no está en `local.properties`. Se
> recomienda **eliminar ese valor por defecto** y exigir siempre la variable de entorno, para
> no exponer endpoints reales en el control de versiones.

---

## 7. Cómo ejecutar el proyecto localmente

### Desde Android Studio
1. Selecciona un dispositivo/emulador en la barra superior.
2. Elige la configuración de ejecución **`app`**.
3. Pulsa **Run ▶** (o `Shift + F10`).

### Desde la línea de comandos

> En Windows usa `gradlew.bat` en lugar de `./gradlew`.

```bash
# Compilar en modo debug
./gradlew assembleDebug

# Instalar y ejecutar en un dispositivo/emulador conectado
./gradlew installDebug

# Generar el APK de release (sin firmar)
./gradlew assembleRelease
```

El APK generado queda en:

```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

> ℹ️ El build de `release` tiene la ofuscación desactivada (`isMinifyEnabled = false`) y el
> APK se genera **sin firmar**. Para distribuir necesitarás configurar tu propio
> `signingConfig`.

```bash
# Ejecutar las pruebas unitarias
./gradlew test

# Limpiar artefactos de compilación
./gradlew clean
```

---

## 8. Estructura de carpetas

Paquete principal: **`com.example.myapplication`**
*(el `namespace` y `applicationId` mantienen el identificador por defecto de Android Studio).*

```
AlegriAppProyecto/
├── app/
│   ├── build.gradle.kts          # Configuración del módulo app (SDK, BuildConfig, deps)
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/              # Recursos (iconos, temas, strings, XML de backup)
│       │   └── java/com/example/myapplication/
│       │       ├── AlegriApp.kt          # Composable raíz: arranca sync y navegación
│       │       ├── MainActivity.kt       # Activity única (single-activity + Compose)
│       │       │
│       │       ├── core/                 # Infraestructura transversal
│       │       │   ├── common/           # Utilidades (DateUtils, ResultState, Uuids, Constants)
│       │       │   ├── di/               # AppModule: inyección de dependencias MANUAL
│       │       │   ├── navigation/       # AppRoutes + AppNavGraph (Navigation Compose)
│       │       │   ├── network/          # RetrofitClient, SupabaseConfig, NetworkMonitor
│       │       │   ├── notifications/    # SyncNotifications (notificaciones del sistema)
│       │       │   ├── permissions/      # Manejo de permiso de cámara
│       │       │   ├── preferences/      # AuthPreferences, SyncPreferences (DataStore)
│       │       │   └── sync/             # SyncScheduler + SyncWorker (WorkManager)
│       │       │
│       │       ├── domain/               # Capa de dominio (reglas de negocio puras)
│       │       │   ├── model/            # Modelos de negocio (Student, Grade, Incident, sync/, telegram/)
│       │       │   ├── repository/       # Interfaces de repositorio (contratos)
│       │       │   ├── service/          # Servicios de dominio
│       │       │   └── usecase/          # Casos de uso por feature
│       │       │       ├── attendance/   #   (asistencia)
│       │       │       ├── auth/         #   (login)
│       │       │       ├── grade/        #   (calificaciones)
│       │       │       ├── grades/
│       │       │       ├── incidents/    #   (incidentes)
│       │       │       ├── ocr/          #   (lectura OCR de hojas)
│       │       │       ├── student/      #   (estudiantes)
│       │       │       └── telegram/     #   (envío de mensajes)
│       │       │
│       │       ├── data/                 # Capa de datos (implementaciones)
│       │       │   ├── local/            # Room: AppDatabase, DatabaseSeeder
│       │       │   │   ├── dao/          #   DAOs (Student, Attendance, Grade, Incident, Catalog)
│       │       │   │   ├── entity/       #   Entidades Room
│       │       │   │   └── migrations/   #   Migraciones de esquema (v5→v10)
│       │       │   ├── mapper/           # Mappers DTO ↔ Entity ↔ Domain
│       │       │   ├── remote/           # Acceso remoto
│       │       │   │   ├── api/          #   SupabaseApiService, TelegramApiService (Retrofit)
│       │       │   │   └── dto/          #   DTOs de request/response (Supabase y Telegram)
│       │       │   └── repository/       # Implementaciones de los repositorios del dominio
│       │       │
│       │       ├── presentation/         # Capa de presentación (UI + ViewModels, MVVM)
│       │       │   ├── attendance/       #   Pantalla + ViewModel + UiState/Event de asistencia
│       │       │   ├── grades/           #   Calificaciones (lista, detalle, componentes)
│       │       │   ├── incidents/        #   Incidentes
│       │       │   ├── login/            #   Inicio de sesión
│       │       │   ├── home/             #   Pantalla principal
│       │       │   ├── components/       #   Componentes Compose reutilizables
│       │       │   └── common/           #   UI común (OfflineBanner, selectores, badges de sync)
│       │       │
│       │       ├── services/             # Integraciones de servicios
│       │       │   ├── mlkit/            #   TextRecognitionProcessor (OCR)
│       │       │   └── telegram/         #   TelegramMessageBuilder, TelegramConfig
│       │       │
│       │       ├── states/               # Estados de UI compartidos
│       │       └── ui/theme/             # Tema Compose (Color, Theme, Type)
│       │
│       ├── test/                # Pruebas unitarias (JVM)
│       └── androidTest/         # Pruebas instrumentadas (Espresso / Compose UI test)
│
├── gradle/
│   ├── libs.versions.toml       # Catálogo de versiones (fuente única de versiones)
│   └── wrapper/                 # Gradle Wrapper 8.9
├── build.gradle.kts             # Build de nivel raíz
├── settings.gradle.kts          # Nombre del proyecto y módulos
├── gradle.properties            # Flags de Gradle/AndroidX
├── LICENSE                      # Licencia MIT
└── README.md                    # Este archivo
```

---

## 9. Dependencias de terceros y licencias

Todas las versiones provienen del catálogo real
[`gradle/libs.versions.toml`](gradle/libs.versions.toml). Los identificadores de licencia
siguen el estándar [SPDX](https://spdx.org/licenses/).

| Librería / Framework | Coordenadas Maven | Versión | Licencia (SPDX) |
|---|---|---|---|
| Jetpack Compose (BOM) | `androidx.compose:compose-bom` | `2024.12.01` | `Apache-2.0` |
| Compose UI / Graphics / Tooling | `androidx.compose.ui:ui`, `ui-graphics`, `ui-tooling`, `ui-tooling-preview` | *(BOM)* | `Apache-2.0` |
| Material 3 | `androidx.compose.material3:material3` | *(BOM)* | `Apache-2.0` |
| Material Icons Extended | `androidx.compose.material:material-icons-extended` | `1.7.6` | `Apache-2.0` |
| Activity Compose | `androidx.activity:activity-compose` | `1.9.3` | `Apache-2.0` |
| Navigation Compose | `androidx.navigation:navigation-compose`, `navigation-runtime-ktx` | `2.8.5` | `Apache-2.0` |
| Lifecycle Runtime KTX | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | `Apache-2.0` |
| Lifecycle ViewModel Compose *(en el catálogo, no enlazada en el módulo `app`)* | `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | `Apache-2.0` |
| Core KTX | `androidx.core:core-ktx` | `1.15.0` | `Apache-2.0` |
| Room (runtime / ktx / compiler) | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` | `2.6.1` | `Apache-2.0` |
| DataStore Preferences | `androidx.datastore:datastore-preferences` | `1.1.1` | `Apache-2.0` |
| WorkManager | `androidx.work:work-runtime-ktx` | `2.9.1` | `Apache-2.0` |
| Retrofit (core) | `com.squareup.retrofit2:retrofit` | `2.11.0` | `Apache-2.0` |
| Retrofit Gson Converter | `com.squareup.retrofit2:converter-gson` | `2.11.0` | `Apache-2.0` |
| OkHttp (core) | `com.squareup.okhttp3:okhttp` | `4.12.0` | `Apache-2.0` |
| OkHttp Logging Interceptor | `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | `Apache-2.0` |
| Gson | `com.google.code.gson:gson` | `2.11.0` | `Apache-2.0` |
| Kotlin Coroutines (Android) | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | `Apache-2.0` |
| kotlinx.serialization (JSON) | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.7.3` | `Apache-2.0` |
| ML Kit Text Recognition | `com.google.mlkit:text-recognition` | `16.0.1` | Propietaria — [Google ML Kit Terms of Service](https://developers.google.com/ml-kit/terms) *(sin identificador SPDX)* |
| CameraX *(declarada en el catálogo, no enlazada en el módulo `app`)* | `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` | `1.4.1` | `Apache-2.0` |
| JUnit 4 | `junit:junit` | `4.13.2` | `EPL-1.0` |
| AndroidX Test Ext JUnit | `androidx.test.ext:junit` | `1.2.1` | `Apache-2.0` |
| Espresso Core | `androidx.test.espresso:espresso-core` | `3.6.1` | `Apache-2.0` |
| Compose UI Test | `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` | *(BOM)* | `Apache-2.0` |
| Android Gradle Plugin | `com.android.application` | `8.7.3` | `Apache-2.0` |
| Kotlin (Android / Compose / Serialization) | `org.jetbrains.kotlin.*` | `2.0.21` | `Apache-2.0` |
| KSP | `com.google.devtools.ksp` | `2.0.21-1.0.27` | `Apache-2.0` |
| Gradle (Wrapper) | — | `8.9` | `Apache-2.0` |

### Servicios externos consumidos vía API REST (no son dependencias Gradle)

| Servicio | Cómo se integra | Notas |
|---|---|---|
| **Supabase** (PostgREST) | Retrofit + OkHttp contra el endpoint REST del proyecto | No usa el SDK oficial de Supabase. |
| **Telegram Bot API** | Retrofit + OkHttp contra `https://api.telegram.org/bot<token>/` | No usa un SDK de Telegram. |

---

## 10. Autoría y transparencia

### Código propio del equipo

La **lógica de negocio** y la **arquitectura** son trabajo del equipo de AlegriApp:

- **Capa `domain`**: modelos de negocio, contratos de repositorio y todos los casos de uso
  (asistencia, calificaciones, incidentes, OCR, autenticación, Telegram).
- **Capa `data`**: implementaciones de repositorios, entidades y DAOs de Room, migraciones de
  esquema, mappers DTO↔Entity↔Domain, y los servicios Retrofit hacia Supabase y Telegram.
- **Capa `presentation`**: pantallas en Jetpack Compose, ViewModels, estados (`UiState`) y
  eventos de cada módulo siguiendo el patrón MVVM.
- **Capa `core`**: inyección de dependencias manual, navegación, configuración de red,
  monitoreo de conectividad, sincronización offline-first (WorkManager) y notificaciones.
- **Integraciones**: diseño del esquema de sincronización offline-first, la lógica de envío
  de reportes por Telegram y la lectura OCR de hojas de asistencia con ML Kit.

### Código de terceros

Las librerías y frameworks listados en la [sección 9](#9-dependencias-de-terceros-y-licencias)
(Jetpack Compose, Room, Retrofit, OkHttp, Gson, WorkManager, DataStore, ML Kit, Kotlin
Coroutines, etc.) son **software de terceros** bajo sus respectivas licencias. Supabase y
Telegram son **servicios externos** consumidos vía API REST.

### Uso de herramientas de IA

Durante el desarrollo se emplearon herramientas de asistencia con inteligencia artificial
—**Claude**, **ChatGPT** y **Cursor**— para la **generación de código base**, la
**depuración** y la **documentación**. Las **decisiones de diseño**, la **arquitectura** y la
**integración** de los componentes son responsabilidad del equipo de desarrollo, que revisó y
validó el código resultante.

---

## 11. Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. El texto completo está disponible en el
archivo [`LICENSE`](LICENSE) en la raíz del repositorio.

---

## 12. Equipo de desarrollo

- **Anahí Berrú**
- **Jorge Jara**
- **Ignacio Masaquiza**
- **Danny Yánez**

---

<p align="center"><sub>Hecho con ❤️ para Fe y Alegría.</sub></p>
