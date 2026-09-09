package com.calculadoracalorias.app.presentation.edit

import app.cash.turbine.test
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.usecase.DeleteMealWithHealthSyncUseCase
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.UpdateMealResult
import com.calculadoracalorias.app.domain.usecase.UpdateMealWithHealthSyncUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditMealViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mealRepository: MealRepository = mockk()
    private val recalculatePortionUseCase = RecalculatePortionUseCase()
    private val updateMealWithHealthSyncUseCase: UpdateMealWithHealthSyncUseCase = mockk()
    private val deleteMealWithHealthSyncUseCase: DeleteMealWithHealthSyncUseCase = mockk()

    private lateinit var viewModel: EditMealViewModel

    private val sampleItem = ScannedFoodItem(
        id = "item-1",
        name = "Marraqueta con palta",
        servingGrams = 100.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 8.0,
        carbsPer100g = 45.0,
        fatPer100g = 5.0
    )

    private val sampleMeal = MealEntry(
        id = 10L,
        category = MealCategory.DESAYUNO,
        timestamp = 1788775200000L,
        items = listOf(sampleItem),
        summary = NutritionSummary(250.0, 8.0, 45.0, 5.0),
        healthConnectRecordId = "hc-record-10"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = EditMealViewModel(
            mealRepository = mealRepository,
            recalculatePortionUseCase = recalculatePortionUseCase,
            updateMealWithHealthSyncUseCase = updateMealWithHealthSyncUseCase,
            deleteMealWithHealthSyncUseCase = deleteMealWithHealthSyncUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Debe cargar la comida por ID y reflejarla en el UiState")
    fun testLoadMealSuccess() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)

        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(10L, state.mealId)
        assertEquals(MealCategory.DESAYUNO, state.category)
        assertEquals(1, state.items.size)
        assertEquals("Marraqueta con palta", state.items.first().name)
        assertEquals(250.0, state.summary.totalCalories)
    }

    @Test
    @DisplayName("Debe mostrar error si no se encuentra la comida solicitada")
    fun testLoadMealNotFound() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(99L) } returns Result.success(null)

        viewModel.loadMeal(99L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("No se encontró el registro de la comida solicitada.", state.errorMessage)
    }

    @Test
    @DisplayName("Debe recalcular la porción y los totales cuando cambia la cantidad de gramos")
    fun testPortionChangedRecalculates() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        // Cambiar porción de 100g a 200g
        viewModel.onEvent(EditMealEvent.OnServingGramsChanged(0, 200.0))

        val state = viewModel.uiState.value
        assertEquals(200.0, state.items[0].servingGrams)
        assertEquals(500.0, state.items[0].totalCalories)
        assertEquals(500.0, state.summary.totalCalories)
    }

    @Test
    @DisplayName("Debe recalcular los totales al remover un alimento")
    fun testRemoveItemRecalculates() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(EditMealEvent.OnRemoveItem(0))

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals(0.0, state.summary.totalCalories)
    }

    @Test
    @DisplayName("Debe guardar los cambios con éxito e indicar isSavedSuccessfully")
    fun testSaveChangesSuccess() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery {
            updateMealWithHealthSyncUseCase(
                mealId = 10L,
                category = MealCategory.DESAYUNO,
                timestamp = any(),
                items = any(),
                existingHealthConnectRecordId = "hc-record-10"
            )
        } returns Result.success(
            UpdateMealResult(
                mealId = 10L,
                healthConnectRecordId = "hc-record-10",
                syncedWithHealthConnect = true
            )
        )

        viewModel.uiState.test {
            skipItems(1) // estado previo
            viewModel.onEvent(EditMealEvent.OnSaveMealClicked)
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = expectMostRecentItem()
            assertFalse(finalState.isSaving)
            assertTrue(finalState.isSavedSuccessfully)
        }
    }

    @Test
    @DisplayName("Debe manejar el diálogo de confirmación y eliminar la comida con éxito")
    fun testDeleteMealSuccess() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(EditMealEvent.OnDeleteMealClicked)
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        coEvery { deleteMealWithHealthSyncUseCase(10L) } returns Result.success(Unit)

        viewModel.onEvent(EditMealEvent.OnConfirmDeleteClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertFalse(state.showDeleteConfirmation)
        assertTrue(state.isDeletedSuccessfully)

        coVerify(exactly = 1) {
            deleteMealWithHealthSyncUseCase(10L)
        }
    }

    @Test
    @DisplayName("Debe actualizar el nombre de un alimento cuando se emite OnItemNameChanged")
    fun testItemNameChanged() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(EditMealEvent.OnItemNameChanged(0, "Media marraqueta tostada"))

        val state = viewModel.uiState.value
        assertEquals("Media marraqueta tostada", state.items[0].name)
        // La densidad nutricional y totales se mantienen idénticos
        assertEquals(250.0, state.summary.totalCalories)
    }

    @Test
    @DisplayName("Debe agregar un nuevo alimento a la comida y recalcular instantáneamente el resumen nutricional")
    fun testItemAddedRecalculatesSummary() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealById(10L) } returns Result.success(sampleMeal)
        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        val newItem = ScannedFoodItem(
            id = "item-2",
            name = "Huevo duro",
            servingGrams = 50.0,
            caloriesPer100g = 155.0,
            proteinPer100g = 13.0,
            carbsPer100g = 1.1,
            fatPer100g = 11.0
        )

        viewModel.onEvent(EditMealEvent.OnItemAdded(newItem))

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals("Huevo duro", state.items[1].name)
        // 250 kcal (sampleItem) + (155 * 0.5 = 77.5) = 327.5 kcal
        assertEquals(327.5, state.summary.totalCalories, 0.01)
        // 8.0g + 6.5g = 14.5g
        assertEquals(14.5, state.summary.totalProtein, 0.01)
        // 45.0g + 0.55g = 45.55g
        assertEquals(45.55, state.summary.totalCarbs, 0.01)
        // 5.0g + 5.5g = 10.5g
        assertEquals(10.5, state.summary.totalFat, 0.01)
    }

    @Test
    @DisplayName("Debe delegar búsqueda en catálogo local y alimentos populares a FoodCatalogRepository")
    fun testCatalogSearchDelegation() = runTest(testDispatcher) {
        val foodCatalog: com.calculadoracalorias.app.domain.repository.FoodCatalogRepository = mockk()
        val customViewModel = EditMealViewModel(
            mealRepository = mealRepository,
            recalculatePortionUseCase = recalculatePortionUseCase,
            updateMealWithHealthSyncUseCase = updateMealWithHealthSyncUseCase,
            deleteMealWithHealthSyncUseCase = deleteMealWithHealthSyncUseCase,
            foodCatalogRepository = foodCatalog
        )

        coEvery { foodCatalog.searchFood("palta") } returns listOf(sampleItem)
        coEvery { foodCatalog.getPopularFoods() } returns listOf(sampleItem)

        val searchResults = customViewModel.searchCatalog("palta")
        assertEquals(1, searchResults.size)
        assertEquals("Marraqueta con palta", searchResults.first().name)

        val popularResults = customViewModel.getPopularFoods()
        assertEquals(1, popularResults.size)
    }

    @Test
    @DisplayName("OnDateChanged debe actualizar el timestamp conservando la hora original")
    fun testDateChangedPreservesTime() = runTest(testDispatcher) {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val originalTime = LocalTime.of(13, 45)
        val initialTimestamp = today.atTime(originalTime).atZone(zoneId).toInstant().toEpochMilli()

        val mealWithSpecificTime = sampleMeal.copy(timestamp = initialTimestamp)
        coEvery { mealRepository.getMealById(10L) } returns Result.success(mealWithSpecificTime)

        viewModel.loadMeal(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        val newTargetDate = today.minusDays(3)
        viewModel.onEvent(EditMealEvent.OnDateChanged(newTargetDate))

        val updatedState = viewModel.uiState.value
        val updatedZoned = Instant.ofEpochMilli(updatedState.timestamp).atZone(zoneId)

        assertEquals(newTargetDate, updatedZoned.toLocalDate())
        assertEquals(originalTime.hour, updatedZoned.toLocalTime().hour)
        assertEquals(originalTime.minute, updatedZoned.toLocalTime().minute)
    }
}
