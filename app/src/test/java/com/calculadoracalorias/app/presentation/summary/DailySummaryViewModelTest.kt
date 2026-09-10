package com.calculadoracalorias.app.presentation.summary

import app.cash.turbine.test
import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.domain.model.DailySummary
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import com.calculadoracalorias.app.domain.usecase.CalculateDailyStreakUseCase
import com.calculadoracalorias.app.domain.usecase.GetDailyMealSummaryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailySummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getDailyMealSummaryUseCase: GetDailyMealSummaryUseCase = mockk()
    private val calculateDailyStreakUseCase: CalculateDailyStreakUseCase = mockk()
    private val supplementRepository: SupplementRepository = mockk()
    private lateinit var viewModel: DailySummaryViewModel

    private val initialDate = LocalDate.of(2026, 9, 7)

    private val sampleBudget = DailyMacroBudget(
        targetCalories = 2000.0,
        targetProteinGrams = 150.0,
        targetCarbsGrams = 200.0,
        targetFatGrams = 65.0
    )

    private val sampleSummary = DailySummary(
        dateEpochDay = initialDate.toEpochDay(),
        budget = sampleBudget,
        consumed = NutritionSummary(500.0, 40.0, 50.0, 15.0),
        remainingCalories = 1500.0,
        mealsByCategory = mapOf(
            MealCategory.DESAYUNO to listOf(
                MealEntry(
                    id = 1L,
                    category = MealCategory.DESAYUNO,
                    timestamp = 1000L,
                    items = listOf(
                        ScannedFoodItem(
                            id = "1",
                            name = "Marraqueta con palta",
                            servingGrams = 100.0,
                            caloriesPer100g = 250.0,
                            proteinPer100g = 8.0,
                            carbsPer100g = 45.0,
                            fatPer100g = 5.0
                        )
                    ),
                    summary = NutritionSummary(500.0, 40.0, 50.0, 15.0)
                )
            )
        )
    )

    private val sampleStreak = DailyStreak(
        currentStreakDays = 3,
        hasBreakfastToday = true,
        hasLunchToday = false,
        hasDinnerToday = false
    )

    private val sampleSupplementsFlow = MutableStateFlow(
        listOf(
            Supplement(
                id = "omega_3",
                name = "Omega 3",
                dosageDescription = "2 cápsulas",
                calories = 18.0,
                fatGrams = 2.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                isTakenToday = false
            )
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getDailyMealSummaryUseCase(any(), any()) } returns flowOf(sampleSummary)
        every { calculateDailyStreakUseCase(any(), any(), any()) } returns flowOf(sampleStreak)
        every { supplementRepository.getSupplementsForDate(any()) } returns sampleSupplementsFlow

        viewModel = DailySummaryViewModel(
            getDailyMealSummaryUseCase = getDailyMealSummaryUseCase,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase,
            supplementRepository = supplementRepository,
            initialDate = initialDate
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Debe cargar y reflejar el resumen diario reactivo con racha y suplementos")
    fun testInitialDailySummaryLoads() = runTest(testDispatcher) {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()

            assertFalse(state.isLoading)
            assertEquals(initialDate, state.selectedDate)
            assertEquals(2000.0, state.targetCalories)
            assertEquals(500.0, state.consumedCalories)
            assertEquals(1500.0, state.remainingCalories)
            assertEquals(3, state.streak.currentStreakDays)
            assertEquals(1, state.supplements.size)
            assertFalse(state.supplements.first().isTakenToday)
        }
    }

    @Test
    @DisplayName("Debe sumar calorías y macronutrientes al marcar un suplemento como tomado")
    fun testNutritionalImpactWhenSupplementTaken() = runTest(testDispatcher) {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val initialState = expectMostRecentItem()
            assertEquals(500.0, initialState.consumedCalories)
            assertEquals(15.0, initialState.consumedFatGrams)

            // Simular que el suplemento fue marcado como tomado (+18 kcal, +2g grasas)
            sampleSupplementsFlow.value = listOf(
                Supplement(
                    id = "omega_3",
                    name = "Omega 3",
                    dosageDescription = "2 cápsulas",
                    calories = 18.0,
                    fatGrams = 2.0,
                    proteinGrams = 0.0,
                    carbsGrams = 0.0,
                    isTakenToday = true
                )
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val updatedState = expectMostRecentItem()
            assertEquals(518.0, updatedState.consumedCalories)
            assertEquals(1482.0, updatedState.remainingCalories)
            assertEquals(17.0, updatedState.consumedFatGrams)
            assertTrue(updatedState.supplements.first().isTakenToday)
        }
    }

    @Test
    @DisplayName("OnToggleSupplement debe delegar al repositorio de suplementos")
    fun testOnToggleSupplementDelegatesToRepository() = runTest(testDispatcher) {
        coEvery { supplementRepository.toggleSupplementTaken(any(), any(), any()) } returns Result.success(Unit)

        viewModel.onEvent(DailySummaryEvent.OnToggleSupplement("omega_3", true))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            supplementRepository.toggleSupplementTaken(initialDate, "omega_3", true)
        }
    }

    @Test
    @DisplayName("Debe decrementar la fecha al presionar día anterior")
    fun testPreviousDayClicked() = runTest(testDispatcher) {
        val previousDay = initialDate.minusDays(1)
        val previousSummary = sampleSummary.copy(dateEpochDay = previousDay.toEpochDay())
        every { getDailyMealSummaryUseCase(previousDay, any()) } returns flowOf(previousSummary)

        viewModel.onEvent(DailySummaryEvent.OnPreviousDayClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(previousDay, viewModel.uiState.value.selectedDate)
    }

    @Test
    @DisplayName("Debe incrementar la fecha al presionar día siguiente")
    fun testNextDayClicked() = runTest(testDispatcher) {
        val nextDay = initialDate.plusDays(1)
        val nextSummary = sampleSummary.copy(dateEpochDay = nextDay.toEpochDay())
        every { getDailyMealSummaryUseCase(nextDay, any()) } returns flowOf(nextSummary)

        viewModel.onEvent(DailySummaryEvent.OnNextDayClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(nextDay, viewModel.uiState.value.selectedDate)
    }

    @Test
    @DisplayName("Debe cambiar a la fecha seleccionada por DatePicker")
    fun testDateSelected() = runTest(testDispatcher) {
        val pickedDate = LocalDate.of(2026, 9, 15)
        val pickedSummary = sampleSummary.copy(dateEpochDay = pickedDate.toEpochDay())
        every { getDailyMealSummaryUseCase(pickedDate, any()) } returns flowOf(pickedSummary)

        viewModel.onEvent(DailySummaryEvent.OnDateSelected(pickedDate))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pickedDate, viewModel.uiState.value.selectedDate)
    }

    @Test
    @DisplayName("Debe propagar mensaje de error si falla la consulta del resumen")
    fun testSummaryErrorHandled() = runTest(testDispatcher) {
        val errorDate = LocalDate.of(2026, 9, 20)
        every { getDailyMealSummaryUseCase(errorDate, any()) } returns flow {
            throw IllegalStateException("Fallo en la base de datos")
        }

        viewModel.onEvent(DailySummaryEvent.OnDateSelected(errorDate))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("Fallo en la base de datos", state.errorMessage)
    }

    @Test
    @DisplayName("Debe reflejar las calorías quemadas y pasos desde el caso de uso de actividad")
    fun testHealthActivityReflectedInUiState() = runTest(testDispatcher) {
        val healthUseCase: com.calculadoracalorias.app.domain.usecase.GetDailyHealthActivityUseCase = mockk()
        val prefsRepo: com.calculadoracalorias.app.data.preferences.UserPreferencesRepository = mockk()
        val prefs = com.calculadoracalorias.app.data.preferences.UserPreferences(
            healthConnectActivitySyncEnabled = true,
            includeBurnedCaloriesInBudget = true
        )
        val activity = com.calculadoracalorias.app.domain.model.DailyHealthActivity(
            date = initialDate,
            burnedCalories = 350.0,
            stepsCount = 8200L
        )

        every { prefsRepo.userPreferencesFlow } returns flowOf(prefs)
        coEvery { healthUseCase(initialDate, true) } returns activity

        val customViewModel = DailySummaryViewModel(
            getDailyMealSummaryUseCase = getDailyMealSummaryUseCase,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase,
            supplementRepository = supplementRepository,
            getDailyHealthActivityUseCase = healthUseCase,
            userPreferencesRepository = prefsRepo,
            initialDate = initialDate
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = customViewModel.uiState.value
        assertEquals(350.0, state.burnedCalories)
        assertEquals(8200L, state.stepsCount)
        assertTrue(state.includeBurnedInBudget)
        // target (2000) - consumed (500) + burned (350) = 1850
        assertEquals(1850.0, state.remainingCalories)
    }

    @Test
    @DisplayName("No debe sumar calorías quemadas al presupuesto si includeBurnedInBudget es falso")
    fun testBurnedCaloriesNotAddedWhenDisabled() = runTest(testDispatcher) {
        val healthUseCase: com.calculadoracalorias.app.domain.usecase.GetDailyHealthActivityUseCase = mockk()
        val prefsRepo: com.calculadoracalorias.app.data.preferences.UserPreferencesRepository = mockk()
        val prefs = com.calculadoracalorias.app.data.preferences.UserPreferences(
            healthConnectActivitySyncEnabled = true,
            includeBurnedCaloriesInBudget = false
        )
        val activity = com.calculadoracalorias.app.domain.model.DailyHealthActivity(
            date = initialDate,
            burnedCalories = 400.0,
            stepsCount = 9000L
        )

        every { prefsRepo.userPreferencesFlow } returns flowOf(prefs)
        coEvery { healthUseCase(initialDate, true) } returns activity

        val customViewModel = DailySummaryViewModel(
            getDailyMealSummaryUseCase = getDailyMealSummaryUseCase,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase,
            supplementRepository = supplementRepository,
            getDailyHealthActivityUseCase = healthUseCase,
            userPreferencesRepository = prefsRepo,
            initialDate = initialDate
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = customViewModel.uiState.value
        assertEquals(400.0, state.burnedCalories)
        assertEquals(9000L, state.stepsCount)
        assertFalse(state.includeBurnedInBudget)
        // target (2000) - consumed (500) = 1500 (sin sumar los 400 quemados)
        assertEquals(1500.0, state.remainingCalories)
    }
}
