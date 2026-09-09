package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
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
import java.time.ZoneId

class GetDailyMealSummaryUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val dailyBudgetRepository: DailyBudgetRepository = mockk()
    private lateinit var useCase: GetDailyMealSummaryUseCase

    private val testZoneId = ZoneId.of("America/Santiago")
    private val testDate = LocalDate.of(2026, 9, 7)

    private val breakfastItem = ScannedFoodItem(
        id = "marraqueta-1",
        name = "Marraqueta con palta",
        servingGrams = 100.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 8.0,
        carbsPer100g = 45.0,
        fatPer100g = 5.0
    )

    private val lunchItem = ScannedFoodItem(
        id = "porotos-1",
        name = "Porotos con riendas",
        servingGrams = 200.0,
        caloriesPer100g = 180.0,
        proteinPer100g = 10.0,
        carbsPer100g = 25.0,
        fatPer100g = 4.0
    )

    @BeforeEach
    fun setUp() {
        useCase = GetDailyMealSummaryUseCase(mealRepository, dailyBudgetRepository)
    }

    @Test
    @DisplayName("Debe consolidar correctamente las comidas del día agrupadas por categoría y calcular calorías restantes")
    fun testConsolidateDailySummary() = runBlocking {
        val defaultBudget = DailyMacroBudget(
            targetCalories = 2000.0,
            targetProteinGrams = 150.0,
            targetCarbsGrams = 200.0,
            targetFatGrams = 65.0
        )

        val breakfastEntry = MealEntry(
            id = 1L,
            category = MealCategory.DESAYUNO,
            timestamp = 1788775200000L,
            items = listOf(breakfastItem),
            summary = NutritionSummary.fromItems(listOf(breakfastItem))
        )

        val lunchEntry = MealEntry(
            id = 2L,
            category = MealCategory.ALMUERZO,
            timestamp = 1788793200000L,
            items = listOf(lunchItem),
            summary = NutritionSummary.fromItems(listOf(lunchItem))
        )

        every { dailyBudgetRepository.getDailyBudget() } returns flowOf(defaultBudget)
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(listOf(breakfastEntry, lunchEntry))

        val summary = useCase(testDate, testZoneId).first()

        assertEquals(testDate.toEpochDay(), summary.dateEpochDay)
        assertEquals(defaultBudget, summary.budget)

        // desayuno: 250 kcal, almuerzo: 360 kcal -> total 610 kcal
        assertEquals(610.0, summary.consumed.totalCalories)
        // 2000 - 610 = 1390
        assertEquals(1390.0, summary.remainingCalories)

        // Verificación de categorías chilenas
        assertEquals(1, summary.mealsByCategory[MealCategory.DESAYUNO]?.size)
        assertEquals(1, summary.mealsByCategory[MealCategory.ALMUERZO]?.size)
        assertTrue(summary.mealsByCategory[MealCategory.ONCE_CENA]?.isEmpty() == true)
        assertTrue(summary.mealsByCategory[MealCategory.COLACIONES]?.isEmpty() == true)
    }

    @Test
    @DisplayName("Debe manejar un día sin comidas registradas devolviendo consumo cero y restante igual al presupuesto")
    fun testEmptyDaySummary() = runBlocking {
        val budget = DailyMacroBudget(targetCalories = 1800.0)
        every { dailyBudgetRepository.getDailyBudget() } returns flowOf(budget)
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(emptyList())

        val summary = useCase(testDate, testZoneId).first()

        assertEquals(0.0, summary.consumed.totalCalories)
        assertEquals(0.0, summary.consumed.totalProtein)
        assertEquals(0.0, summary.consumed.totalCarbs)
        assertEquals(0.0, summary.consumed.totalFat)
        assertEquals(1800.0, summary.remainingCalories)
        MealCategory.entries.forEach { category ->
            assertTrue(summary.mealsByCategory[category]?.isEmpty() == true)
        }
    }

    @Test
    @DisplayName("Debe calcular correctamente calorías restantes negativas en caso de superávit")
    fun testExceededCalories() = runBlocking {
        val budget = DailyMacroBudget(targetCalories = 500.0)
        val lunchEntry = MealEntry(
            id = 2L,
            category = MealCategory.ALMUERZO,
            timestamp = 1788793200000L,
            items = listOf(lunchItem),
            summary = NutritionSummary.fromItems(listOf(lunchItem)) // 360 kcal
        )
        val breakfastEntry = MealEntry(
            id = 1L,
            category = MealCategory.DESAYUNO,
            timestamp = 1788775200000L,
            items = listOf(breakfastItem),
            summary = NutritionSummary.fromItems(listOf(breakfastItem)) // 250 kcal
        )

        every { dailyBudgetRepository.getDailyBudget() } returns flowOf(budget)
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(listOf(lunchEntry, breakfastEntry))

        val summary = useCase(testDate, testZoneId).first()

        assertEquals(610.0, summary.consumed.totalCalories)
        assertEquals(-110.0, summary.remainingCalories)
    }
}
