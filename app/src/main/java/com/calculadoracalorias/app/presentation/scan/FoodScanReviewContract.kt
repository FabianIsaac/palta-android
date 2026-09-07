package com.calculadoracalorias.app.presentation.scan

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem

/**
 * Eventos unidireccionales emitidos desde la interfaz de usuario hacia el ViewModel.
 */
sealed interface FoodScanReviewEvent {
    data class OnPortionChanged(val itemId: String, val newGrams: Double) : FoodScanReviewEvent
    data class OnItemRemoved(val itemId: String) : FoodScanReviewEvent
    data class OnItemAdded(val item: ScannedFoodItem) : FoodScanReviewEvent
    data class OnMealCategoryChanged(val category: MealCategory) : FoodScanReviewEvent
    data object OnConfirmAndSave : FoodScanReviewEvent
    data object OnDismissError : FoodScanReviewEvent
    data object OnDismissSuccess : FoodScanReviewEvent
}

/**
 * Estado inmutable completo de la pantalla de revisión y ajuste interactivo de comida.
 */
data class FoodScanReviewUiState(
    val isLoading: Boolean = false,
    val imagePath: String? = null,
    val items: List<ScannedFoodItem> = emptyList(),
    val selectedCategory: MealCategory = MealCategory.ALMUERZO,
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val syncedWithHealthConnect: Boolean = false,
    val errorMessage: String? = null
)
