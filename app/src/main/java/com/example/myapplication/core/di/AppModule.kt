package com.example.myapplication.core.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DatabaseSeeder
import com.example.myapplication.data.local.migrations.Migration_5_6
import com.example.myapplication.data.local.migrations.Migration_6_7
import com.example.myapplication.data.local.migrations.Migration_7_8
import com.example.myapplication.data.local.migrations.Migration_8_9
import com.example.myapplication.data.local.migrations.Migration_9_10
import com.example.myapplication.data.repository.CatalogRepositoryImpl
import com.example.myapplication.domain.repository.CatalogRepository
import com.example.myapplication.domain.usecase.attendance.GetAttendanceByDateAndCourseUseCase
import com.example.myapplication.domain.usecase.grade.GetGradesByCatalogFiltersUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsByCourseUseCase
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.core.network.RetrofitClient
import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.core.preferences.SyncPreferences
import com.example.myapplication.core.sync.SyncScheduler
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.repository.AttendanceRepositoryImpl
import com.example.myapplication.data.repository.AuthRepositoryImpl
import com.example.myapplication.data.repository.GradeRepositoryImpl
import com.example.myapplication.data.repository.IncidentRepositoryImpl
import com.example.myapplication.data.repository.OcrRepositoryImpl
import com.example.myapplication.data.repository.StudentRepositoryImpl
import com.example.myapplication.data.repository.SyncRepositoryImpl
import com.example.myapplication.data.repository.TelegramRepositoryImpl
import com.example.myapplication.data.repository.TeacherDataSyncer
import com.example.myapplication.data.remote.api.TelegramApiService
import com.example.myapplication.domain.repository.SyncRepository
import com.example.myapplication.domain.repository.AttendanceRepository
import com.example.myapplication.domain.repository.AuthRepository
import com.example.myapplication.domain.repository.GradeRepository
import com.example.myapplication.domain.repository.IncidentRepository
import com.example.myapplication.domain.repository.OcrRepository
import com.example.myapplication.domain.repository.StudentRepository
import com.example.myapplication.domain.repository.TelegramRepository
import com.example.myapplication.domain.service.AttendanceTranscriptionService
import com.example.myapplication.domain.usecase.attendance.GetAttendanceByDateUseCase
import com.example.myapplication.domain.usecase.attendance.SaveAttendanceUseCase
import com.example.myapplication.domain.usecase.grade.GetGradesByStudentUseCase
import com.example.myapplication.domain.usecase.grade.GetGradesBySubjectAndPeriodUseCase
import com.example.myapplication.domain.usecase.grade.SaveGradeUseCase
import com.example.myapplication.domain.usecase.incidents.SaveIncidentUseCase
import com.example.myapplication.domain.usecase.incidents.SendIncidentReportUseCase
import com.example.myapplication.domain.usecase.incidents.SendPendingIncidentsUseCase
import com.example.myapplication.domain.usecase.auth.LoginUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentByIdUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import com.example.myapplication.domain.usecase.telegram.SendParentTelegramUseCase
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
import com.example.myapplication.services.mlkit.TextRecognitionProcessor
import com.example.myapplication.services.telegram.TelegramConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppModule {

    /**
     * Scope de aplicación para tareas de inicialización que NO deben bloquear el
     * hilo que construye la base (antes se usaba `runBlocking`, mala práctica que
     * congelaba el hilo llamante mientras se sembraba la BD). Usa [SupervisorJob]
     * para que un fallo en una tarea no cancele las demás.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var db: AppDatabase? = null

    @Volatile
    private var networkMonitor: NetworkMonitor? = null

    @Volatile
    private var supabaseApi: SupabaseApiService? = null

    @Volatile
    private var syncScheduler: SyncScheduler? = null

    @Volatile
    private var syncPreferences: SyncPreferences? = null

    @Volatile
    private var authPreferences: AuthPreferences? = null

    @Volatile
    private var telegramHttpClient: OkHttpClient? = null

    @Volatile
    private var catalogRepository: CatalogRepository? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var teacherDataSyncer: TeacherDataSyncer? = null

    fun provideDatabase(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "alegriapp.db"
            )
                .addMigrations(Migration_5_6, Migration_6_7, Migration_7_8, Migration_8_9, Migration_9_10)
                // Solo v1–v4 sin migración definida. No incluir 5 ni 6: chocan con Migration_5_6 / Migration_6_7.
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { database ->
                    db = database
                    // Siembra demo en segundo plano (no bloquea al llamante). Las
                    // pantallas leen vía Flow, así que se refrescan solas cuando
                    // la data demo queda insertada.
                    appScope.launch {
                        DatabaseSeeder.seedIfEmpty(database)
                    }
                }
        }

    fun provideNetworkMonitor(context: Context): NetworkMonitor =
        networkMonitor ?: synchronized(this) {
            networkMonitor ?: NetworkMonitor(context.applicationContext).also { networkMonitor = it }
        }

    fun provideSyncScheduler(context: Context): SyncScheduler =
        syncScheduler ?: synchronized(this) {
            syncScheduler ?: SyncScheduler(context.applicationContext).also { syncScheduler = it }
        }

    fun provideSyncPreferences(context: Context): SyncPreferences =
        syncPreferences ?: synchronized(this) {
            syncPreferences ?: SyncPreferences(context.applicationContext).also { syncPreferences = it }
        }

    fun provideAuthPreferences(context: Context): AuthPreferences =
        authPreferences ?: synchronized(this) {
            authPreferences ?: AuthPreferences(context.applicationContext).also { authPreferences = it }
        }

    fun provideSupabaseApiService(): SupabaseApiService? =
        supabaseApi ?: synchronized(this) {
            supabaseApi ?: RetrofitClient.createSupabaseApi(
                baseUrl = BuildConfig.SUPABASE_URL,
                apiKey = BuildConfig.SUPABASE_KEY
            ).also { supabaseApi = it }
        }

    fun provideCatalogRepository(context: Context): CatalogRepository =
        catalogRepository ?: synchronized(this) {
            catalogRepository ?: CatalogRepositoryImpl(
                supabaseApi = provideSupabaseApiService(),
                catalogDao = provideDatabase(context).catalogDao(),
                studentDao = provideDatabase(context).studentDao(),
                authPreferences = provideAuthPreferences(context)
            ).also { catalogRepository = it }
        }

    fun provideTeacherDataSyncer(context: Context): TeacherDataSyncer =
        teacherDataSyncer ?: synchronized(this) {
            val database = provideDatabase(context)
            teacherDataSyncer ?: TeacherDataSyncer(
                supabaseApi = provideSupabaseApiService(),
                catalogDao = database.catalogDao(),
                studentDao = database.studentDao()
            ).also { teacherDataSyncer = it }
        }

    fun provideAuthRepository(context: Context): AuthRepository =
        authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                supabaseApi = provideSupabaseApiService(),
                authPreferences = provideAuthPreferences(context),
                teacherDataSyncer = provideTeacherDataSyncer(context)
            ).also { authRepository = it }
        }

    fun provideSyncRepository(context: Context): SyncRepository {
        val database = provideDatabase(context)
        return SyncRepositoryImpl(
            supabaseApi = provideSupabaseApiService(),
            studentDao = database.studentDao(),
            attendanceDao = database.attendanceDao(),
            gradeDao = database.gradeDao(),
            incidentDao = database.incidentDao(),
            catalogDao = database.catalogDao(),
            catalogRepository = provideCatalogRepository(context),
            networkMonitor = provideNetworkMonitor(context),
            authPreferences = provideAuthPreferences(context),
            teacherDataSyncer = provideTeacherDataSyncer(context)
        )
    }

    fun provideStudentRepository(context: Context): StudentRepository =
        StudentRepositoryImpl(
            studentDao = provideDatabase(context).studentDao(),
            authPreferences = provideAuthPreferences(context)
        )

    fun provideAttendanceRepository(context: Context): AttendanceRepository =
        AttendanceRepositoryImpl(
            attendanceDao = provideDatabase(context).attendanceDao(),
            studentDao = provideDatabase(context).studentDao()
        )

    fun provideGradeRepository(context: Context): GradeRepository =
        GradeRepositoryImpl(
            gradeDao = provideDatabase(context).gradeDao()
        )

    fun provideIncidentRepository(context: Context): IncidentRepository =
        IncidentRepositoryImpl(
            incidentDao = provideDatabase(context).incidentDao()
        )

    fun provideGetStudentsUseCase(context: Context): GetStudentsUseCase =
        GetStudentsUseCase(provideStudentRepository(context))

    fun provideLoginUseCase(context: Context): LoginUseCase =
        LoginUseCase(provideAuthRepository(context))

    fun provideGetAttendanceByDateUseCase(context: Context): GetAttendanceByDateUseCase =
        GetAttendanceByDateUseCase(provideAttendanceRepository(context))

    fun provideGetAttendanceByDateAndCourseUseCase(context: Context): GetAttendanceByDateAndCourseUseCase =
        GetAttendanceByDateAndCourseUseCase(provideAttendanceRepository(context))

    fun provideGetStudentsByCourseUseCase(context: Context): GetStudentsByCourseUseCase =
        GetStudentsByCourseUseCase(provideStudentRepository(context))

    fun provideGetGradesByCatalogFiltersUseCase(context: Context): GetGradesByCatalogFiltersUseCase =
        GetGradesByCatalogFiltersUseCase(provideGradeRepository(context))

    fun provideSaveAttendanceUseCase(context: Context): SaveAttendanceUseCase =
        SaveAttendanceUseCase(provideAttendanceRepository(context))

    fun provideGetGradesBySubjectAndPeriodUseCase(context: Context): GetGradesBySubjectAndPeriodUseCase =
        GetGradesBySubjectAndPeriodUseCase(provideGradeRepository(context))

    fun provideGetGradesByStudentUseCase(context: Context): GetGradesByStudentUseCase =
        GetGradesByStudentUseCase(provideGradeRepository(context))

    fun provideGetStudentByIdUseCase(context: Context): GetStudentByIdUseCase =
        GetStudentByIdUseCase(provideStudentRepository(context))

    fun provideSaveGradeUseCase(context: Context): SaveGradeUseCase =
        SaveGradeUseCase(provideGradeRepository(context))

    fun provideSaveIncidentUseCase(context: Context): SaveIncidentUseCase =
        SaveIncidentUseCase(provideIncidentRepository(context))

    fun provideSendParentTelegramUseCase(context: Context): SendParentTelegramUseCase =
        SendParentTelegramUseCase(
            sendTelegramMessageUseCase = provideSendTelegramMessageUseCase(),
            catalogRepository = provideCatalogRepository(context),
            defaultChatId = BuildConfig.TELEGRAM_DEFAULT_CHAT_ID,
            defaultBotToken = BuildConfig.TELEGRAM_BOT_TOKEN
        )

    fun provideSendIncidentReportUseCase(context: Context): SendIncidentReportUseCase =
        SendIncidentReportUseCase(
            sendParentTelegramUseCase = provideSendParentTelegramUseCase(context),
            incidentRepository = provideIncidentRepository(context)
        )

    fun provideSendPendingIncidentsUseCase(context: Context): SendPendingIncidentsUseCase =
        SendPendingIncidentsUseCase(
            incidentRepository = provideIncidentRepository(context),
            studentRepository = provideStudentRepository(context),
            sendIncidentReportUseCase = provideSendIncidentReportUseCase(context)
        )

    fun provideOcrRepository(context: Context): OcrRepository =
        OcrRepositoryImpl(
            processor = TextRecognitionProcessor(context.applicationContext)
        )

    fun provideRecognizeTextFromImageUseCase(context: Context): RecognizeTextFromImageUseCase =
        RecognizeTextFromImageUseCase(provideOcrRepository(context))

    fun provideAttendanceTranscriptionService(): AttendanceTranscriptionService =
        AttendanceTranscriptionService()

    fun provideTelegramHttpClient(): OkHttpClient =
        telegramHttpClient ?: synchronized(this) {
            telegramHttpClient ?: OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.HEADERS
                    }
                )
                .build()
                .also { telegramHttpClient = it }
        }

    fun provideTelegramApiService(): TelegramApiService {
        val botToken = BuildConfig.TELEGRAM_BOT_TOKEN
        return Retrofit.Builder()
            .baseUrl(TelegramConfig.botBaseUrl(botToken.ifBlank { "placeholder" }))
            .client(provideTelegramHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TelegramApiService::class.java)
    }

    fun provideTelegramRepository(): TelegramRepository =
        TelegramRepositoryImpl(
            defaultApi = provideTelegramApiService(),
            defaultBotToken = BuildConfig.TELEGRAM_BOT_TOKEN,
            httpClient = provideTelegramHttpClient()
        )

    fun provideSendTelegramMessageUseCase(): SendTelegramMessageUseCase =
        SendTelegramMessageUseCase(provideTelegramRepository())
}
