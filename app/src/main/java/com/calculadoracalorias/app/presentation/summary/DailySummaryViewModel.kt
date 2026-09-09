package com.calculadoracalorias.app.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import com.calculadoracalorias.app.domain.usecase.CalculateDailyStreakUseCase
import com.calculadoracalorias.app.domain.usecase.GetDailyMealSummaryUseCase
import com.calculadoracalorias.app.domain.usecase.RefinePendingMealsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailySummaryViewModel(
    private val getDailyMealSummaryUseCase: GetDailyMealSummaryUseCase,
    private val calculateDailyStreakUseCase: CalculateDailyStreakUseCase,
    private val supplementRepository: SupplementRepository,
    private val refinePendingMealsUseCase: RefinePendingMealsUseCase? = null,
    initialDate: LocalDate = LocalDate.now()
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(initialDate)
    private val _uiState = MutableStateFlow(DailySummaryUiState(selectedDate = initialDate))
    val uiState: StateFlow<DailySummaryUiState> = _uiState.asStateFlow()

    init {
        observeDailySummary()
        triggerPendingMealsRefinement()
    }

    private fun observeDailySummary() {
        _selectedDate
            .flatMapLatest { date ->
                _uiState.update { it.copy(isLoading = true, selectedDate = date, errorMessage = null) }
                combine(
                    getDailyMealSummaryUseCase(date),
                    supplementRepository.getSupplementsForDate(date),
                    calculateDailyStreakUseCase()
                ) { summary, supplements, streak ->
                    Triple(summary, supplements, streak)
                }
            }
            .onEach { (summary, supplements, streak) ->
                val takenSupplements = supplements.filter { it.isTakenToday }
                val supplementsCalories = takenSupplements.sumOf { it.calories }
                val supplementsProtein = takenSupplements.sumOf { it.proteinGrams }
                val supplementsCarbs = takenSupplements.sumOf { it.carbsGrams }
                val supplementsFat = takenSupplements.sumOf { it.fatGrams }

                val totalConsumedCalories = round(summary.consumed.totalCalories + supplementsCalories, 1)
                val totalConsumedProtein = round(summary.consumed.totalProtein + supplementsProtein, 2)
                val totalConsumedCarbs = round(summary.consumed.totalCarbs + supplementsCarbs, 2)
                val totalConsumedFat = round(summary.consumed.totalFat + supplementsFat, 2)
                val remainingCalories = round(summary.budget.targetCalories - totalConsumedCalories, 1)

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        targetCalories = summary.budget.targetCalories,
                        consumedCalories = totalConsumedCalories,
                        remainingCalories = remainingCalories,
                        targetProteinGrams = summary.budget.targetProteinGrams,
                        consumedProteinGrams = totalConsumedProtein,
                        targetCarbsGrams = summary.budget.targetCarbsGrams,
                        consumedCarbsGrams = totalConsumedCarbs,
                        targetFatGrams = summary.budget.targetFatGrams,
                        consumedFatGrams = totalConsumedFat,
                        mealsByCategory = summary.mealsByCategory,
                        streak = streak,
                        supplements = supplements,
                        errorMessage = null
                    )
                }
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al cargar el resumen diario."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DailySummaryEvent) {
        when (event) {
            DailySummaryEvent.OnPreviousDayClicked -> {
                _selectedDate.update { it.minusDays(1) }
            }
            DailySummaryEvent.OnNextDayClicked -> {
                _selectedDate.update { it.plusDays(1) }
            }
            is DailySummaryEvent.OnDateSelected -> {
                _selectedDate.value = event.date
            }
            is DailySummaryEvent.OnToggleSupplement -> {
                viewModelScope.launch {
                    supplementRepository.toggleSupplementTaken(
                        date = _selectedDate.value,
                        supplementId = event.supplementId,
                        isTaken = event.isTaken
                    )
                }
            }
            is DailySummaryEvent.OnAddMealClicked -> {
                // Evento de navegación atendido por la UI
            }
            is DailySummaryEvent.OnMealItemClicked -> {
                // Evento de navegación atendido por la UI
            }
        }
    }

    fun triggerPendingMealsRefinement() {
        val useCase = refinePendingMealsUseCase ?: return
        viewModelScope.launch {
            try {
                useCase()
            } catch (_: Exception) {
                // Silencioso ante fallos temporales de red
            }
        }
    }

    private fun round(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
