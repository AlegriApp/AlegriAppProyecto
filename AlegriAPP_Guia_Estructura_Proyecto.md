# AlegriAPP — Guía de estructura del proyecto Android

> Proyecto móvil educativo para centralizar asistencia, calificaciones, incidentes y comunicación institucional con Telegram, persistencia local y OCR con Google ML Kit.

---

## 1. Contexto funcional del proyecto

AlegriAPP nace como una aplicación móvil para instituciones educativas de Fe y Alegría. Su objetivo principal es reducir el trabajo manual del docente y mejorar la comunicación entre docentes, autoridades y representantes.

La app debe cubrir tres flujos principales:

1. **Asistencia**
   - Registrar estudiantes como presente, tardanza o ausente.
   - Guardar información aunque no haya internet.
   - Enviar reporte a inspección o autoridades.
   - Permitir digitalización futura de hojas físicas con OCR.

2. **Calificaciones**
   - Registrar notas por materia y período.
   - Calcular promedios.
   - Generar boletín académico.
   - Enviar resumen por Telegram.

3. **Incidentes**
   - Registrar novedades disciplinarias, académicas o de salud.
   - Seleccionar estudiante, tipo de incidente y descripción.
   - Enviar notificación mediante Telegram.
   - Mantener historial local.

Tecnologías base:

- Kotlin
- Jetpack Compose
- MVVM
- Room Database
- Retrofit / API REST
- Telegram Bot API
- Google ML Kit Text Recognition
- Material Design 3
- Navegación con Navigation Compose
- Manejo de estado con sealed classes, StateFlow y ViewModel

---

## 2. Estructura inicial observada

Actualmente el proyecto tiene una estructura base generada por Android Studio:

```text
app/
├── manifests/
│   └── AndroidManifest.xml
├── kotlin+java/
│   └── com.example.myapplication/
│       ├── states/
│       ├── ui.theme/
│       └── MainActivity.kt
├── res/
│   ├── drawable/
│   ├── mipmap/
│   ├── values/
│   └── xml/
└── Gradle Scripts/
    ├── build.gradle.kts (Project)
    ├── build.gradle.kts (Module :app)
    ├── proguard-rules.pro
    ├── gradle.properties
    ├── gradle-wrapper.properties
    ├── libs.versions.toml
    ├── local.properties
    └── settings.gradle.kts
```

Esta estructura sirve para iniciar, pero para AlegriAPP es mejor separarla por capas y responsabilidades.

---

## 3. Estructura recomendada final

La estructura recomendada usa arquitectura limpia ligera basada en **UI / Domain / Data**, manteniendo MVVM para pantallas.

```text
app/src/main/java/com/example/myapplication/
├── MainActivity.kt
├── AlegriApp.kt
│
├── core/
│   ├── common/
│   │   ├── Constants.kt
│   │   ├── ResultState.kt
│   │   └── DateUtils.kt
│   ├── navigation/
│   │   ├── AppRoutes.kt
│   │   └── AppNavGraph.kt
│   ├── network/
│   │   ├── RetrofitClient.kt
│   │   └── NetworkMonitor.kt
│   └── permissions/
│       └── CameraPermissionHandler.kt
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── StudentDao.kt
│   │   │   ├── AttendanceDao.kt
│   │   │   ├── GradeDao.kt
│   │   │   └── IncidentDao.kt
│   │   └── entity/
│   │       ├── StudentEntity.kt
│   │       ├── AttendanceEntity.kt
│   │       ├── GradeEntity.kt
│   │       └── IncidentEntity.kt
│   │
│   ├── remote/
│   │   ├── api/
│   │   │   ├── AlegriApiService.kt
│   │   │   └── TelegramApiService.kt
│   │   └── dto/
│   │       ├── TelegramMessageRequest.kt
│   │       ├── AttendanceSyncRequest.kt
│   │       ├── GradeSyncRequest.kt
│   │       └── IncidentSyncRequest.kt
│   │
│   ├── mapper/
│   │   ├── StudentMapper.kt
│   │   ├── AttendanceMapper.kt
│   │   ├── GradeMapper.kt
│   │   └── IncidentMapper.kt
│   │
│   └── repository/
│       ├── StudentRepositoryImpl.kt
│       ├── AttendanceRepositoryImpl.kt
│       ├── GradeRepositoryImpl.kt
│       ├── IncidentRepositoryImpl.kt
│       ├── TelegramRepositoryImpl.kt
│       └── OcrRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   ├── Student.kt
│   │   ├── Attendance.kt
│   │   ├── AttendanceStatus.kt
│   │   ├── Grade.kt
│   │   ├── Incident.kt
│   │   └── IncidentType.kt
│   │
│   ├── repository/
│   │   ├── StudentRepository.kt
│   │   ├── AttendanceRepository.kt
│   │   ├── GradeRepository.kt
│   │   ├── IncidentRepository.kt
│   │   ├── TelegramRepository.kt
│   │   └── OcrRepository.kt
│   │
│   └── usecase/
│       ├── attendance/
│       │   ├── GetStudentsUseCase.kt
│       │   ├── SaveAttendanceUseCase.kt
│       │   └── SendAttendanceReportUseCase.kt
│       ├── grades/
│       │   ├── SaveGradesUseCase.kt
│       │   └── SendGradeReportUseCase.kt
│       ├── incidents/
│       │   ├── SaveIncidentUseCase.kt
│       │   └── SendIncidentReportUseCase.kt
│       └── ocr/
│           └── ReadAttendanceSheetUseCase.kt
│
├── presentation/
│   ├── components/
│   │   ├── AppScaffold.kt
│   │   ├── StudentCard.kt
│   │   ├── PrimaryActionButton.kt
│   │   ├── LoadingContent.kt
│   │   └── ErrorContent.kt
│   │
│   ├── attendance/
│   │   ├── AttendanceScreen.kt
│   │   ├── AttendanceViewModel.kt
│   │   ├── AttendanceUiState.kt
│   │   └── AttendanceEvent.kt
│   │
│   ├── grades/
│   │   ├── GradesScreen.kt
│   │   ├── GradesViewModel.kt
│   │   ├── GradesUiState.kt
│   │   └── GradesEvent.kt
│   │
│   ├── incidents/
│   │   ├── IncidentScreen.kt
│   │   ├── IncidentViewModel.kt
│   │   ├── IncidentUiState.kt
│   │   └── IncidentEvent.kt
│   │
│   ├── ocr/
│   │   ├── OcrAttendanceScreen.kt
│   │   ├── OcrViewModel.kt
│   │   └── OcrUiState.kt
│   │
│   └── home/
│       └── HomeScreen.kt
│
├── services/
│   ├── telegram/
│   │   ├── TelegramMessageBuilder.kt
│   │   └── TelegramConfig.kt
│   └── mlkit/
│       └── TextRecognitionProcessor.kt
│
└── ui/
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 4. Archivos principales y contenido recomendado

### 4.1 `MainActivity.kt`

Responsabilidad: punto de entrada de la app. Solo debe cargar el tema y la aplicación principal.

```kotlin
package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.ui.theme.AlegriAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AlegriAppTheme {
                AlegriApp()
            }
        }
    }
}
```

---

### 4.2 `AlegriApp.kt`

Responsabilidad: inicializar navegación general.

```kotlin
package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.core.navigation.AppNavGraph

@Composable
fun AlegriApp() {
    val navController = rememberNavController()
    AppNavGraph(navController = navController)
}
```

---

### 4.3 `core/navigation/AppRoutes.kt`

Responsabilidad: definir rutas de navegación de forma centralizada.

```kotlin
package com.example.myapplication.core.navigation

sealed class AppRoutes(val route: String) {
    data object Home : AppRoutes("home")
    data object Attendance : AppRoutes("attendance")
    data object Grades : AppRoutes("grades")
    data object Incidents : AppRoutes("incidents")
    data object OcrAttendance : AppRoutes("ocr_attendance")
}
```

---

### 4.4 `core/navigation/AppNavGraph.kt`

Responsabilidad: conectar pantallas usando Navigation Compose.

```kotlin
package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.presentation.attendance.AttendanceScreen
import com.example.myapplication.presentation.grades.GradesScreen
import com.example.myapplication.presentation.home.HomeScreen
import com.example.myapplication.presentation.incidents.IncidentScreen
import com.example.myapplication.presentation.ocr.OcrAttendanceScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home.route
    ) {
        composable(AppRoutes.Home.route) {
            HomeScreen(
                onAttendanceClick = { navController.navigate(AppRoutes.Attendance.route) },
                onGradesClick = { navController.navigate(AppRoutes.Grades.route) },
                onIncidentsClick = { navController.navigate(AppRoutes.Incidents.route) },
                onOcrClick = { navController.navigate(AppRoutes.OcrAttendance.route) }
            )
        }

        composable(AppRoutes.Attendance.route) {
            AttendanceScreen(onBack = { navController.popBackStack() })
        }

        composable(AppRoutes.Grades.route) {
            GradesScreen(onBack = { navController.popBackStack() })
        }

        composable(AppRoutes.Incidents.route) {
            IncidentScreen(onBack = { navController.popBackStack() })
        }

        composable(AppRoutes.OcrAttendance.route) {
            OcrAttendanceScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

---

## 5. Capa Domain

La capa `domain` contiene reglas del negocio. No depende de Android, Room, Retrofit ni Compose.

---

### 5.1 `domain/model/Student.kt`

```kotlin
package com.example.myapplication.domain.model

data class Student(
    val id: Long,
    val fullName: String,
    val grade: String,
    val section: String,
    val representativeName: String,
    val telegramChatId: String?
)
```

---

### 5.2 `domain/model/AttendanceStatus.kt`

```kotlin
package com.example.myapplication.domain.model

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    UNMARKED
}
```

---

### 5.3 `domain/model/Attendance.kt`

```kotlin
package com.example.myapplication.domain.model

data class Attendance(
    val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: AttendanceStatus,
    val synced: Boolean = false
)
```

---

### 5.4 `domain/model/Grade.kt`

```kotlin
package com.example.myapplication.domain.model

data class Grade(
    val id: Long = 0,
    val studentId: Long,
    val subject: String,
    val period: String,
    val score: Double,
    val synced: Boolean = false
)
```

---

### 5.5 `domain/model/IncidentType.kt`

```kotlin
package com.example.myapplication.domain.model

enum class IncidentType {
    BEHAVIOR,
    ACADEMIC,
    HEALTH,
    OTHER
}
```

---

### 5.6 `domain/model/Incident.kt`

```kotlin
package com.example.myapplication.domain.model

data class Incident(
    val id: Long = 0,
    val studentId: Long,
    val type: IncidentType,
    val description: String,
    val severity: String,
    val date: String,
    val synced: Boolean = false
)
```

---

## 6. Estados globales

### 6.1 `core/common/ResultState.kt`

Se usa para representar carga, éxito y error en repositorios o casos de uso.

```kotlin
package com.example.myapplication.core.common

sealed class ResultState<out T> {
    data object Loading : ResultState<Nothing>()
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
}
```

---

### 6.2 Estado para acciones de envío

Se recomienda usarlo en envíos por Telegram o sincronización.

```kotlin
package com.example.myapplication.core.common

sealed class SendState {
    data object Idle : SendState()
    data object Sending : SendState()
    data class Success(val message: String) : SendState()
    data class Error(val message: String) : SendState()
}
```

---

## 7. Capa Data: Room

### 7.1 `data/local/entity/StudentEntity.kt`

```kotlin
package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Long,
    val fullName: String,
    val grade: String,
    val section: String,
    val representativeName: String,
    val telegramChatId: String?
)
```

---

### 7.2 `data/local/entity/AttendanceEntity.kt`

```kotlin
package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val status: String,
    val synced: Boolean
)
```

---

### 7.3 `data/local/entity/GradeEntity.kt`

```kotlin
package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val subject: String,
    val period: String,
    val score: Double,
    val synced: Boolean
)
```

---

### 7.4 `data/local/entity/IncidentEntity.kt`

```kotlin
package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val type: String,
    val description: String,
    val severity: String,
    val date: String,
    val synced: Boolean
)
```

---

### 7.5 `data/local/dao/StudentDao.kt`

```kotlin
package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY fullName")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)
}
```

---

### 7.6 `data/local/dao/AttendanceDao.kt`

```kotlin
package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun observeAttendanceByDate(date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttendance(records: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance WHERE synced = 0")
    suspend fun getPendingSync(): List<AttendanceEntity>
}
```

---

### 7.7 `data/local/AppDatabase.kt`

```kotlin
package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.data.local.entity.StudentEntity

@Database(
    entities = [
        StudentEntity::class,
        AttendanceEntity::class,
        GradeEntity::class,
        IncidentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun gradeDao(): GradeDao
    abstract fun incidentDao(): IncidentDao
}
```

---

## 8. API REST y Telegram

### 8.1 `data/remote/api/AlegriApiService.kt`

Responsabilidad: sincronizar asistencia, notas e incidentes con backend institucional.

```kotlin
package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.AttendanceSyncRequest
import com.example.myapplication.data.remote.dto.GradeSyncRequest
import com.example.myapplication.data.remote.dto.IncidentSyncRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AlegriApiService {

    @POST("attendance/sync")
    suspend fun syncAttendance(
        @Body request: AttendanceSyncRequest
    )

    @POST("grades/sync")
    suspend fun syncGrades(
        @Body request: GradeSyncRequest
    )

    @POST("incidents/sync")
    suspend fun syncIncidents(
        @Body request: IncidentSyncRequest
    )
}
```

---

### 8.2 `data/remote/api/TelegramApiService.kt`

Responsabilidad: consumir Telegram Bot API.

```kotlin
package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TelegramApiService {

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: TelegramMessageRequest
    )
}
```

---

### 8.3 `data/remote/dto/TelegramMessageRequest.kt`

```kotlin
package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TelegramMessageRequest(
    @SerializedName("chat_id") val chatId: String,
    val text: String,
    @SerializedName("parse_mode") val parseMode: String = "Markdown"
)
```

---

### 8.4 `services/telegram/TelegramConfig.kt`

No se recomienda hardcodear el token en código fuente. Para práctica académica puede usarse `BuildConfig`, pero lo ideal es backend o variables seguras.

```kotlin
package com.example.myapplication.services.telegram

object TelegramConfig {
    const val TELEGRAM_BASE_URL = "https://api.telegram.org/"
}
```

---

### 8.5 `services/telegram/TelegramMessageBuilder.kt`

Responsabilidad: construir mensajes claros para asistencia, notas e incidentes.

```kotlin
package com.example.myapplication.services.telegram

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.Student

object TelegramMessageBuilder {

    fun buildAttendanceReport(
        date: String,
        records: List<Pair<Student, Attendance>>
    ): String {
        val present = records.count { it.second.status.name == "PRESENT" }
        val late = records.count { it.second.status.name == "LATE" }
        val absent = records.count { it.second.status.name == "ABSENT" }

        return buildString {
            appendLine("*AlegriAPP - Reporte de Asistencia*")
            appendLine("Fecha: $date")
            appendLine("Presentes: $present")
            appendLine("Tardanzas: $late")
            appendLine("Ausentes: $absent")
            appendLine()
            appendLine("*Detalle:*")
            records.forEach { (student, attendance) ->
                appendLine("- ${student.fullName}: ${attendance.status}")
            }
        }
    }

    fun buildGradeReport(
        subject: String,
        period: String,
        grades: List<Pair<Student, Grade>>
    ): String {
        val average = grades.map { it.second.score }.average()

        return buildString {
            appendLine("*AlegriAPP - Boletín Académico*")
            appendLine("Materia: $subject")
            appendLine("Período: $period")
            appendLine("Promedio general: ${"%.2f".format(average)}")
            appendLine()
            grades.forEach { (student, grade) ->
                appendLine("- ${student.fullName}: ${grade.score}")
            }
        }
    }

    fun buildIncidentReport(
        student: Student,
        incident: Incident
    ): String {
        return buildString {
            appendLine("*AlegriAPP - Reporte de Incidente*")
            appendLine("Estudiante: ${student.fullName}")
            appendLine("Tipo: ${incident.type}")
            appendLine("Severidad: ${incident.severity}")
            appendLine("Fecha: ${incident.date}")
            appendLine()
            appendLine("*Descripción:*")
            appendLine(incident.description)
        }
    }
}
```

---

## 9. Repositorios

### 9.1 `domain/repository/TelegramRepository.kt`

```kotlin
package com.example.myapplication.domain.repository

interface TelegramRepository {
    suspend fun sendMessage(chatId: String, message: String): Boolean
}
```

---

### 9.2 `data/repository/TelegramRepositoryImpl.kt`

```kotlin
package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.TelegramApiService
import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import com.example.myapplication.domain.repository.TelegramRepository

class TelegramRepositoryImpl(
    private val api: TelegramApiService,
    private val botToken: String
) : TelegramRepository {

    override suspend fun sendMessage(chatId: String, message: String): Boolean {
        return try {
            api.sendMessage(
                token = botToken,
                request = TelegramMessageRequest(
                    chatId = chatId,
                    text = message
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

---

## 10. Google ML Kit OCR

### 10.1 Dependencias necesarias

En `build.gradle.kts (Module :app)`:

```kotlin
dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
}
```

---

### 10.2 Permiso de cámara en `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Dentro de `<application>` se mantiene la `MainActivity`.

---

### 10.3 `services/mlkit/TextRecognitionProcessor.kt`

Responsabilidad: recibir una imagen y devolver texto detectado.

```kotlin
package com.example.myapplication.services.mlkit

import android.net.Uri
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognitionProcessor(
    private val context: Context
) {

    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    suspend fun processImage(uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.text
    }
}
```

---

### 10.4 `domain/repository/OcrRepository.kt`

```kotlin
package com.example.myapplication.domain.repository

import android.net.Uri

interface OcrRepository {
    suspend fun readTextFromImage(uri: Uri): String
}
```

---

### 10.5 `data/repository/OcrRepositoryImpl.kt`

```kotlin
package com.example.myapplication.data.repository

import android.net.Uri
import com.example.myapplication.domain.repository.OcrRepository
import com.example.myapplication.services.mlkit.TextRecognitionProcessor

class OcrRepositoryImpl(
    private val processor: TextRecognitionProcessor
) : OcrRepository {

    override suspend fun readTextFromImage(uri: Uri): String {
        return processor.processImage(uri)
    }
}
```

---

## 11. Pantallas y ViewModels

---

## 11.1 Home

### `presentation/home/HomeScreen.kt`

```kotlin
package com.example.myapplication.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onAttendanceClick: () -> Unit,
    onGradesClick: () -> Unit,
    onIncidentsClick: () -> Unit,
    onOcrClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AlegriAPP",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Sistema de Comunicación Docente - Fe y Alegría",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(onClick = onAttendanceClick) {
            Text("Asistencia")
        }

        Button(onClick = onGradesClick) {
            Text("Calificaciones")
        }

        Button(onClick = onIncidentsClick) {
            Text("Incidentes")
        }

        Button(onClick = onOcrClick) {
            Text("Digitalizar asistencia")
        }
    }
}
```

---

## 11.2 Asistencia

### `presentation/attendance/AttendanceUiState.kt`

```kotlin
package com.example.myapplication.presentation.attendance

import com.example.myapplication.core.common.SendState
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.Student

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val students: List<Student> = emptyList(),
    val selectedStatus: Map<Long, AttendanceStatus> = emptyMap(),
    val sendState: SendState = SendState.Idle,
    val errorMessage: String? = null
)
```

---

### `presentation/attendance/AttendanceEvent.kt`

```kotlin
package com.example.myapplication.presentation.attendance

import com.example.myapplication.domain.model.AttendanceStatus

sealed class AttendanceEvent {
    data class MarkStudent(
        val studentId: Long,
        val status: AttendanceStatus
    ) : AttendanceEvent()

    data object SaveAttendance : AttendanceEvent()
    data object SendReport : AttendanceEvent()
}
```

---

### `presentation/attendance/AttendanceViewModel.kt`

```kotlin
package com.example.myapplication.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.SendState
import com.example.myapplication.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun onEvent(event: AttendanceEvent) {
        when (event) {
            is AttendanceEvent.MarkStudent -> markStudent(event.studentId, event.status)
            AttendanceEvent.SaveAttendance -> saveAttendance()
            AttendanceEvent.SendReport -> sendReport()
        }
    }

    private fun markStudent(studentId: Long, status: AttendanceStatus) {
        val updated = _uiState.value.selectedStatus.toMutableMap()
        updated[studentId] = status
        _uiState.value = _uiState.value.copy(selectedStatus = updated)
    }

    private fun saveAttendance() {
        viewModelScope.launch {
            // Guardar en Room mediante UseCase
        }
    }

    private fun sendReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sendState = SendState.Sending)
            // Enviar mensaje por Telegram mediante UseCase
            _uiState.value = _uiState.value.copy(
                sendState = SendState.Success("Reporte enviado correctamente")
            )
        }
    }
}
```

---

### `presentation/attendance/AttendanceScreen.kt`

```kotlin
package com.example.myapplication.presentation.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.AttendanceStatus

@Composable
fun AttendanceScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Toma de Asistencia")

        // En implementación real este estado viene del ViewModel.
        val demoStudents = listOf("María González", "Juan Pérez", "Ana Rodríguez")

        demoStudents.forEachIndexed { index, student ->
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = student)
                    Text(text = "5to Grado Sección A")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { }) {
                            Text("Presente")
                        }
                        Button(onClick = { }) {
                            Text("Tardanza")
                        }
                        Button(onClick = { }) {
                            Text("Ausente")
                        }
                    }
                }
            }
        }

        Button(onClick = { }) {
            Text("Enviar Reporte")
        }
    }
}
```

---

## 11.3 Calificaciones

### `presentation/grades/GradesUiState.kt`

```kotlin
package com.example.myapplication.presentation.grades

import com.example.myapplication.core.common.SendState
import com.example.myapplication.domain.model.Student

data class GradesUiState(
    val students: List<Student> = emptyList(),
    val selectedSubject: String = "Matemáticas",
    val selectedPeriod: String = "1er Lapso",
    val grades: Map<Long, Double> = emptyMap(),
    val average: Double = 0.0,
    val sendState: SendState = SendState.Idle
)
```

---

### `presentation/grades/GradesEvent.kt`

```kotlin
package com.example.myapplication.presentation.grades

sealed class GradesEvent {
    data class ChangeSubject(val subject: String) : GradesEvent()
    data class ChangePeriod(val period: String) : GradesEvent()
    data class UpdateGrade(val studentId: Long, val score: Double) : GradesEvent()
    data object SaveGrades : GradesEvent()
    data object SendGradeReport : GradesEvent()
}
```

---

### `presentation/grades/GradesScreen.kt`

```kotlin
package com.example.myapplication.presentation.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GradesScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registro de Calificaciones")
        Text("Materia: Matemáticas")
        Text("Período: 1er Lapso")

        listOf("María González", "Juan Pérez", "Ana Rodríguez").forEach { student ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(student, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    placeholder = { Text("0-20") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Promedio de la Sección: 0 / 20")

        Button(onClick = { }) {
            Text("Enviar Boletín")
        }
    }
}
```

---

## 11.4 Incidentes

### `presentation/incidents/IncidentUiState.kt`

```kotlin
package com.example.myapplication.presentation.incidents

import com.example.myapplication.core.common.SendState
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.Student

data class IncidentUiState(
    val students: List<Student> = emptyList(),
    val selectedStudentId: Long? = null,
    val selectedType: IncidentType = IncidentType.BEHAVIOR,
    val description: String = "",
    val severity: String = "Medio",
    val sendState: SendState = SendState.Idle
)
```

---

### `presentation/incidents/IncidentEvent.kt`

```kotlin
package com.example.myapplication.presentation.incidents

import com.example.myapplication.domain.model.IncidentType

sealed class IncidentEvent {
    data class SelectStudent(val studentId: Long) : IncidentEvent()
    data class SelectType(val type: IncidentType) : IncidentEvent()
    data class ChangeDescription(val description: String) : IncidentEvent()
    data class ChangeSeverity(val severity: String) : IncidentEvent()
    data object SaveIncident : IncidentEvent()
    data object SendIncident : IncidentEvent()
}
```

---

### `presentation/incidents/IncidentScreen.kt`

```kotlin
package com.example.myapplication.presentation.incidents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IncidentScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reporte de Incidentes")
        Text("Enviar directo a autoridades y representantes vía Telegram")

        Text("Estudiante involucrado")
        Button(onClick = { }) {
            Text("Seleccionar estudiante")
        }

        Text("Tipo de incidente")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = { }, label = { Text("Comportamiento") })
            FilterChip(selected = false, onClick = { }, label = { Text("Académico") })
        }

        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("Descripción del incidente") },
            minLines = 4
        )

        Button(onClick = { }) {
            Text("Enviar Reporte")
        }
    }
}
```

---

## 11.5 OCR de asistencia

### `presentation/ocr/OcrUiState.kt`

```kotlin
package com.example.myapplication.presentation.ocr

data class OcrUiState(
    val isProcessing: Boolean = false,
    val detectedText: String = "",
    val errorMessage: String? = null
)
```

---

### `presentation/ocr/OcrViewModel.kt`

```kotlin
package com.example.myapplication.presentation.ocr

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.repository.OcrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OcrViewModel(
    private val repository: OcrRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrUiState())
    val uiState: StateFlow<OcrUiState> = _uiState

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            try {
                val text = repository.readTextFromImage(uri)
                _uiState.value = OcrUiState(
                    isProcessing = false,
                    detectedText = text
                )
            } catch (e: Exception) {
                _uiState.value = OcrUiState(
                    isProcessing = false,
                    errorMessage = "No se pudo procesar la imagen"
                )
            }
        }
    }
}
```

---

### `presentation/ocr/OcrAttendanceScreen.kt`

```kotlin
package com.example.myapplication.presentation.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OcrAttendanceScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Digitalizar hoja de asistencia")
        Text("Toma una foto de la hoja física para convertirla en texto.")

        Button(onClick = { }) {
            Text("Tomar foto")
        }

        Button(onClick = { }) {
            Text("Seleccionar imagen")
        }

        Text("Texto detectado aparecerá aquí.")
    }
}
```

---

## 12. Tema visual con Material Design 3

### `ui/theme/Theme.kt`

```kotlin
package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

@Composable
fun AlegriAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## 13. Recursos XML recomendados

### `res/values/strings.xml`

```xml
<resources>
    <string name="app_name">AlegriAPP</string>
    <string name="attendance">Asistencia</string>
    <string name="grades">Calificaciones</string>
    <string name="incidents">Incidentes</string>
    <string name="ocr_attendance">Digitalizar asistencia</string>
    <string name="send_report">Enviar reporte</string>
    <string name="send_grade_report">Enviar boletín</string>
</resources>
```

---

### `res/values/colors.xml`

Aunque Compose usa `MaterialTheme`, se puede dejar este archivo para compatibilidad.

```xml
<resources>
    <color name="primary">#0D47A1</color>
    <color name="secondary">#1976D2</color>
    <color name="background">#FFFFFF</color>
</resources>
```

---

## 14. Gradle recomendado

### `build.gradle.kts (Module :app)`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "TELEGRAM_BOT_TOKEN",
            "\"COLOCAR_TOKEN_SOLO_EN_ENTORNO_SEGURO\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ML Kit OCR
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
}
```

---

## 15. Buenas prácticas para AlegriAPP

### 15.1 Separación de responsabilidades

- `presentation`: pantallas, componentes y ViewModels.
- `domain`: modelos, contratos y casos de uso.
- `data`: Room, Retrofit, DTOs, mappers y repositorios concretos.
- `services`: integraciones concretas como Telegram y ML Kit.
- `core`: navegación, constantes, estados comunes, utilidades y permisos.

### 15.2 Estado

- Usar `StateFlow` en ViewModels.
- Usar `collectAsStateWithLifecycle()` en Compose.
- Usar `sealed class` para estados finitos.
- Evitar lógica de negocio dentro de Composables.

### 15.3 Persistencia

- Room debe ser la fuente local.
- Todo registro creado sin internet debe guardarse con `synced = false`.
- Al recuperar internet, sincronizar registros pendientes.
- No perder asistencia o incidentes por cerrar la app.

### 15.4 Telegram

- No guardar el token real en GitHub.
- Para producción, enviar mensajes desde backend y no directamente desde la app.
- Para PMV académico, se puede usar `BuildConfig` o archivo local no versionado.
- El mensaje debe ser claro, corto y trazable.

### 15.5 Google ML Kit

- Solicitar permiso de cámara en tiempo de ejecución.
- Validar que la imagen sea legible antes de procesarla.
- Mostrar texto detectado para que el docente confirme antes de guardar.
- No guardar automáticamente sin revisión humana.

### 15.6 UI / UX

- Usar Material Design 3.
- Botones grandes y claros para docentes.
- Evitar pantallas recargadas.
- Mostrar estados de carga, éxito y error.
- Usar textos desde `strings.xml` para facilitar escalabilidad.

---

## 16. Orden de implementación sugerido

### Sprint 1 — Base
1. Crear estructura de paquetes.
2. Configurar navegación.
3. Configurar tema Material 3.
4. Crear modelos del dominio.

### Sprint 2 — Interfaces
1. Pantalla Home.
2. Pantalla Asistencia.
3. Pantalla Calificaciones.
4. Pantalla Incidentes.
5. Estados UI por pantalla.

### Sprint 3 — Persistencia
1. Agregar Room.
2. Crear entities.
3. Crear DAOs.
4. Crear database.
5. Crear repositorios.

### Sprint 4 — Telegram
1. Crear `TelegramApiService`.
2. Crear `TelegramMessageBuilder`.
3. Enviar reporte de incidentes.
4. Enviar asistencia.
5. Enviar boletín.

### Sprint 5 — ML Kit
1. Pedir permiso de cámara.
2. Seleccionar o capturar imagen.
3. Procesar OCR.
4. Mostrar texto detectado.
5. Convertir texto a registros de asistencia.

### Sprint 6 — Validación
1. Probar sin internet.
2. Probar sincronización.
3. Probar envío Telegram.
4. Probar rotación de pantalla.
5. Mejorar diseño y mensajes de error.

---

## 17. Recomendación de evolución del PMV

Para un PMV realista, se recomienda implementar primero:

1. **Pantalla de asistencia con datos demo.**
2. **Pantalla de incidentes con envío Telegram.**
3. **Room local para guardar incidentes y asistencia.**
4. **OCR básico con ML Kit solo para extraer texto.**

Luego se puede avanzar a:

1. Sincronización con API REST.
2. Confirmación de lectura.
3. Gestión de representantes.
4. Login por rol.
5. Panel administrativo web.

---

## 18. Conclusión técnica

AlegriAPP debe estructurarse como una aplicación Android moderna, modular y escalable. La estructura propuesta permite separar UI, lógica de negocio, datos e integraciones externas. Esto facilita que el equipo pueda trabajar por partes: una persona en pantallas, otra en Room, otra en Telegram y otra en ML Kit.

La clave del proyecto no es solo que funcione, sino que sea mantenible. Por eso se recomienda usar MVVM, modelos inmutables, estados sellados, Room para offline, Retrofit para comunicación, Telegram para notificaciones y ML Kit para digitalizar asistencia física.

Con esta guía, el equipo puede transformar la estructura inicial del proyecto en una base profesional para desarrollar el PMV de AlegriAPP.
