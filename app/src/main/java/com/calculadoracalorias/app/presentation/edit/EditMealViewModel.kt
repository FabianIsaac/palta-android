package com.calculadoracalorias.app.presentation.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.usecase.DeleteMealWithHealthSyncUseCase
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.UpdateMealWithHealthSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class EditMealViewModel(
    private val mealRepository: MealRepository,
    private val recalculatePortionUseCase: RecalculatePortionUseCase,
    private val updateMealWithHealthSyncUseCase: UpdateMealWithHealthSyncUseCase,
    private val deleteMealWithHealthSyncUseCase: DeleteMealWithHealthSyncUseCase,
    private val foodCatalogRepository: FoodCatalogRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMealUiState())
    val uiState: StateFlow<EditMealUiState> = _uiState.asStateFlow()

    suspend fun searchCatalog(query: String): List<ScannedFoodItem> {
        return foodCatalogRepository?.searchFood(query) ?: emptyList()
    }

    suspend fun getPopularFoods(): List<ScannedFoodItem> {
        return foodCatalogRepository?.getPopularFoods() ?: emptyList()
    }

    fun loadMeal(mealId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = mealRepository.getMealById(mealId)
            result.fold(
                onSuccess = { meal ->
                    if (meal != null) {
                        _uiState.update {
                            it.copy(
                                mealId = meal.id,
                                category = meal.category,
                                timestamp = meal.timestamp,
                                items = meal.items,
                                summary = meal.summary,
                                healthConnectRecordId = meal.healthConnectRecordId,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "No se encontró el registro de la comida solicitada."
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Error al cargar la comida."
                        )
                    }
                }
            )
        }
    }

    fun onEvent(event: EditMealEvent) {
        when (event) {
            is EditMealEvent.OnServingGramsChanged -> {
                val currentItems = _uiState.value.items.toMutableList()
                if (event.index in currentItems.indices) {
                    val currentItem = currentItems[event.index]
                    val updatedItem = recalculatePortionUseCase(currentItem, event.newGrams)
                    currentItems[event.index] = updatedItem
                    val newSummary = NutritionSummary.fromItems(currentItems)
                    _uiState.update {
                        it.copy(
                            items = currentItems,
                            summary = newSummary,
                            errorMessage = null
                        )
                    }
                }
            }

            is EditMealEvent.OnRemoveItem -> {
                val currentItems = _uiState.value.items.toMutableList()
                if (event.index in currentItems.indices) {
                    currentItems.removeAt(event.index)
                    val newSummary = NutritionSummary.fromItems(currentItems)
                    _uiState.update {
                        it.copy(
                            items = currentItems,
                            summary = newSummary,
                            errorMessage = null
                        )
                    }
                }
            }

            is EditMealEvent.OnItemNameChanged -> {
                val currentItems = _uiState.value.items.toMutableList()
                if (event.index in currentItems.indices) {
                    val trimmed = event.newName.trim()
                    if (trimmed.isNotBlank()) {
                        currentItems[event.index] = currentItems[event.index].copy(name = trimmed)
                        _uiState.update { it.copy(items = currentItems) }
                    }
                }
            }

            is EditMealEvent.OnItemAdded -> {
                val currentItems = _uiState.value.items + event.item
                val newSummary = com.calculadoracalorias.app.domain.model.NutritionSummary.fromItems(currentItems)
                _uiState.update {
                    it.copy(
                        items = currentItems,
                        summary = newSummary,
                        errorMessage = null
                    )
                }
            }

            is EditMealEvent.OnCategoryChanged -> {
                _uiState.update { it.copy(category = event.category) }
            }

            is EditMealEvent.OnDateChanged -> {
                val zoneId = ZoneId.systemDefault()
                val originalTime = try {
                    if (_uiState.value.timestamp > 0L) {
                        Instant.ofEpochMilli(_uiState.value.timestamp).atZone(zoneId).toLocalTime()
                    } else {
                        LocalTime.now()
                    }
                } catch (_: Exception) {
                    LocalTime.now()
                }
                val newTimestamp = event.newDate
                    .atTime(originalTime)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()

                _uiState.update { it.copy(timestamp = newTimestamp) }
            }

            EditMealEvent.OnSaveMealClicked -> {
                saveChanges()
            }

            EditMealEvent.OnDeleteMealClicked -> {
                _uiState.update { it.copy(showDeleteConfirmation = true) }
            }

            EditMealEvent.OnConfirmDeleteClicked -> {
                deleteMeal()
            }

            EditMealEvent.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
            }

            EditMealEvent.OnDismiss -> {
                // Atendido por la navegación
            }
        }
    }

    private fun saveChanges() {
        val state = _uiState.value
        if (state.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Agrega al menos un alimento para poder guardar tu comida.") }
            return
        }

        if (state.items.any { it.servingGrams <= 0.0 }) {
            _uiState.update { it.copy(errorMessage = "Las porciones de los alimentos deben ser mayores a 0 gramos.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = updateMealWithHealthSyncUseCase(
                mealId = state.mealId,
                category = state.category,
                timestamp = state.timestamp,
                items = state.items,
                existingHealthConnectRecordId = state.healthConnectRecordId
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSavedSuccessfully = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Error al guardar los cambios."
                        )
                    }
                }
            )
        }
    }

    private fun deleteMeal() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false, errorMessage = null) }
            val result = deleteMealWithHealthSyncUseCase(state.mealId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            isDeletedSuccessfully = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = error.message ?: "Error al eliminar la comida."
                        )
                    }
                }
            )
        }
    }
}
