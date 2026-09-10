package com.calculadoracalorias.app.presentation.scan

import com.calculadoracalorias.app.domain.model.AiTechnicalDetails
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import java.time.LocalDate

/**
 * Eventos unidireccionales emitidos desde la interfaz de usuario hacia el ViewModel.
 */
sealed interface FoodScanReviewEvent {
    data class OnPortionChanged(val itemId: String, val newGrams: Double) : FoodScanReviewEvent
    data class OnItemRemoved(val itemId: String) : FoodScanReviewEvent
    data class OnItemAdded(val item: ScannedFoodItem) : FoodScanReviewEvent
    data class OnItemNameChanged(val itemId: String, val newName: String) : FoodScanReviewEvent
    data class OnMealCategoryChanged(val category: MealCategory) : FoodScanReviewEvent
    data class OnTargetDateChanged(val date: LocalDate) : FoodScanReviewEvent
    data class OnAnalyzeNaturalLanguage(val description: String, val category: MealCategory? = null, val targetDate: LocalDate? = null) : FoodScanReviewEvent
    data object OnRetryNaturalLanguage : FoodScanReviewEvent
    data object OnConfirmAndSave : FoodScanReviewEvent
    data object OnSavePendingAiRefinement : FoodScanReviewEvent
    data object OnDismissError : FoodScanReviewEvent
    data object OnDismissSuccess : FoodScanReviewEvent
    data object OnResetState : FoodScanReviewEvent
}

/**
 * Estado inmutable completo de la pantalla de revisión y ajuste interactivo de comida.
 */
data class FoodScanReviewUiState(
    val isLoading: Boolean = false,
    val isAnalyzingText: Boolean = false,
    val imagePath: String? = null,
    val items: List<ScannedFoodItem> = emptyList(),
    val selectedCategory: MealCategory = MealCategory.ALMUERZO,
    val targetDate: LocalDate = LocalDate.now(),
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val syncedWithHealthConnect: Boolean = false,
    val errorMessage: String? = null,
    val consolidateTargetMealMinutesAgo: Int? = null,
    val lastRawDescription: String? = null,
    val isPendingAiRefinement: Boolean = false,
    val canRetryTextAnalysis: Boolean = false,
    val lastTechnicalError: AiTechnicalDetails? = null
)
