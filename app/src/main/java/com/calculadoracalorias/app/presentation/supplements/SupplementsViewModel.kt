package com.calculadoracalorias.app.presentation.supplements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import com.calculadoracalorias.app.domain.usecase.EstimateSupplementNutritionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SupplementsViewModel(
    private val supplementRepository: SupplementRepository,
    private val estimateSupplementNutritionUseCase: EstimateSupplementNutritionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplementsUiState())
    val uiState: StateFlow<SupplementsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SupplementsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeSupplements()
    }

    private fun observeSupplements() {
        supplementRepository.getAllSupplements()
            .onEach { supplementsList ->
                _uiState.update { current ->
                    current.copy(supplements = supplementsList)
                }
            }
            .catch { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: "Error al cargar la lista de suplementos.")
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SupplementsEvent) {
        when (event) {
            is SupplementsEvent.OnInputNameChanged -> {
                _uiState.update { it.copy(inputName = event.value, errorMessage = null) }
            }
            is SupplementsEvent.OnInputDosageChanged -> {
                _uiState.update { it.copy(inputDosage = event.value) }
            }
            is SupplementsEvent.OnInputCaloriesChanged -> {
                _uiState.update { it.copy(inputCalories = event.value) }
            }
            is SupplementsEvent.OnInputProteinChanged -> {
                _uiState.update { it.copy(inputProtein = event.value) }
            }
            is SupplementsEvent.OnInputCarbsChanged -> {
                _uiState.update { it.copy(inputCarbs = event.value) }
            }
            is SupplementsEvent.OnInputFatChanged -> {
                _uiState.update { it.copy(inputFat = event.value) }
            }
            SupplementsEvent.OnEstimateClicked -> {
                estimateNutrition()
            }
            SupplementsEvent.OnSaveClicked -> {
                saveSupplement()
            }
            is SupplementsEvent.OnToggleActive -> {
                viewModelScope.launch {
                    val result = supplementRepository.toggleSupplementActive(event.supplementId, event.isActive)
                    if (result.isFailure) {
                        _uiState.update { it.copy(errorMessage = "Error al actualizar el estado del suplemento.") }
                    }
                }
            }
            is SupplementsEvent.OnDeleteSupplement -> {
                viewModelScope.launch {
                    val result = supplementRepository.deleteSupplement(event.supplementId)
                    if (result.isFailure) {
                        _uiState.update { it.copy(errorMessage = "Error al eliminar el suplemento.") }
                    }
                }
            }
            is SupplementsEvent.OnToggleSuggestion -> {
                toggleSuggestion(event.suggestion)
            }
            SupplementsEvent.OnDismissMessage -> {
                _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun estimateNutrition() {
        val query = _uiState.value.inputName.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa el nombre de un suplemento para estimar.") }
            return
        }

        _uiState.update { it.copy(isEstimating = true, errorMessage = null) }
        viewModelScope.launch {
            val result = estimateSupplementNutritionUseCase(query)
            if (result.isSuccess) {
                val estimate = result.getOrThrow()
                _uiState.update { current ->
                    current.copy(
                        isEstimating = false,
                        inputDosage = estimate.dosageDescription,
                        inputCalories = if (estimate.calories == 0.0) "0" else estimate.calories.toString(),
                        inputProtein = if (estimate.proteinGrams == 0.0) "0" else estimate.proteinGrams.toString(),
                        inputCarbs = if (estimate.carbsGrams == 0.0) "0" else estimate.carbsGrams.toString(),
                        inputFat = if (estimate.fatGrams == 0.0) "0" else estimate.fatGrams.toString()
                    )
                }
            } else {
                _uiState.update { current ->
                    current.copy(
                        isEstimating = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "No se pudo estimar la información nutricional."
                    )
                }
            }
        }
    }

    private fun saveSupplement() {
        val name = _uiState.value.inputName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes ingresar un nombre para el suplemento.") }
            return
        }

        val dosage = _uiState.value.inputDosage.trim().ifBlank { "1 porción" }
        val calories = _uiState.value.inputCalories.toDoubleOrNull() ?: 0.0
        val protein = _uiState.value.inputProtein.toDoubleOrNull() ?: 0.0
        val carbs = _uiState.value.inputCarbs.toDoubleOrNull() ?: 0.0
        val fat = _uiState.value.inputFat.toDoubleOrNull() ?: 0.0

        val newSupplement = Supplement(
            id = "custom_${UUID.randomUUID()}",
            name = name,
            dosageDescription = dosage,
            calories = calories,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            isActive = true,
            isCustom = true
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val result = supplementRepository.saveSupplement(newSupplement)
            if (result.isSuccess) {
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        inputName = "",
                        inputDosage = "",
                        inputCalories = "",
                        inputProtein = "",
                        inputCarbs = "",
                        inputFat = "",
                        successMessage = "Suplemento guardado con éxito."
                    )
                }
            } else {
                _uiState.update { current ->
                    current.copy(
                        isSaving = false,
                        errorMessage = "Error al guardar el suplemento."
                    )
                }
            }
        }
    }

    private fun toggleSuggestion(suggestion: Supplement) {
        viewModelScope.launch {
            val existing = _uiState.value.supplements.find { it.id == suggestion.id }
            if (existing != null) {
                supplementRepository.toggleSupplementActive(existing.id, !existing.isActive)
            } else {
                supplementRepository.saveSupplement(suggestion.copy(isActive = true))
            }
        }
    }
}
