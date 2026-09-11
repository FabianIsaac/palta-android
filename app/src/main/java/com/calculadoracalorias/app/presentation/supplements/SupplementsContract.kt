package com.calculadoracalorias.app.presentation.supplements

import com.calculadoracalorias.app.domain.model.Supplement

data class SupplementsUiState(
    val supplements: List<Supplement> = emptyList(),
    val inputName: String = "",
    val inputDosage: String = "",
    val inputCalories: String = "",
    val inputProtein: String = "",
    val inputCarbs: String = "",
    val inputFat: String = "",
    val isEstimating: Boolean = false,
    val isScanningLabel: Boolean = false,
    val areSuggestionsExpanded: Boolean = false,
    val intakeCounts: Map<String, Int> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed interface SupplementsEvent {
    data class OnInputNameChanged(val value: String) : SupplementsEvent
    data class OnInputDosageChanged(val value: String) : SupplementsEvent
    data class OnInputCaloriesChanged(val value: String) : SupplementsEvent
    data class OnInputProteinChanged(val value: String) : SupplementsEvent
    data class OnInputCarbsChanged(val value: String) : SupplementsEvent
    data class OnInputFatChanged(val value: String) : SupplementsEvent
    data object OnEstimateClicked : SupplementsEvent
    data object OnToggleSuggestionsExpanded : SupplementsEvent
    data class OnScanImageSelected(val imageBytes: ByteArray) : SupplementsEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as OnScanImageSelected
            return imageBytes.contentEquals(other.imageBytes)
        }
        override fun hashCode(): Int = imageBytes.contentHashCode()
    }
    data object OnSaveClicked : SupplementsEvent
    data class OnToggleActive(val supplementId: String, val isActive: Boolean) : SupplementsEvent
    data class OnDeleteSupplement(val supplementId: String) : SupplementsEvent
    data class OnToggleSuggestion(val suggestion: Supplement) : SupplementsEvent
    data object OnDismissMessage : SupplementsEvent
}

sealed interface SupplementsEffect {
    data class ShowSnackbar(val message: String) : SupplementsEffect
}
