package com.example.myapplication.presentation.incidents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.Student
import com.example.myapplication.ui.theme.AlegriWarning
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PuceBlue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun IncidentScreen(
    onBack: () -> Unit = {}
) {
    IncidentScreenRoute(onBack = onBack)
}

@Composable
fun IncidentScreenRoute(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember(context) {
        IncidentViewModel(
            getStudentsUseCase = AppModule.provideGetStudentsUseCase(context),
            saveIncidentUseCase = AppModule.provideSaveIncidentUseCase(context),
            sendIncidentReportUseCase = AppModule.provideSendIncidentReportUseCase(context),
            incidentRepository = AppModule.provideIncidentRepository(context),
            studentRepository = AppModule.provideStudentRepository(context),
            recognizeTextFromImageUseCase = AppModule.provideRecognizeTextFromImageUseCase(context)
        )
    }
    IncidentScreenContent(
        uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
        onEvent = viewModel::onEvent,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentScreenContent(
    uiState: IncidentUiState,
    onEvent: (IncidentEvent) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isStudentMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onEvent(IncidentEvent.OcrImageSelected(uri))
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
                            text = "Incidentes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registro de Incidentes",
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentMaxWidth = if (maxWidth > 900.dp) 820.dp else maxWidth

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .widthIn(max = contentMaxWidth),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        IncidentHeader(uiState)

                        if (uiState.isLoadingStudents) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (uiState.students.isEmpty() && !uiState.isLoadingStudents) {
                            EmptyStudentsMessage(onRetry = { onEvent(IncidentEvent.LoadStudents) })
                        }

                        IncidentFormCard(
                            uiState = uiState,
                            isStudentMenuExpanded = isStudentMenuExpanded,
                            onStudentMenuExpandedChange = { isStudentMenuExpanded = it },
                            onPickOcrImage = { pickImageLauncher.launch("image/*") },
                            onEvent = onEvent
                        )

                        IncidentActionButtons(uiState = uiState, onEvent = onEvent)
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .widthIn(max = contentMaxWidth),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Historial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (uiState.isLoadingIncidents) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else if (uiState.incidents.isEmpty()) {
                            Text(
                                text = "Aun no hay incidentes registrados.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.incidents, key = { it.incident.id }) { item ->
                    IncidentHistoryCard(
                        item = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .widthIn(max = contentMaxWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStudentsMessage(onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "No hay estudiantes disponibles. Sincroniza o carga estudiantes antes de registrar incidentes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun IncidentHeader(uiState: IncidentUiState) {
    val colors = rememberIncidentColors()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = colors.accent
            )
            Text(
                text = uiState.screenTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = uiState.screenDescription,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IncidentFormCard(
    uiState: IncidentUiState,
    isStudentMenuExpanded: Boolean,
    onStudentMenuExpandedChange: (Boolean) -> Unit,
    onPickOcrImage: () -> Unit,
    onEvent: (IncidentEvent) -> Unit
) {
    val colors = rememberIncidentColors()
    val selectedStudent = uiState.selectedStudent

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.formCardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colors.formCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            IncidentFormSectionTitle("Estudiante involucrado")
            IncidentDropdownField(
                value = selectedStudent?.fullName
                    ?: uiState.manualStudentDraft.fullName.ifBlank { "Seleccionar estudiante..." },
                isPlaceholder = selectedStudent == null && uiState.manualStudentDraft.fullName.isBlank(),
                expanded = isStudentMenuExpanded,
                onClick = { onStudentMenuExpandedChange(!isStudentMenuExpanded) }
            )
            uiState.studentError?.let { ErrorText(it) }

            if (isStudentMenuExpanded) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.dropdownMenuContainer),
                    border = BorderStroke(1.dp, colors.formCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        uiState.students.forEach { student ->
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onEvent(IncidentEvent.StudentSelected(student.id))
                                        onStudentMenuExpandedChange(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickOcrImage,
                    enabled = !uiState.isProcessingOcr,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.ImageSearch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leer foto")
                }
                TextButton(
                    onClick = {
                        onEvent(IncidentEvent.ToggleManualStudentForm(!uiState.showManualStudentForm))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.showManualStudentForm) "Usar lista" else "Agregar manual")
                }
            }

            if (uiState.isProcessingOcr) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.ocrMatchMessage?.let { message ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.infoContainer
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            if (uiState.detectedOcrText.isNotBlank()) {
                OutlinedButton(
                    onClick = { onEvent(IncidentEvent.ApplyOcrSuggestions) },
                    enabled = !uiState.isProcessingOcr,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar sugerencia OCR")
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.infoContainer
                ) {
                    Text(
                        text = uiState.detectedOcrText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.showManualStudentForm) {
                IncidentFormSectionTitle("Registro manual del estudiante")
                OutlinedTextField(
                    value = uiState.manualStudentDraft.fullName,
                    onValueChange = { onEvent(IncidentEvent.ManualStudentNameChanged(it)) },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.manualStudentError != null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.manualStudentDraft.grade,
                        onValueChange = { onEvent(IncidentEvent.ManualStudentGradeChanged(it)) },
                        label = { Text("Grado") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.manualStudentDraft.section,
                        onValueChange = { onEvent(IncidentEvent.ManualStudentSectionChanged(it)) },
                        label = { Text("Seccion") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = uiState.manualStudentDraft.representativeName,
                    onValueChange = { onEvent(IncidentEvent.ManualRepresentativeChanged(it)) },
                    label = { Text("Representante") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                uiState.manualStudentError?.let { ErrorText(it) }
            }

            IncidentFormSectionTitle("Tipo de incidente")
            IncidentOptionGrid(
                options = IncidentType.entries,
                selected = uiState.selectedType,
                label = { it.label },
                onSelected = { onEvent(IncidentEvent.TypeSelected(it)) }
            )
            uiState.typeError?.let { ErrorText(it) }

            IncidentFormSectionTitle("Severidad")
            IncidentOptionGrid(
                options = IncidentSeverity.entries,
                selected = uiState.selectedSeverity,
                label = { it.label },
                onSelected = { onEvent(IncidentEvent.SeveritySelected(it)) }
            )

            IncidentFormSectionTitle("Descripcion del incidente")
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { onEvent(IncidentEvent.DescriptionChanged(it)) },
                placeholder = { Text("Describe detalladamente lo ocurrido...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.fieldBorderFocused,
                    unfocusedBorderColor = colors.fieldBorder,
                    focusedContainerColor = colors.fieldContainer,
                    unfocusedContainerColor = colors.fieldContainer,
                    cursorColor = colors.accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                minLines = 5,
                maxLines = 6,
                isError = uiState.descriptionError != null,
                supportingText = {
                    Text(uiState.descriptionError ?: "${uiState.description.length} caracteres (minimo 10)")
                },
                shape = RoundedCornerShape(8.dp)
            )

            IncidentInfoBox()
        }
    }
}

@Composable
private fun IncidentActionButtons(
    uiState: IncidentUiState,
    onEvent: (IncidentEvent) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { onEvent(IncidentEvent.SaveIncidentClicked) },
            enabled = uiState.canSubmit,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar")
            }
        }

        Button(
            onClick = { onEvent(IncidentEvent.SendReportClicked) },
            enabled = uiState.canSubmit,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (uiState.sendStatus is IncidentSendStatus.Sending) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar")
            }
        }
    }
}

@Composable
private fun IncidentFormSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun IncidentDropdownField(
    value: String,
    isPlaceholder: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colors = rememberIncidentColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.fieldContainer)
            .border(1.dp, colors.fieldBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Cerrar selector" else "Abrir selector",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> IncidentOptionGrid(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowOptions.forEach { option ->
                    IncidentTypeChip(
                        modifier = Modifier.weight(1f),
                        label = label(option),
                        selected = option == selected,
                        onClick = { onSelected(option) }
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun IncidentTypeChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = rememberIncidentColors()
    val containerColor = if (selected) colors.accent else colors.chipContainer
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) Color.Transparent else colors.chipBorder

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun IncidentInfoBox() {
    val colors = rememberIncidentColors()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.infoContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Este reporte sera enviado a:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "- Coordinacion de Fe y Alegria",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "- Representante del estudiante si tiene chat configurado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "- Archivo historico local del estudiante",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncidentHistoryCard(
    item: IncidentHistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.studentName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (item.incident.sent) "Enviado" else "Pendiente",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.incident.sent) PuceBlue else AlegriWarning
                )
            }
            Text(
                text = "${item.incident.type.label} - ${item.incident.severity.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = humanDateTime(item.incident.dateTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun rememberIncidentColors(): IncidentColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    return remember(colorScheme, isDark) {
        IncidentColors(
            formCardContainer = if (isDark) colorScheme.surface.copy(alpha = 0.96f) else colorScheme.surface,
            formCardBorder = if (isDark) colorScheme.outline.copy(alpha = 0.32f) else colorScheme.surfaceVariant,
            dropdownMenuContainer = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.24f) else colorScheme.surface,
            fieldContainer = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.12f) else colorScheme.surface,
            fieldBorder = if (isDark) colorScheme.outline.copy(alpha = 0.34f) else colorScheme.surfaceVariant,
            fieldBorderFocused = if (isDark) colorScheme.secondary else PuceBlue,
            chipContainer = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.18f) else colorScheme.surface,
            chipBorder = if (isDark) colorScheme.outline.copy(alpha = 0.4f) else colorScheme.surfaceVariant,
            infoContainer = if (isDark) colorScheme.secondaryContainer.copy(alpha = 0.28f) else colorScheme.surfaceVariant.copy(alpha = 0.45f),
            accent = AlegriWarning
        )
    }
}

private data class IncidentColors(
    val formCardContainer: Color,
    val formCardBorder: Color,
    val dropdownMenuContainer: Color,
    val fieldContainer: Color,
    val fieldBorder: Color,
    val fieldBorderFocused: Color,
    val chipContainer: Color,
    val chipBorder: Color,
    val infoContainer: Color,
    val accent: Color
)

private fun humanDateTime(dateTime: String): String {
    val parsed = runCatching { LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
    return parsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: dateTime
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun IncidentScreenPreview() {
    MyApplicationTheme {
        IncidentScreenContent(
            uiState = IncidentUiState(
                students = listOf(
                    Student(1L, "Maria Gonzalez", "7mo", "A", "Representante"),
                    Student(2L, "Juan Perez", "7mo", "A", "Representante")
                ),
                selectedStudentId = 1L,
                selectedType = IncidentType.BEHAVIOR,
                description = "Descripcion preliminar del incidente para vista previa.",
                incidents = listOf(
                    IncidentHistoryItem(
                        incident = Incident(
                            id = 1L,
                            studentId = 1L,
                            type = IncidentType.BEHAVIOR,
                            severity = IncidentSeverity.MEDIUM,
                            description = "Incidente de prueba",
                            dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        ),
                        studentName = "Maria Gonzalez"
                    )
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
