package com.example.myapplication.core.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DatabaseSeeder
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.core.network.RetrofitClient
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.data.repository.AttendanceRepositoryImpl
import com.example.myapplication.data.repository.GradeRepositoryImpl
import com.example.myapplication.data.repository.OcrRepositoryImpl
import com.example.myapplication.data.repository.StudentRepositoryImpl
import com.example.myapplication.data.repository.SyncRepositoryImpl
import com.example.myapplication.data.repository.TelegramRepositoryImpl
import com.example.myapplication.data.remote.api.TelegramApiService
import com.example.myapplication.domain.repository.SyncRepository
import com.example.myapplication.domain.repository.AttendanceRepository
import com.example.myapplication.domain.repository.GradeRepository
import com.example.myapplication.domain.repository.OcrRepository
import com.example.myapplication.domain.repository.StudentRepository
import com.example.myapplication.domain.repository.TelegramRepository
import com.example.myapplication.domain.usecase.attendance.GetAttendanceByDateUseCase
import com.example.myapplication.domain.usecase.attendance.SaveAttendanceUseCase
import com.example.myapplication.domain.usecase.grade.GetGradesBySubjectAndPeriodUseCase
import com.example.myapplication.domain.usecase.grade.SaveGradeUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
import com.example.myapplication.services.mlkit.TextRecognitionProcessor
import com.example.myapplication.services.telegram.TelegramConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppModule {
    @Volatile
    private var db: AppDatabase? = null

    @Volatile
    private var networkMonitor: NetworkMonitor? = null

    @Volatile
    private var supabaseApi: SupabaseApiService? = null

    fun provideDatabase(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "alegriapp.db"
            ).fallbackToDestructiveMigration().build().also { database ->
                runBlocking(Dispatchers.IO) {
                    DatabaseSeeder.seedIfEmpty(database)
                }
                db = database
            }
        }

    fun provideNetworkMonitor(context: Context): NetworkMonitor =
        networkMonitor ?: synchronized(this) {
            networkMonitor ?: NetworkMonitor(context.applicationContext).also { networkMonitor = it }
        }

    fun provideSupabaseApiService(): SupabaseApiService? =
        supabaseApi ?: synchronized(this) {
            supabaseApi ?: RetrofitClient.createSupabaseApi(
                baseUrl = BuildConfig.SUPABASE_URL,
                apiKey = BuildConfig.SUPABASE_KEY
            ).also { supabaseApi = it }
        }

    fun provideSyncRepository(context: Context): SyncRepository {
        val database = provideDatabase(context)
        return SyncRepositoryImpl(
            supabaseApi = provideSupabaseApiService(),
            studentDao = database.studentDao(),
            attendanceDao = database.attendanceDao(),
            gradeDao = database.gradeDao(),
            networkMonitor = provideNetworkMonitor(context)
        )
    }

    fun provideStudentRepository(context: Context): StudentRepository =
        StudentRepositoryImpl(
            studentDao = provideDatabase(context).studentDao()
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

    fun provideGetStudentsUseCase(context: Context): GetStudentsUseCase =
        GetStudentsUseCase(provideStudentRepository(context))

    fun provideGetAttendanceByDateUseCase(context: Context): GetAttendanceByDateUseCase =
        GetAttendanceByDateUseCase(provideAttendanceRepository(context))

    fun provideSaveAttendanceUseCase(context: Context): SaveAttendanceUseCase =
        SaveAttendanceUseCase(provideAttendanceRepository(context))

    fun provideGetGradesBySubjectAndPeriodUseCase(context: Context): GetGradesBySubjectAndPeriodUseCase =
        GetGradesBySubjectAndPeriodUseCase(provideGradeRepository(context))

    fun provideSaveGradeUseCase(context: Context): SaveGradeUseCase =
        SaveGradeUseCase(provideGradeRepository(context))

    fun provideOcrRepository(context: Context): OcrRepository =
        OcrRepositoryImpl(
            processor = TextRecognitionProcessor(context.applicationContext)
        )

    fun provideRecognizeTextFromImageUseCase(context: Context): RecognizeTextFromImageUseCase =
        RecognizeTextFromImageUseCase(provideOcrRepository(context))

    fun provideTelegramApiService(): TelegramApiService {
        val botToken = BuildConfig.TELEGRAM_BOT_TOKEN
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(TelegramConfig.botBaseUrl(botToken.ifBlank { "placeholder" }))
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TelegramApiService::class.java)
    }

    fun provideTelegramRepository(): TelegramRepository =
        TelegramRepositoryImpl(
            api = provideTelegramApiService(),
            botToken = BuildConfig.TELEGRAM_BOT_TOKEN
        )

    fun provideSendTelegramMessageUseCase(): SendTelegramMessageUseCase =
        SendTelegramMessageUseCase(provideTelegramRepository())
}
