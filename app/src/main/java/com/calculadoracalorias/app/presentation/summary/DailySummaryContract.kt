package com.calculadoracalorias.app.presentation.summary

import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.Supplement
import java.time.LocalDate

data class DailySummaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val targetCalories: Double = 2000.0,
    val consumedCalories: Double = 0.0,
    val remainingCalories: Double = 2000.0,
    val targetProteinGrams: Double = 150.0,
    val consumedProteinGrams: Double = 0.0,
    val targetCarbsGrams: Double = 200.0,
    val consumedCarbsGrams: Double = 0.0,
    val targetFatGrams: Double = 65.0,
    val consumedFatGrams: Double = 0.0,
    val mealsByCategory: Map<MealCategory, List<MealEntry>> = emptyMap(),
    val streak: DailyStreak = DailyStreak(),
    val supplements: List<Supplement> = emptyList(),
    val burnedCalories: Double = 0.0,
    val stepsCount: Long = 0L,
    val isActivitySyncEnabled: Boolean = true,
    val includeBurnedInBudget: Boolean = false,
    val errorMessage: String? = null
)

sealed interface DailySummaryEvent {
    data object OnPreviousDayClicked : DailySummaryEvent
    data object OnNextDayClicked : DailySummaryEvent
    data class OnDateSelected(val date: LocalDate) : DailySummaryEvent
    data class OnAddMealClicked(val category: MealCategory) : DailySummaryEvent
    data class OnMealItemClicked(val mealId: Long) : DailySummaryEvent
    data class OnToggleSupplement(val supplementId: String, val isTaken: Boolean) : DailySummaryEvent
    data object OnRefreshActivity : DailySummaryEvent
}
