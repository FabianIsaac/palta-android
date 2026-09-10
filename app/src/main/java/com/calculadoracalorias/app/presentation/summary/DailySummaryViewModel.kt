package com.calculadoracalorias.app.presentation.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.data.preferences.UserPreferences
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.domain.model.DailyHealthActivity
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import com.calculadoracalorias.app.domain.usecase.CalculateDailyStreakUseCase
import com.calculadoracalorias.app.domain.usecase.GetDailyHealthActivityUseCase
import com.calculadoracalorias.app.domain.usecase.GetDailyMealSummaryUseCase
import com.calculadoracalorias.app.domain.usecase.RefinePendingMealsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private data class DailyCombinedData(
    val summary: com.calculadoracalorias.app.domain.model.DailySummary,
    val supplements: List<com.calculadoracalorias.app.domain.model.Supplement>,
    val streak: com.calculadoracalorias.app.domain.model.DailyStreak,
    val preferences: UserPreferences,
    val activity: DailyHealthActivity
)

@OptIn(ExperimentalCoroutinesApi::class)
class DailySummaryViewModel(
    private val getDailyMealSummaryUseCase: GetDailyMealSummaryUseCase,
    private val calculateDailyStreakUseCase: CalculateDailyStreakUseCase,
    private val supplementRepository: SupplementRepository,
    private val getDailyHealthActivityUseCase: GetDailyHealthActivityUseCase? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
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
        val prefsFlow = userPreferencesRepository?.userPreferencesFlow
            ?: flowOf(UserPreferences())

        _selectedDate
            .flatMapLatest { date ->
                _uiState.update { it.copy(isLoading = true, selectedDate = date, errorMessage = null) }
                combine(
                    getDailyMealSummaryUseCase(date),
                    supplementRepository.getSupplementsForDate(date),
                    calculateDailyStreakUseCase(),
                    prefsFlow
                ) { summary, supplements, streak, preferences ->
                    val activity = getDailyHealthActivityUseCase?.invoke(date, preferences.healthConnectActivitySyncEnabled)
                        ?: DailyHealthActivity(date = date)
                    DailyCombinedData(summary, supplements, streak, preferences, activity)
                }
            }
            .onEach { combined ->
                val summary = combined.summary
                val supplements = combined.supplements
                val streak = combined.streak
                val preferences = combined.preferences
                val activity = combined.activity

                val takenSupplements = supplements.filter { it.isTakenToday }
                val supplementsCalories = takenSupplements.sumOf { it.calories }
                val supplementsProtein = takenSupplements.sumOf { it.proteinGrams }
                val supplementsCarbs = takenSupplements.sumOf { it.carbsGrams }
                val supplementsFat = takenSupplements.sumOf { it.fatGrams }

                val totalConsumedCalories = round(summary.consumed.totalCalories + supplementsCalories, 1)
                val totalConsumedProtein = round(summary.consumed.totalProtein + supplementsProtein, 2)
                val totalConsumedCarbs = round(summary.consumed.totalCarbs + supplementsCarbs, 2)
                val totalConsumedFat = round(summary.consumed.totalFat + supplementsFat, 2)

                val baseRemaining = summary.budget.targetCalories - totalConsumedCalories
                val remainingCalories = if (preferences.includeBurnedCaloriesInBudget) {
                    round(baseRemaining + activity.burnedCalories, 1)
                } else {
                    round(baseRemaining, 1)
                }

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
                        burnedCalories = round(activity.burnedCalories, 1),
                        stepsCount = activity.stepsCount,
                        isActivitySyncEnabled = preferences.healthConnectActivitySyncEnabled,
                        includeBurnedInBudget = preferences.includeBurnedCaloriesInBudget,
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
            DailySummaryEvent.OnRefreshActivity -> {
                refreshHealthActivity()
            }
            is DailySummaryEvent.OnAddMealClicked -> {
                // Evento de navegación atendido por la UI
            }
            is DailySummaryEvent.OnMealItemClicked -> {
                // Evento de navegación atendido por la UI
            }
        }
    }

    fun refreshHealthActivity() {
        val useCase = getDailyHealthActivityUseCase ?: return
        val prefsRepo = userPreferencesRepository
        viewModelScope.launch {
            val prefs = prefsRepo?.userPreferencesFlow?.firstOrNull() ?: UserPreferences()
            val activity = useCase(_selectedDate.value, prefs.healthConnectActivitySyncEnabled)
            _uiState.update { current ->
                val baseRemaining = current.targetCalories - current.consumedCalories
                val remaining = if (prefs.includeBurnedCaloriesInBudget) {
                    round(baseRemaining + activity.burnedCalories, 1)
                } else {
                    round(baseRemaining, 1)
                }
                current.copy(
                    burnedCalories = round(activity.burnedCalories, 1),
                    stepsCount = activity.stepsCount,
                    isActivitySyncEnabled = prefs.healthConnectActivitySyncEnabled,
                    includeBurnedInBudget = prefs.includeBurnedCaloriesInBudget,
                    remainingCalories = remaining
                )
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
