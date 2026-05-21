package com.example.myapplication.presentation.incidents

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AlegriWarning
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PuceBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentScreen(
    onBack: () -> Unit = {}
) {
    val initialState = rememberSaveable(saver = IncidentUiState.Saver) {
        IncidentUiState()
    }
    var selectedStudentId by rememberSaveable { mutableStateOf(initialState.selectedStudentId) }
    var selectedType by rememberSaveable { mutableStateOf(initialState.selectedType) }
    var description by rememberSaveable { mutableStateOf(initialState.description) }
    var isStudentMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val selectedStudent = initialState.students.firstOrNull { it.id == selectedStudentId }
    val descriptionCount = description.length
    val colors = rememberIncidentColors()

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
                                    text = initialState.screenTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Text(
                                text = initialState.screenDescription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = colors.formCardContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, colors.formCardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                IncidentFormSectionTitle("Estudiante Involucrado")

                                IncidentDropdownField(
                                    value = selectedStudent?.name ?: "Seleccionar estudiante...",
                                    isPlaceholder = selectedStudent == null,
                                    expanded = isStudentMenuExpanded,
                                    onClick = { isStudentMenuExpanded = !isStudentMenuExpanded }
                                )

                                if (isStudentMenuExpanded) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = colors.dropdownMenuContainer
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            colors.formCardBorder
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    ) {
                                        Column {
                                            initialState.students.forEach { student ->
                                                Text(
                                                    text = student.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedStudentId = student.id
                                                            isStudentMenuExpanded = false
                                                        }
                                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                IncidentFormSectionTitle("Tipo de Incidente")

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        IncidentTypeChip(
                                            modifier = Modifier.weight(1f),
                                            label = IncidentTypeOption.BEHAVIOR.label,
                                            selected = selectedType == IncidentTypeOption.BEHAVIOR,
                                            onClick = { selectedType = IncidentTypeOption.BEHAVIOR }
                                        )
                                        IncidentTypeChip(
                                            modifier = Modifier.weight(1f),
                                            label = IncidentTypeOption.ACADEMIC.label,
                                            selected = selectedType == IncidentTypeOption.ACADEMIC,
                                            onClick = { selectedType = IncidentTypeOption.ACADEMIC }
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        IncidentTypeChip(
                                            modifier = Modifier.weight(1f),
                                            label = IncidentTypeOption.HEALTH.label,
                                            selected = selectedType == IncidentTypeOption.HEALTH,
                                            onClick = { selectedType = IncidentTypeOption.HEALTH }
                                        )
                                        IncidentTypeChip(
                                            modifier = Modifier.weight(1f),
                                            label = IncidentTypeOption.OTHER.label,
                                            selected = selectedType == IncidentTypeOption.OTHER,
                                            onClick = { selectedType = IncidentTypeOption.OTHER }
                                        )
                                    }
                                }

                                IncidentFormSectionTitle("Descripción del Incidente")

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    placeholder = {
                                        Text("Describe detalladamente lo ocurrido...")
                                    },
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
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Text(
                                    text = "$descriptionCount caracteres (mínimo 10)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                IncidentInfoBox()
                            }
                        }

                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = colors.disabledButtonContainer,
                                disabledContentColor = colors.disabledButtonContent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enviar Reporte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "AlegriApp • Comunicación garantizada vía Telegram",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
            .clip(RoundedCornerShape(16.dp))
            .background(colors.fieldContainer)
            .border(
                width = 1.dp,
                color = colors.fieldBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPlaceholder) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
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
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun IncidentInfoBox() {
    val colors = rememberIncidentColors()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.infoContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Este reporte será enviado a:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• Coordinación de Fe y Alegría",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• Representante del estudiante",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• Archivo histórico del estudiante",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
            disabledButtonContainer = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.28f) else colorScheme.surfaceVariant.copy(alpha = 0.55f),
            disabledButtonContent = if (isDark) colorScheme.onSurface.copy(alpha = 0.62f) else colorScheme.onSurfaceVariant,
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
    val disabledButtonContainer: Color,
    val disabledButtonContent: Color,
    val accent: Color
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun IncidentScreenPreview() {
    MyApplicationTheme {
        IncidentScreen()
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = 0x20)
@Composable
private fun IncidentScreenDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        IncidentScreen()
    }
}
