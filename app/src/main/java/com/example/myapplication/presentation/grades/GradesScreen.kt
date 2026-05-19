package com.example.myapplication.presentation.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.presentation.grades.components.GradeFilterSection
import com.example.myapplication.presentation.grades.components.GradeStudentCard
import com.example.myapplication.presentation.grades.components.GradeSummaryCard
import com.example.myapplication.presentation.grades.components.GradeSummaryIcons
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.presentation.grades.components.gradesMockStudents
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@Composable
fun GradesScreen(
    onBack: () -> Unit = {},
    onOpenDetail: (Long) -> Unit = {}
) {
    GradesScreenContent(
        onBack = onBack,
        onOpenDetail = { studentId, _, _ -> onOpenDetail(studentId) }
    )
}

@Composable
fun GradesScreenRoute(
    onBack: () -> Unit = {},
    onOpenDetail: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    GradesScreenContent(
        onBack = onBack,
        onOpenDetail = onOpenDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradesScreenContent(
    onBack: () -> Unit,
    onOpenDetail: (Long, String, String) -> Unit
) {
    var selectedSubject by rememberSaveable { mutableStateOf(GradesUiState().selectedSubject) }
    var selectedPeriod by rememberSaveable { mutableStateOf(GradesUiState().selectedPeriod) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val students = gradesMockStudents
    val average = students.mapNotNull { it.score }.average().takeIf { !it.isNaN() } ?: 0.0
    val riskCount = students.count { it.status == GradeVisualStatus.AT_RISK }
    val pendingCount = students.count { it.status == GradeVisualStatus.NOT_REGISTERED }

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
                        text = GradesUiState().courseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                GradeFilterSection(
                    selectedSubject = selectedSubject,
                    selectedPeriod = selectedPeriod,
                    onSubjectSelected = { selectedSubject = it },
                    onPeriodSelected = { selectedPeriod = it }
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
                            value = String.format("%.1f", average),
                            icon = GradeSummaryIcons.Average
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Estudiantes",
                            value = students.size.toString(),
                            icon = GradeSummaryIcons.Students
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "En riesgo",
                            value = riskCount.toString(),
                            icon = GradeSummaryIcons.Risk
                        )
                    }
                    item {
                        GradeSummaryCard(
                            title = "Boletines pendientes",
                            value = pendingCount.toString(),
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
                        onOpenDetail(studentId, selectedSubject, selectedPeriod)
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Prototipo visual: envío por Telegram pendiente"
                            )
                        }
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDetailPlaceholderScreen(
    studentId: Long,
    selectedSubject: String,
    selectedPeriod: String,
    onBack: () -> Unit = {}
) {
    val studentName = gradesMockStudents.firstOrNull { it.id == studentId }?.name ?: "Estudiante"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle de calificaciones") },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Detalle de calificaciones",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text("Estudiante: $studentName")
                Text("Materia: $selectedSubject")
                Text("Periodo: $selectedPeriod")
                Text(
                    text = "Vista de detalle pendiente de lógica",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBack) {
                    Text("Volver")
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

@Preview(showBackground = true)
@Composable
private fun GradeDetailPlaceholderPreview() {
    MyApplicationTheme {
        GradeDetailPlaceholderScreen(
            studentId = 1L,
            selectedSubject = "Matemáticas",
            selectedPeriod = "1er Lapso"
        )
    }
}
