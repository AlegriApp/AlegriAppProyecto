package com.example.myapplication.presentation.grades.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.presentation.common.CatalogDropdown
import com.example.myapplication.presentation.common.CatalogOption
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun GradeFilterSection(
    courseOptions: List<CatalogOption>,
    selectedCourseId: Long?,
    onCourseSelected: (Long) -> Unit,
    subjectOptions: List<CatalogOption>,
    selectedSubjectId: Long?,
    onSubjectSelected: (Long) -> Unit,
    evaluationTypeOptions: List<CatalogOption>,
    selectedEvaluationTypeId: Long?,
    onEvaluationTypeSelected: (Long) -> Unit,
    periodOptions: List<CatalogOption>,
    selectedPeriodId: Long?,
    onPeriodSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        CatalogDropdown(
            label = stringResource(R.string.grade_filter_course),
            options = courseOptions,
            selectedId = selectedCourseId,
            onSelected = onCourseSelected
        )
        CatalogDropdown(
            label = stringResource(R.string.grade_filter_subject),
            options = subjectOptions,
            selectedId = selectedSubjectId,
            onSelected = onSubjectSelected,
            enabled = subjectOptions.isNotEmpty()
        )
        CatalogDropdown(
            label = stringResource(R.string.grade_filter_evaluation_type),
            options = evaluationTypeOptions,
            selectedId = selectedEvaluationTypeId,
            onSelected = onEvaluationTypeSelected,
            enabled = evaluationTypeOptions.isNotEmpty()
        )
        CatalogDropdown(
            label = stringResource(R.string.grade_filter_period),
            options = periodOptions,
            selectedId = selectedPeriodId,
            onSelected = onPeriodSelected,
            enabled = periodOptions.isNotEmpty()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeFilterSectionPreview() {
    MyApplicationTheme {
        GradeFilterSection(
            courseOptions = listOf(CatalogOption(1L, "5to A")),
            selectedCourseId = 1L,
            onCourseSelected = {},
            subjectOptions = listOf(CatalogOption(1L, "Matemáticas")),
            selectedSubjectId = 1L,
            onSubjectSelected = {},
            evaluationTypeOptions = listOf(CatalogOption(6L, "Parcial")),
            selectedEvaluationTypeId = 6L,
            onEvaluationTypeSelected = {},
            periodOptions = listOf(CatalogOption(1L, "2025-2026")),
            selectedPeriodId = 1L,
            onPeriodSelected = {}
        )
    }
}
