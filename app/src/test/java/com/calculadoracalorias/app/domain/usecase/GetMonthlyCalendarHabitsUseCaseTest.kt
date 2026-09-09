package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class GetMonthlyCalendarHabitsUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val dailyBudgetRepository: DailyBudgetRepository = mockk()
    private val supplementRepository: SupplementRepository = mockk()

    private lateinit var useCase: GetMonthlyCalendarHabitsUseCase

    private val testZoneId = ZoneId.of("America/Santiago")
    private val september2026 = YearMonth.of(2026, 9) // 1 de Septiembre 2026 es Martes (leading: 1 día, Lunes 31 de Agosto)
    private val defaultBudget = DailyMacroBudget(
        targetCalories = 2000.0,
        targetProteinGrams = 150.0,
        targetCarbsGrams = 200.0,
        targetFatGrams = 65.0
    )

    @BeforeEach
    fun setUp() {
        useCase = GetMonthlyCalendarHabitsUseCase(
            mealRepository = mealRepository,
            dailyBudgetRepository = dailyBudgetRepository,
            supplementRepository = supplementRepository
        )

        every { dailyBudgetRepository.getDailyBudget() } returns flowOf(defaultBudget)
    }

    private fun createMeal(category: MealCategory, date: LocalDate, hour: Int = 12): MealEntry {
        val timestamp = date.atTime(LocalTime.of(hour, 0)).atZone(testZoneId).toInstant().toEpochMilli()
        return MealEntry(
            id = timestamp,
            category = category,
            timestamp = timestamp,
            items = emptyList(),
            summary = NutritionSummary(
                totalCalories = 400.0,
                totalProtein = 25.0,
                totalCarbs = 40.0,
                totalFat = 10.0
            )
        )
    }

    @Test
    @DisplayName("Genera grilla de semanas completas de Lunes a Domingo para Septiembre 2026")
    fun testGridGeneration() = runBlocking {
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(emptyList())
        every { supplementRepository.getSupplementsForDateRange(any(), any()) } returns flowOf(emptyMap())

        val grid = useCase(september2026, testZoneId).first()

        // Debe ser múltiplo de 7
        assertEquals(0, grid.size % 7)
        // El primer día de la grilla debe ser Lunes
        assertEquals(DayOfWeek.MONDAY, grid.first().date.dayOfWeek)
        // El último día de la grilla debe ser Domingo
        assertEquals(DayOfWeek.SUNDAY, grid.last().date.dayOfWeek)

        // Para Septiembre 2026 (empieza un Martes, 30 días, termina Miércoles):
        // Leading: 1 (Lun 31 Ago). Días mes: 30. Trailing: 4 (Jue 1 a Dom 4 Oct). Total: 35 días (5 semanas).
        assertEquals(35, grid.size)

        // El primer día es 31 de Agosto (no es del mes actual)
        assertFalse(grid.first().isCurrentMonth)
        // El segundo día es 1 de Septiembre (sí es del mes actual)
        assertTrue(grid[1].isCurrentMonth)
    }

    @Test
    @DisplayName("Marca hábito cumplido cuando existen las 3 comidas principales chilenas")
    fun testHabitCompletionInCalendar() = runBlocking {
        val dateWithHabit = LocalDate.of(2026, 9, 15)
        val dateWithoutHabit = LocalDate.of(2026, 9, 16)

        val meals = listOf(
            createMeal(MealCategory.DESAYUNO, dateWithHabit, 8),
            createMeal(MealCategory.ALMUERZO, dateWithHabit, 13),
            createMeal(MealCategory.ONCE_CENA, dateWithHabit, 20),
            // En el otro día sólo hay desayuno y almuerzo (falta once/cena)
            createMeal(MealCategory.DESAYUNO, dateWithoutHabit, 8),
            createMeal(MealCategory.ALMUERZO, dateWithoutHabit, 13)
        )

        val supp = Supplement(
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

        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)
        every { supplementRepository.getSupplementsForDateRange(any(), any()) } returns flowOf(
            mapOf(dateWithHabit to listOf(supp))
        )

        val grid = useCase(september2026, testZoneId).first()

        val summaryHabit = grid.first { it.date == dateWithHabit }
        assertTrue(summaryHabit.hasBreakfast)
        assertTrue(summaryHabit.hasLunch)
        assertTrue(summaryHabit.hasDinner)
        assertTrue(summaryHabit.isHabitCompleted)
        assertTrue(summaryHabit.hasSupplementsTaken)
        assertEquals(1200.0, summaryHabit.totalCalories)

        val summaryNoHabit = grid.first { it.date == dateWithoutHabit }
        assertTrue(summaryNoHabit.hasBreakfast)
        assertTrue(summaryNoHabit.hasLunch)
        assertFalse(summaryNoHabit.hasDinner)
        assertFalse(summaryNoHabit.isHabitCompleted)
        assertFalse(summaryNoHabit.hasSupplementsTaken)
        assertEquals(800.0, summaryNoHabit.totalCalories)
    }
}
