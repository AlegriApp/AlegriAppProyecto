import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { load(it) }
        }
    }
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val telegramToken = (localProperties.getProperty("TELEGRAM_BOT_TOKEN") ?: "").replace("\"", "\\\"")
        val telegramChatId = (localProperties.getProperty("TELEGRAM_CHAT_ID") ?: "").replace("\"", "\\\"")
        val supabaseUrl = (localProperties.getProperty("SUPABASE_URL")
            ?: "https://nqtobrslyrfwcuexffdu.supabase.co/rest/v1/").replace("\"", "\\\"")
        val supabaseKey = (localProperties.getProperty("SUPABASE_KEY") ?: "").replace("\"", "\\\"")
        val defaultCursoId = localProperties.getProperty("SUPABASE_DEFAULT_CURSO_ID") ?: "1"
        val defaultMateriaId = localProperties.getProperty("SUPABASE_DEFAULT_MATERIA_ID") ?: "1"
        val defaultTipoEvalId = localProperties.getProperty("SUPABASE_DEFAULT_TIPO_EVALUACION_ID") ?: "6"
        val defaultPeriodoId = localProperties.getProperty("SUPABASE_DEFAULT_PERIODO_ID") ?: "1"
        val defaultTipoIncidenteId = localProperties.getProperty("SUPABASE_DEFAULT_TIPO_INCIDENTE_ID") ?: "1"
        val defaultReportadoPorId = localProperties.getProperty("SUPABASE_DEFAULT_REPORTADO_POR_ID") ?: "0"
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$telegramToken\"")
        buildConfigField("String", "TELEGRAM_DEFAULT_CHAT_ID", "\"$telegramChatId\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("long", "SUPABASE_DEFAULT_CURSO_ID", "${defaultCursoId}L")
        buildConfigField("long", "SUPABASE_DEFAULT_MATERIA_ID", "${defaultMateriaId}L")
        buildConfigField("long", "SUPABASE_DEFAULT_TIPO_EVALUACION_ID", "${defaultTipoEvalId}L")
        buildConfigField("long", "SUPABASE_DEFAULT_PERIODO_ID", "${defaultPeriodoId}L")
        buildConfigField("long", "SUPABASE_DEFAULT_TIPO_INCIDENTE_ID", "${defaultTipoIncidenteId}L")
        buildConfigField("long", "SUPABASE_DEFAULT_REPORTADO_POR_ID", "${defaultReportadoPorId}L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
}
