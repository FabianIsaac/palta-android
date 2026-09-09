package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.StatisticsRange
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GetPeriodStatisticsUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val dailyBudgetRepository: DailyBudgetRepository = mockk()
    private val supplementRepository: SupplementRepository = mockk()
    private val calculateDailyStreakUseCase: CalculateDailyStreakUseCase = mockk()

    private lateinit var useCase: GetPeriodStatisticsUseCase

    private val testZoneId = ZoneId.of("America/Santiago")
    private val testEndDate = LocalDate.of(2026, 9, 8)
    private val defaultBudget = DailyMacroBudget(
        targetCalories = 2000.0,
        targetProteinGrams = 150.0,
        targetCarbsGrams = 200.0,
        targetFatGrams = 65.0
    )

    @BeforeEach
    fun setUp() {
        useCase = GetPeriodStatisticsUseCase(
            mealRepository = mealRepository,
            dailyBudgetRepository = dailyBudgetRepository,
            supplementRepository = supplementRepository,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase
        )

        every { dailyBudgetRepository.getDailyBudget() } returns flowOf(defaultBudget)
        every { calculateDailyStreakUseCase(any(), any(), any()) } returns flowOf(
            DailyStreak(
                currentStreakDays = 4,
                hasBreakfastToday = true,
                hasLunchToday = true,
                hasDinnerToday = true
            )
        )
    }

    private fun createMeal(category: MealCategory, date: LocalDate, hour: Int = 12): MealEntry {
        val timestamp = date.atTime(LocalTime.of(hour, 0)).atZone(testZoneId).toInstant().toEpochMilli()
        return MealEntry(
            id = timestamp,
            category = category,
            timestamp = timestamp,
            items = emptyList(),
            summary = NutritionSummary(
                totalCalories = 500.0,
                totalProtein = 30.0,
                totalCarbs = 50.0,
                totalFat = 15.0
            )
        )
    }

    @Test
    @DisplayName("Calcula correctamente promedios y métricas diarias para 7 días")
    fun test7DaysStatistics() = runBlocking {
        // En los últimos 2 días hay 3 comidas principales por día (1500 kcal por día)
        val meals = listOf(
            createMeal(MealCategory.DESAYUNO, testEndDate, 8),
            createMeal(MealCategory.ALMUERZO, testEndDate, 13),
            createMeal(MealCategory.ONCE_CENA, testEndDate, 20),
            createMeal(MealCategory.DESAYUNO, testEndDate.minusDays(1), 8),
            createMeal(MealCategory.ALMUERZO, testEndDate.minusDays(1), 13),
            createMeal(MealCategory.ONCE_CENA, testEndDate.minusDays(1), 20)
        )

        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)
        every { supplementRepository.getSupplementsForDateRange(any(), any()) } returns flowOf(emptyMap())

        val stats = useCase(StatisticsRange.LAST_7_DAYS, testEndDate, testZoneId).first()

        assertEquals(StatisticsRange.LAST_7_DAYS, stats.range)
        assertEquals(7, stats.totalPeriodDays)
        assertEquals(7, stats.dailyCalorieMetrics.size)
        assertEquals(testEndDate.minusDays(6), stats.startDate)
        assertEquals(testEndDate, stats.endDate)
        assertEquals(2, stats.habitCompletedDaysCount)
        assertEquals(4, stats.currentStreakDays)

        // 3000 kcal totales en 7 días = ~428.6 kcal promedio
        assertEquals(428.6, stats.averageDailyCalories)
        // 180 g proteína total en 7 días = ~25.7 g promedio
        assertEquals(25.7, stats.averageProteinGrams)
    }

    @Test
    @DisplayName("Calcula correctamente la adherencia de suplementos")
    fun testSupplementAdherence() = runBlocking {
        val supp1 = Supplement(
            id = "creatina",
            name = "Creatina",
            dosageDescription = "5g",
            calories = 0.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 0.0,
            isActive = true,
            isTakenToday = true
        )
        val supp2 = Supplement(
            id = "omega3",
            name = "Omega 3",
            dosageDescription = "2 cápsulas",
            calories = 18.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 2.0,
            isActive = true,
            isTakenToday = false
        )

        // Simular que en cada uno de los 7 días hay 2 suplementos esperados, y solo supp1 está tomado (50% de adherencia)
        val suppsMap = (0..6).associate { dayOffset ->
            testEndDate.minusDays(dayOffset.toLong()) to listOf(supp1, supp2)
        }

        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(emptyList())
        every { supplementRepository.getSupplementsForDateRange(any(), any()) } returns flowOf(suppsMap)

        val stats = useCase(StatisticsRange.LAST_7_DAYS, testEndDate, testZoneId).first()

        // 7 dosis tomadas de 14 esperadas = 50.0%
        assertEquals(50.0, stats.supplementAdherencePercentage)
    }

    @Test
    @DisplayName("Rango de 14 y 30 días genera la cantidad exacta de métricas diarias")
    fun test14And30DaysRanges() = runBlocking {
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(emptyList())
        every { supplementRepository.getSupplementsForDateRange(any(), any()) } returns flowOf(emptyMap())

        val stats14 = useCase(StatisticsRange.LAST_14_DAYS, testEndDate, testZoneId).first()
        assertEquals(14, stats14.dailyCalorieMetrics.size)
        assertEquals(14, stats14.totalPeriodDays)

        val stats30 = useCase(StatisticsRange.LAST_30_DAYS, testEndDate, testZoneId).first()
        assertEquals(30, stats30.dailyCalorieMetrics.size)
        assertEquals(30, stats30.totalPeriodDays)
    }
}
