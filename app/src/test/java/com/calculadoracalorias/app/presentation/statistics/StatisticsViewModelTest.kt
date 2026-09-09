package com.calculadoracalorias.app.presentation.statistics

import com.calculadoracalorias.app.domain.model.DayHabitSummary
import com.calculadoracalorias.app.domain.model.PeriodStatistics
import com.calculadoracalorias.app.domain.model.StatisticsRange
import com.calculadoracalorias.app.domain.usecase.GetMonthlyCalendarHabitsUseCase
import com.calculadoracalorias.app.domain.usecase.GetPeriodStatisticsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getPeriodStatisticsUseCase: GetPeriodStatisticsUseCase = mockk()
    private val getMonthlyCalendarHabitsUseCase: GetMonthlyCalendarHabitsUseCase = mockk()

    private lateinit var viewModel: StatisticsViewModel
    private val initialDate = LocalDate.of(2026, 9, 8)
    private val initialYearMonth = YearMonth.of(2026, 9)

    private val mockStats7 = PeriodStatistics(
        range = StatisticsRange.LAST_7_DAYS,
        startDate = initialDate.minusDays(6),
        endDate = initialDate,
        averageDailyCalories = 2100.0,
        targetDailyCalories = 2000.0,
        averageProteinGrams = 150.0,
        averageCarbsGrams = 190.0,
        averageFatGrams = 60.0,
        habitCompletedDaysCount = 5,
        totalPeriodDays = 7,
        currentStreakDays = 3,
        supplementAdherencePercentage = 85.0,
        dailyCalorieMetrics = emptyList()
    )

    private val mockStats30 = mockStats7.copy(
        range = StatisticsRange.LAST_30_DAYS,
        startDate = initialDate.minusDays(29),
        totalPeriodDays = 30
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { getPeriodStatisticsUseCase(StatisticsRange.LAST_7_DAYS, any(), any()) } returns flowOf(mockStats7)
        every { getPeriodStatisticsUseCase(StatisticsRange.LAST_30_DAYS, any(), any()) } returns flowOf(mockStats30)
        every { getMonthlyCalendarHabitsUseCase(any(), any()) } returns flowOf(emptyList())

        viewModel = StatisticsViewModel(
            getPeriodStatisticsUseCase = getPeriodStatisticsUseCase,
            getMonthlyCalendarHabitsUseCase = getMonthlyCalendarHabitsUseCase,
            initialRange = StatisticsRange.LAST_7_DAYS,
            initialYearMonth = initialYearMonth,
            initialCalendarDate = initialDate
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Estado inicial carga estadísticas de 7 días")
    fun testInitialStateLoads7Days() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatisticsRange.LAST_7_DAYS, state.selectedRange)
        assertEquals(mockStats7, state.statistics)
        assertFalse(state.isLoading)
        assertFalse(state.isCalendarOpen)
        assertEquals(initialYearMonth, state.calendarYearMonth)
        assertEquals(initialDate, state.selectedCalendarDate)
    }

    @Test
    @DisplayName("Cambiar a rango de 30 días actualiza estadísticas")
    fun testRangeSelection() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(StatisticsEvent.OnRangeSelected(StatisticsRange.LAST_30_DAYS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatisticsRange.LAST_30_DAYS, state.selectedRange)
        assertEquals(mockStats30, state.statistics)
    }

    @Test
    @DisplayName("Abrir y cerrar calendario altera isCalendarOpen")
    fun testCalendarOpenAndClose() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(StatisticsEvent.OnOpenCalendarClicked())
        assertTrue(viewModel.uiState.value.isCalendarOpen)

        viewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked)
        assertFalse(viewModel.uiState.value.isCalendarOpen)
    }

    @Test
    @DisplayName("Abrir calendario con fecha inicial sincroniza selectedCalendarDate y calendarYearMonth")
    fun testCalendarOpenWithInitialDate() = runTest(testDispatcher) {
        advanceUntilIdle()

        val targetDate = LocalDate.of(2026, 7, 15)
        viewModel.onEvent(StatisticsEvent.OnOpenCalendarClicked(initialDate = targetDate))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCalendarOpen)
        assertEquals(targetDate, state.selectedCalendarDate)
        assertEquals(YearMonth.of(2026, 7), state.calendarYearMonth)
    }

    @Test
    @DisplayName("Navegación de mes anterior y siguiente altera calendarYearMonth")
    fun testMonthNavigation() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onEvent(StatisticsEvent.OnPreviousMonthClicked)
        assertEquals(YearMonth.of(2026, 8), viewModel.uiState.value.calendarYearMonth)

        viewModel.onEvent(StatisticsEvent.OnNextMonthClicked)
        viewModel.onEvent(StatisticsEvent.OnNextMonthClicked)
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.calendarYearMonth)
    }

    @Test
    @DisplayName("Seleccionar fecha del calendario actualiza selectedCalendarDate")
    fun testDateSelection() = runTest(testDispatcher) {
        advanceUntilIdle()

        val newDate = LocalDate.of(2026, 9, 20)
        viewModel.onEvent(StatisticsEvent.OnCalendarDateSelected(newDate))
        assertEquals(newDate, viewModel.uiState.value.selectedCalendarDate)
    }
}
