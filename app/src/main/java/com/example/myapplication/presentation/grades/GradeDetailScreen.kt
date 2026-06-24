package com.example.myapplication.presentation.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.presentation.grades.components.GradeDetailEmptyState
import com.example.myapplication.presentation.grades.components.GradeDetailErrorState
import com.example.myapplication.presentation.grades.components.GradeDetailHeaderCard
import com.example.myapplication.presentation.grades.components.GradeDetailLoadingState
import com.example.myapplication.presentation.grades.components.GradeDetailOfflineBanner
import com.example.myapplication.presentation.grades.components.GradeDetailOfflineState
import com.example.myapplication.presentation.grades.components.GradeDetailStudent
import com.example.myapplication.presentation.grades.components.GradeSummaryCard
import com.example.myapplication.presentation.grades.components.GradeSummaryIcons
import com.example.myapplication.presentation.grades.components.gradeDetailMockStudent
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * Punto de entrada de producción del detalle de calificaciones.
 *
 * Crea el [GradeDetailViewModel] con la DI manual del proyecto ([AppModule]) y
 * observa el estado real (Room/offline-first). Reemplaza el flujo mock anterior.
 */
@Composable
fun GradeDetailScreenRoute(
    studentId: Long,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GradeDetailViewModel = viewModel(
        key = "grade_detail_$studentId",
        factory = viewModelFactory {
            initializer {
                GradeDetailViewModel(
                    studentId = studentId,
                    getStudentByIdUseCase = AppModule.provideGetStudentByIdUseCase(context),
                    getGradesByStudentUseCase = AppModule.provideGetGradesByStudentUseCase(context),
                    networkMonitor = AppModule.provideNetworkMonitor(context),
                    syncRepository = AppModule.provideSyncRepository(context)
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GradeDetailContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onSyncNow = viewModel::refresh
    )
}

/**
 * Versión "tonta" (stateless) para @Preview y tests: renderiza un estado fijo
 * sin ViewModel. No carga datos mock; solo dibuja el [GradeDetailUiState] dado.
 */
@Composable
fun GradeDetailScreen(
    studentId: Long,
    onBack: () -> Unit = {},
    initialState: GradeDetailUiState? = null
) {
    GradeDetailContent(
        uiState = initialState ?: GradeDetailUiState.Loading,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeDetailContent(
    uiState: GradeDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    onSyncNow: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Calificaciones del estudiante",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Detalle académico",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                GradeDetailUiState.Loading -> GradeDetailLoadingState()
                GradeDetailUiState.Empty -> GradeDetailEmptyState(onRetry = onRetry)
                GradeDetailUiState.Offline -> GradeDetailOfflineState(onRetry = onRetry)
                is GradeDetailUiState.Error -> GradeDetailErrorState(
                    message = state.message,
                    onRetry = onRetry
                )
                is GradeDetailUiState.Success -> GradeDetailSuccessContent(
                    student = state.student,
                    isFromCache = state.isFromCache,
                    onExportPdf = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Prototipo visual: exportar PDF pendiente"
                            )
                        }
                    },
                    onSyncNow = {
                        onSyncNow()
                        scope.launch {
                            snackbarHostState.showSnackbar("Sincronizando con el servidor…")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GradeDetailSuccessContent(
    student: GradeDetailStudent,
    isFromCache: Boolean,
    onExportPdf: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (isFromCache) {
            GradeDetailOfflineBanner()
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Resumen del estudiante",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Información general y avance académico",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { GradeDetailHeaderCard(student = student) }

            item {
                Text(
                    text = "Resumen visual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        GradeSummaryCard(
                            title = "Promedio general",
                            value = String.format("%.1f", student.generalAverage),
                            icon = GradeSummaryIcons.Average
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Materias aprobadas",
                            value = student.approvedSubjects.toString(),
                            icon = GradeSummaryIcons.Students
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Bajo rendimiento",
                            value = student.atRiskSubjects.toString(),
                            icon = GradeSummaryIcons.Risk
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Última sincronización",
                            value = student.lastSyncDate,
                            icon = GradeSummaryIcons.Pending
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Detalle por materia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(student.subjects, key = { it.id }) { subject ->
                com.example.myapplication.presentation.grades.components.GradeDetailSubjectCard(
                    subject = subject,
                    initiallyExpanded = subject == student.subjects.firstOrNull()
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar boletín en PDF")
                    }
                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizar ahora")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailSuccessPreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Success(
                student = gradeDetailMockStudent,
                isFromCache = false
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailOfflineSuccessPreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Success(
                student = gradeDetailMockStudent,
                isFromCache = true
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailEmptyPreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Empty
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailLoadingPreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Loading
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailErrorPreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Error("No se pudo cargar la información.")
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailOfflineStatePreview() {
    MyApplicationTheme {
        GradeDetailScreen(
            studentId = 1L,
            initialState = GradeDetailUiState.Offline
        )
    }
}
