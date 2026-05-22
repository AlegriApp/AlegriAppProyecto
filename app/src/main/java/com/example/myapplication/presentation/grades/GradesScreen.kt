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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.presentation.common.OfflineBanner
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
            sendTelegramMessageUseCase = AppModule.provideSendTelegramMessageUseCase(),
            networkMonitor = AppModule.provideNetworkMonitor(context),
            syncRepository = AppModule.provideSyncRepository(context)
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
            sendTelegramMessageUseCase = AppModule.provideSendTelegramMessageUseCase(),
            networkMonitor = AppModule.provideNetworkMonitor(context),
            syncRepository = AppModule.provideSyncRepository(context)
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

    val editingStudent = uiState.editingStudentId?.let { id ->
        students.firstOrNull { it.id == id }
    }
    if (editingStudent != null) {
        GradeEditDialog(
            studentName = editingStudent.name,
            initialScore = editingStudent.score?.toString() ?: "",
            maxScore = editingStudent.maxScore,
            onDismiss = { viewModel.onEvent(GradesEvent.DismissEditDialog) },
            onConfirm = { score ->
                viewModel.onEvent(
                    GradesEvent.EditGrade(
                        studentId = editingStudent.id,
                        activityName = GradeEditDraft.DEFAULT_ACTIVITY_NAME,
                        activityType = GradeEditDraft.DEFAULT_ACTIVITY_TYPE,
                        score = score,
                        maxScore = editingStudent.maxScore.toDouble()
                    )
                )
            }
        )
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
            if (uiState.isOffline) {
                item {
                    OfflineBanner()
                }
            }

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
                    },
                    onEdit = { studentId ->
                        viewModel.onEvent(GradesEvent.OpenEditDialog(studentId))
                    }
                )
            }

            item {
                Button(
                    onClick = { viewModel.onEvent(GradesEvent.SaveGrades) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && !uiState.isLoading && uiState.hasUnsavedEdits
                ) {
                    Text(
                        if (uiState.isSaving) "Guardando calificaciones…" else "Guardar calificaciones"
                    )
                }
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
                item {
                    Button(
                        onClick = { viewModel.onEvent(GradesEvent.ApplyOcrSuggestions) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isProcessingOcr
                    ) {
                        Text("Aplicar sugerencias OCR")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.onEvent(GradesEvent.SendBulletinClicked)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSending && !uiState.isSaving
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

@Composable
private fun GradeEditDialog(
    studentName: String,
    initialScore: String,
    maxScore: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var scoreText by remember(initialScore, studentName) { mutableStateOf(initialScore) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nota de $studentName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = {
                        scoreText = it
                        errorText = null
                    },
                    label = { Text("Nota (0–$maxScore)") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val score = scoreText.replace(',', '.').toDoubleOrNull()
                    when {
                        score == null -> errorText = "Ingresa un número válido."
                        score !in 0.0..maxScore.toDouble() -> errorText = "La nota debe estar entre 0 y $maxScore."
                        else -> onConfirm(score)
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun GradesScreenPreview() {
    MyApplicationTheme {
        GradesScreen()
    }
}
