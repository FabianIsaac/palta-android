package com.calculadoracalorias.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.model.StatisticsRange
import com.calculadoracalorias.app.domain.usecase.GetMonthlyCalendarHabitsUseCase
import com.calculadoracalorias.app.domain.usecase.GetPeriodStatisticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val getPeriodStatisticsUseCase: GetPeriodStatisticsUseCase,
    private val getMonthlyCalendarHabitsUseCase: GetMonthlyCalendarHabitsUseCase,
    initialRange: StatisticsRange = StatisticsRange.LAST_7_DAYS,
    initialYearMonth: YearMonth = YearMonth.now(),
    initialCalendarDate: LocalDate = LocalDate.now()
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(initialRange)
    private val _calendarYearMonth = MutableStateFlow(initialYearMonth)
    private val _uiState = MutableStateFlow(
        StatisticsUiState(
            selectedRange = initialRange,
            calendarYearMonth = initialYearMonth,
            selectedCalendarDate = initialCalendarDate
        )
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        observePeriodStatistics()
        observeMonthlyCalendarHabits()
    }

    private fun observePeriodStatistics() {
        _selectedRange
            .flatMapLatest { range ->
                _uiState.update { it.copy(isLoading = true, selectedRange = range, errorMessage = null) }
                getPeriodStatisticsUseCase(range)
            }
            .onEach { stats ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statistics = stats,
                        errorMessage = null
                    )
                }
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al cargar las estadísticas del periodo."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMonthlyCalendarHabits() {
        _calendarYearMonth
            .flatMapLatest { yearMonth ->
                getMonthlyCalendarHabitsUseCase(yearMonth)
            }
            .onEach { days ->
                _uiState.update {
                    it.copy(
                        calendarYearMonth = _calendarYearMonth.value,
                        calendarDays = days
                    )
                }
            }
            .catch { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Error al cargar el calendario de hábitos."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            is StatisticsEvent.OnRangeSelected -> {
                _selectedRange.value = event.range
                _uiState.update { it.copy(selectedRange = event.range, isLoading = true) }
            }

            is StatisticsEvent.OnOpenCalendarClicked -> {
                val targetDate = event.initialDate ?: _uiState.value.selectedCalendarDate
                val targetYearMonth = YearMonth.from(targetDate)
                _calendarYearMonth.value = targetYearMonth
                _uiState.update {
                    it.copy(
                        isCalendarOpen = true,
                        calendarYearMonth = targetYearMonth,
                        selectedCalendarDate = targetDate
                    )
                }
            }

            is StatisticsEvent.OnCloseCalendarClicked -> {
                _uiState.update { it.copy(isCalendarOpen = false) }
            }

            is StatisticsEvent.OnPreviousMonthClicked -> {
                val prev = _calendarYearMonth.value.minusMonths(1)
                _calendarYearMonth.value = prev
                _uiState.update { it.copy(calendarYearMonth = prev) }
            }

            is StatisticsEvent.OnNextMonthClicked -> {
                val next = _calendarYearMonth.value.plusMonths(1)
                _calendarYearMonth.value = next
                _uiState.update { it.copy(calendarYearMonth = next) }
            }

            is StatisticsEvent.OnCalendarDateSelected -> {
                _uiState.update { it.copy(selectedCalendarDate = event.date) }
            }
        }
    }
}
