package com.example.myapplication.domain.usecase.incidents

import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.repository.IncidentRepository

class SaveIncidentUseCase(
    private val repository: IncidentRepository
) {
    suspend operator fun invoke(incident: Incident): Long = repository.saveIncident(incident)
}
