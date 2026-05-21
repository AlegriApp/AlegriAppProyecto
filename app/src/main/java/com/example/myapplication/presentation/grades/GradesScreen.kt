package com.example.myapplication.presentation.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.presentation.grades.components.GradeFilterSection
import com.example.myapplication.presentation.grades.components.GradeStudentCard
import com.example.myapplication.presentation.grades.components.GradeSummaryCard
import com.example.myapplication.presentation.grades.components.GradeSummaryIcons
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun GradesScreen(
    onBack: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        GradesViewModel(
            getStudentsUseCase = AppModule.provideGetStudentsUseCase(context),
            getGradesBySubjectAndPeriodUseCase = AppModule.provideGetGradesBySubjectAndPeriodUseCase(context),
            saveGradeUseCase = AppModule.provideSaveGradeUseCase(context),
            recognizeTextFromImageUseCase = AppModule.provideRecognizeTextFromImageUseCase(context),
            sendTelegramMessageUseCase = AppModule.provideSendTelegramMessageUseCase()
        )
    }
    GradesScreenContent(
        viewModel = viewModel,
        onBack = onBack,
        onOpenDetail = { studentId, _, _ -> onOpenDetail(studentId) }
    )
}

@Composable
fun GradesScreenRoute(
    onBack: () -> Unit = {},
    onOpenDetail: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        GradesViewModel(
            getStudentsUseCase = AppModule.provideGetStudentsUseCase(context),
            getGradesBySubjectAndPeriodUseCase = AppModule.provideGetGradesBySubjectAndPeriodUseCase(context),
            saveGradeUseCase = AppModule.provideSaveGradeUseCase(context),
            recognizeTextFromImageUseCase = AppModule.provideRecognizeTextFromImageUseCase(context),
            sendTelegramMessageUseCase = AppModule.provideSendTelegramMessageUseCase()
        )
    }
    GradesScreenContent(
        viewModel = viewModel,
        onBack = onBack,
        onOpenDetail = onOpenDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradesScreenContent(
    viewModel: GradesViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long, String, String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val students = uiState.students
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(GradesEvent.OcrImageSelected(uri))
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Calificaciones",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registro académico",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Registro de Calificaciones",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = uiState.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GradeFilterSection(
                    selectedSubject = uiState.selectedSubject,
                    selectedPeriod = uiState.selectedPeriod,
                    subjects = uiState.subjects,
                    periods = uiState.periods,
                    onSubjectSelected = {
                        viewModel.onEvent(GradesEvent.SubjectSelected(it))
                    },
                    onPeriodSelected = {
                        viewModel.onEvent(GradesEvent.PeriodSelected(it))
                    }
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        GradeSummaryCard(
                            title = "Promedio sección",
                            value = String.format("%.1f", uiState.sectionAverage),
                            icon = GradeSummaryIcons.Average
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Estudiantes",
                            value = uiState.totalStudents.toString(),
                            icon = GradeSummaryIcons.Students
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "En riesgo",
                            value = uiState.atRiskStudents.toString(),
                            icon = GradeSummaryIcons.Risk
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Boletines pendientes",
                            value = uiState.pendingBulletins.toString(),
                            icon = GradeSummaryIcons.Pending
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Estudiantes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(students, key = { it.id }) { student ->
                GradeStudentCard(
                    student = student,
                    onOpenDetail = { studentId ->
                        onOpenDetail(studentId, uiState.selectedSubject, uiState.selectedPeriod)
                    }
                )
            }

            item {
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ImageSearch,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Escanear calificaciones con OCR")
                }
            }

            if (uiState.detectedOcrText.isNotBlank()) {
                item {
                    Text(
                        text = uiState.detectedOcrText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.onEvent(GradesEvent.SendBulletinClicked)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar boletín")
                }
            }

            if (uiState.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (uiState.isProcessingOcr) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GradesScreenPreview() {
    MyApplicationTheme {
        GradesScreen()
    }
}
