package com.example.myapplication.presentation.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.presentation.attendance.components.AttendanceListCard
import com.example.myapplication.presentation.common.OfflineBanner
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AttendanceScreenRoute(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        AttendanceViewModel(
            getAttendanceByDateUseCase = AppModule.provideGetAttendanceByDateUseCase(context),
            saveAttendanceUseCase = AppModule.provideSaveAttendanceUseCase(context),
            recognizeTextFromImageUseCase = AppModule.provideRecognizeTextFromImageUseCase(context),
            sendTelegramMessageUseCase = AppModule.provideSendTelegramMessageUseCase(),
            networkMonitor = AppModule.provideNetworkMonitor(context),
            syncRepository = AppModule.provideSyncRepository(context)
        )
    }
    AttendanceScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(AttendanceEvent.OcrImageSelected(uri))
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
                            text = "Asistencias",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Asistencia Padres de Familia",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentMaxWidth = if (maxWidth > 900.dp) 820.dp else maxWidth

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.isOffline) {
                    item {
                        OfflineBanner(
                            modifier = Modifier.widthIn(max = contentMaxWidth)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = uiState.screenTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = uiState.dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth),
                        onClick = { pickImageLauncher.launch("image/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ImageSearch,
                            contentDescription = null
                        )
                        Text(" Escanear lista con OCR")
                    }
                }

                if (uiState.detectedOcrText.isNotBlank()) {
                    item {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth),
                            onClick = { viewModel.onEvent(AttendanceEvent.ApplyOcrSuggestions) }
                        ) {
                            Text("Aplicar sugerencias OCR")
                        }
                    }
                }

                if (uiState.detectedOcrText.isNotBlank()) {
                    item {
                        Text(
                            text = uiState.detectedOcrText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth),
                        onClick = { viewModel.onEvent(AttendanceEvent.MarkAllPresent) },
                        enabled = !uiState.isSaving && !uiState.isLoading && uiState.students.isNotEmpty()
                    ) {
                        Text("Marcar todos presente")
                    }
                }

                item {
                    AttendanceListCard(
                        modifier = Modifier.widthIn(max = contentMaxWidth),
                        courseName = uiState.courseName,
                        students = uiState.students,
                        registeredCount = uiState.registeredCount,
                        statusByStudent = uiState.attendanceByStudent,
                        onStatusSelected = { studentId, status ->
                            when (status) {
                                AttendanceStatus.PRESENT -> viewModel.onEvent(AttendanceEvent.MarkPresent(studentId))
                                AttendanceStatus.LATE -> viewModel.onEvent(AttendanceEvent.MarkLate(studentId))
                                AttendanceStatus.ABSENT -> viewModel.onEvent(AttendanceEvent.MarkAbsent(studentId))
                                AttendanceStatus.JUSTIFIED -> viewModel.onEvent(AttendanceEvent.MarkJustified(studentId))
                                AttendanceStatus.UNMARKED -> Unit
                            }
                        }
                    )
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth),
                        onClick = { viewModel.onEvent(AttendanceEvent.SaveAttendance) },
                        enabled = !uiState.isSaving && !uiState.isLoading
                    ) {
                        Text(
                            if (uiState.isSaving) "Guardando asistencia…" else "Guardar asistencia"
                        )
                    }
                }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = contentMaxWidth),
                        onClick = { viewModel.onEvent(AttendanceEvent.SendReport) },
                        enabled = !uiState.isSending && !uiState.isSaving && !uiState.isSaving
                    ) {
                        Text("Enviar reporte por Telegram")
                    }
                }

                if (uiState.isLoading) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth)
                        )
                    }
                }
                if (uiState.isProcessingOcr) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AttendanceScreenPreview() {
    MyApplicationTheme {
        AttendanceScreenRoute()
    }
}
