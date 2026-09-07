package com.calculadoracalorias.app.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FoodScanReviewViewModel(
    private val recalculatePortionUseCase: RecalculatePortionUseCase = RecalculatePortionUseCase(),
    private val saveMealWithHealthSyncUseCase: SaveMealWithHealthSyncUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodScanReviewUiState())
    val uiState: StateFlow<FoodScanReviewUiState> = _uiState.asStateFlow()

    fun initializeWithResult(result: DetectedMealResult, imagePath: String? = null) {
        val summary = NutritionSummary.fromItems(result.items)
        _uiState.update {
            it.copy(
                isLoading = false,
                imagePath = imagePath,
                items = result.items,
                selectedCategory = result.suggestedMealType,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat,
                errorMessage = null
            )
        }
    }

    fun onEvent(event: FoodScanReviewEvent) {
        when (event) {
            is FoodScanReviewEvent.OnPortionChanged -> handlePortionChanged(event.itemId, event.newGrams)
            is FoodScanReviewEvent.OnItemRemoved -> handleItemRemoved(event.itemId)
            is FoodScanReviewEvent.OnItemAdded -> handleItemAdded(event.item)
            is FoodScanReviewEvent.OnMealCategoryChanged -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
            }
            FoodScanReviewEvent.OnConfirmAndSave -> handleConfirmAndSave()
            FoodScanReviewEvent.OnDismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            FoodScanReviewEvent.OnDismissSuccess -> {
                _uiState.update { it.copy(saveSuccess = false) }
            }
        }
    }

    private fun handlePortionChanged(itemId: String, newGrams: Double) {
        val currentItems = _uiState.value.items
        val updatedItems = recalculatePortionUseCase(currentItems, itemId, newGrams)
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleItemRemoved(itemId: String) {
        val updatedItems = _uiState.value.items.filterNot { it.id == itemId }
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleItemAdded(item: ScannedFoodItem) {
        val updatedItems = _uiState.value.items + item
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleConfirmAndSave() {
        val currentState = _uiState.value
        if (currentState.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Agrega al menos un alimento para poder guardar tu comida.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val result = saveMealWithHealthSyncUseCase(
                mealName = currentState.selectedCategory.displayName,
                category = currentState.selectedCategory,
                timestamp = System.currentTimeMillis(),
                items = currentState.items
            )

            result.fold(
                onSuccess = { saveResult ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            syncedWithHealthConnect = saveResult.syncedWithHealthConnect
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Ocurrió un error al guardar la comida."
                        )
                    }
                }
            )
        }
    }
}
