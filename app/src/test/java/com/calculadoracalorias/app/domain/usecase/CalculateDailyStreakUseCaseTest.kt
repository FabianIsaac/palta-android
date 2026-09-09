package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.repository.MealRepository
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CalculateDailyStreakUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private lateinit var useCase: CalculateDailyStreakUseCase

    private val testZoneId = ZoneId.of("America/Santiago")
    private val today = LocalDate.of(2026, 9, 8)

    @BeforeEach
    fun setUp() {
        useCase = CalculateDailyStreakUseCase(mealRepository)
    }

    private fun createMeal(category: MealCategory, date: LocalDate, hour: Int = 12): MealEntry {
        val timestamp = date.atTime(LocalTime.of(hour, 0)).atZone(testZoneId).toInstant().toEpochMilli()
        return MealEntry(
            id = timestamp,
            category = category,
            timestamp = timestamp,
            items = emptyList(),
            summary = NutritionSummary(300.0, 10.0, 30.0, 5.0)
        )
    }

    private fun createFullDayMeals(date: LocalDate): List<MealEntry> {
        return listOf(
            createMeal(MealCategory.DESAYUNO, date, 8),
            createMeal(MealCategory.ALMUERZO, date, 13),
            createMeal(MealCategory.ONCE_CENA, date, 20)
        )
    }

    @Test
    @DisplayName("Racha activa: hoy completado y ayer completado retorna 2 días de racha")
    fun testActiveStreakWithTodayCompleted() = runBlocking {
        val meals = createFullDayMeals(today) + createFullDayMeals(today.minusDays(1))
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(2, result.currentStreakDays)
        assertTrue(result.hasBreakfastToday)
        assertTrue(result.hasLunchToday)
        assertTrue(result.hasDinnerToday)
        assertTrue(result.isTodayCompleted)
        assertTrue(result.missingMealsToday.isEmpty())
    }

    @Test
    @DisplayName("Día en curso: hoy incompleto pero ayer completado mantiene la racha activa de ayer esperando que termine hoy")
    fun testInProgressDayPreservesYesterdayStreak() = runBlocking {
        val todayMeals = listOf(
            createMeal(MealCategory.DESAYUNO, today, 8),
            createMeal(MealCategory.ALMUERZO, today, 13)
        )
        val yesterdayMeals = createFullDayMeals(today.minusDays(1))
        val twoDaysAgoMeals = createFullDayMeals(today.minusDays(2))

        val meals = todayMeals + yesterdayMeals + twoDaysAgoMeals
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(2, result.currentStreakDays)
        assertTrue(result.hasBreakfastToday)
        assertTrue(result.hasLunchToday)
        assertFalse(result.hasDinnerToday)
        assertFalse(result.isTodayCompleted)
        assertEquals(listOf(MealCategory.ONCE_CENA), result.missingMealsToday)
    }

    @Test
    @DisplayName("Día saltado: racha se reinicia si hubo un día incompleto en el pasado")
    fun testSkippedDayBreaksStreak() = runBlocking {
        // Hoy completo, ayer incompleto (solo desayuno), anteayer completo
        val todayMeals = createFullDayMeals(today)
        val yesterdayMeals = listOf(createMeal(MealCategory.DESAYUNO, today.minusDays(1), 9))
        val twoDaysAgoMeals = createFullDayMeals(today.minusDays(2))

        val meals = todayMeals + yesterdayMeals + twoDaysAgoMeals
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(1, result.currentStreakDays)
        assertTrue(result.isTodayCompleted)
    }

    @Test
    @DisplayName("Racha cero: hoy incompleto y ayer tampoco se completó")
    fun testZeroStreakWhenYesterdayIncompleteAndTodayIncomplete() = runBlocking {
        val todayMeals = listOf(createMeal(MealCategory.DESAYUNO, today, 8))
        val yesterdayMeals = listOf(createMeal(MealCategory.ALMUERZO, today.minusDays(1), 13))

        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(todayMeals + yesterdayMeals)

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(0, result.currentStreakDays)
        assertFalse(result.isTodayCompleted)
    }

    @Test
    @DisplayName("Historial vacío: racha 0 y ninguna comida registrada hoy")
    fun testEmptyHistory() = runBlocking {
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(emptyList())

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(0, result.currentStreakDays)
        assertFalse(result.hasBreakfastToday)
        assertFalse(result.hasLunchToday)
        assertFalse(result.hasDinnerToday)
        assertFalse(result.isTodayCompleted)
        assertEquals(
            listOf(MealCategory.DESAYUNO, MealCategory.ALMUERZO, MealCategory.ONCE_CENA),
            result.missingMealsToday
        )
    }

    @Test
    @DisplayName("Racha prolongada de 5 días seguidos")
    fun testLongStreak() = runBlocking {
        val meals = (0..4).flatMap { daysAgo ->
            createFullDayMeals(today.minusDays(daysAgo.toLong()))
        }
        every { mealRepository.getMealsForDay(any(), any()) } returns flowOf(meals)

        val result = useCase(currentDate = today, zoneId = testZoneId).first()

        assertEquals(5, result.currentStreakDays)
        assertTrue(result.isTodayCompleted)
    }
}
