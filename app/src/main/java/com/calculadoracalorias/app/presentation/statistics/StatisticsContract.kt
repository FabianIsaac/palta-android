package com.calculadoracalorias.app.presentation.statistics

import com.calculadoracalorias.app.domain.model.DayHabitSummary
import com.calculadoracalorias.app.domain.model.PeriodStatistics
import com.calculadoracalorias.app.domain.model.StatisticsRange
import java.time.LocalDate
import java.time.YearMonth

/**
 * Estado inmutable de la pantalla de estadísticas y calendario mensual.
 */
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val selectedRange: StatisticsRange = StatisticsRange.LAST_7_DAYS,
    val statistics: PeriodStatistics? = null,
    val isCalendarOpen: Boolean = false,
    val calendarYearMonth: YearMonth = YearMonth.now(),
    val calendarDays: List<DayHabitSummary> = emptyList(),
    val selectedCalendarDate: LocalDate = LocalDate.now(),
    val errorMessage: String? = null
)

/**
 * Eventos unidireccionales de la pantalla de estadísticas.
 */
sealed interface StatisticsEvent {
    data class OnRangeSelected(val range: StatisticsRange) : StatisticsEvent
    data class OnOpenCalendarClicked(val initialDate: LocalDate? = null) : StatisticsEvent
    data object OnCloseCalendarClicked : StatisticsEvent
    data object OnPreviousMonthClicked : StatisticsEvent
    data object OnNextMonthClicked : StatisticsEvent
    data class OnCalendarDateSelected(val date: LocalDate) : StatisticsEvent
}
