package com.calculadoracalorias.app.presentation.edit

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import java.time.LocalDate

data class EditMealUiState(
    val mealId: Long = 0L,
    val category: MealCategory = MealCategory.DESAYUNO,
    val timestamp: Long = 0L,
    val items: List<ScannedFoodItem> = emptyList(),
    val summary: NutritionSummary = NutritionSummary(),
    val healthConnectRecordId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isSavedSuccessfully: Boolean = false,
    val isDeletedSuccessfully: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null
)

sealed interface EditMealEvent {
    data class OnServingGramsChanged(val index: Int, val newGrams: Double) : EditMealEvent
    data class OnRemoveItem(val index: Int) : EditMealEvent
    data class OnItemNameChanged(val index: Int, val newName: String) : EditMealEvent
    data class OnItemAdded(val item: ScannedFoodItem) : EditMealEvent
    data class OnCategoryChanged(val category: MealCategory) : EditMealEvent
    data class OnDateChanged(val newDate: LocalDate) : EditMealEvent
    data object OnSaveMealClicked : EditMealEvent
    data object OnDeleteMealClicked : EditMealEvent
    data object OnConfirmDeleteClicked : EditMealEvent
    data object OnDismissDeleteDialog : EditMealEvent
    data object OnDismiss : EditMealEvent
}
