package com.calculadoracalorias.app.presentation.scan

import app.cash.turbine.test
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealResult
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import io.mockk.coEvery
import io.mockk.mockk
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
class FoodScanReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val saveMealWithHealthSyncUseCase: SaveMealWithHealthSyncUseCase = mockk()
    private val recalculatePortionUseCase = RecalculatePortionUseCase()

    private lateinit var viewModel: FoodScanReviewViewModel

    private val itemPollo = ScannedFoodItem(
        id = "item-1",
        name = "Pechuga de pollo a la plancha",
        servingGrams = 150.0,
        caloriesPer100g = 165.0,
        proteinPer100g = 31.0,
        carbsPer100g = 0.0,
        fatPer100g = 3.6
    )

    private val itemPalta = ScannedFoodItem(
        id = "item-2",
        name = "Palta hass",
        servingGrams = 100.0,
        caloriesPer100g = 160.0,
        proteinPer100g = 2.0,
        carbsPer100g = 9.0,
        fatPer100g = 15.0
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Debe inicializar el estado con el resultado del análisis y calcular totales correctamente")
    fun testInitializeWithResult() = runTest {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo, itemPalta),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )

        viewModel.initializeWithResult(detectedResult)

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals(MealCategory.ALMUERZO, state.selectedCategory)
        assertEquals(407.5, state.totalCalories, 0.01)
        assertEquals(48.5, state.totalProtein, 0.01)
        assertEquals(9.0, state.totalCarbs, 0.01)
        assertEquals(20.4, state.totalFat, 0.01)
    }

    @Test
    @DisplayName("Debe recalcular instantáneamente los totales cuando el usuario modifica la porción en gramos")
    fun testPortionChangedRecalculatesTotals() = runTest {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo, itemPalta),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
        viewModel.initializeWithResult(detectedResult)

        // Cambiar pollo de 150g a 200g
        viewModel.onEvent(FoodScanReviewEvent.OnPortionChanged("item-1", 200.0))

        val updatedState = viewModel.uiState.value
        val updatedPollo = updatedState.items.first { it.id == "item-1" }
        assertEquals(200.0, updatedPollo.servingGrams)
        assertEquals(330.0, updatedPollo.totalCalories, 0.01)
        assertEquals(62.0, updatedPollo.totalProtein, 0.01)

        // Total: 330 (pollo) + 160 (palta) = 490 kcal
        assertEquals(490.0, updatedState.totalCalories, 0.01)
    }

    @Test
    @DisplayName("Debe deducir calorías y macros al eliminar un alimento detectado erróneamente")
    fun testItemRemovedUpdatesTotals() = runTest {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo, itemPalta),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.LOCAL_DEVICE
        )
        viewModel.initializeWithResult(detectedResult)

        // Eliminar palta
        viewModel.onEvent(FoodScanReviewEvent.OnItemRemoved("item-2"))

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("Pechuga de pollo a la plancha", state.items.first().name)
        assertEquals(247.5, state.totalCalories, 0.01)
        assertEquals(46.5, state.totalProtein, 0.01)
    }

    @Test
    @DisplayName("Debe actualizar la categoría de comida chilena seleccionada")
    fun testCategoryChanged() = runTest {
        viewModel.onEvent(FoodScanReviewEvent.OnMealCategoryChanged(MealCategory.ONCE_CENA))
        assertEquals(MealCategory.ONCE_CENA, viewModel.uiState.value.selectedCategory)
    }

    @Test
    @DisplayName("Debe invocar el caso de uso de guardado y emitir estado de éxito y sincronización")
    fun testConfirmAndSaveSuccess() = runTest(testDispatcher) {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
        viewModel.initializeWithResult(detectedResult)

        coEvery {
            saveMealWithHealthSyncUseCase(
                mealName = "Almuerzo",
                category = MealCategory.ALMUERZO,
                timestamp = any(),
                items = any()
            )
        } returns Result.success(
            SaveMealResult(
                mealId = 1L,
                healthConnectRecordId = "hc-uuid-456",
                syncedWithHealthConnect = true
            )
        )

        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isSaving)

            viewModel.onEvent(FoodScanReviewEvent.OnConfirmAndSave)
            testDispatcher.scheduler.advanceUntilIdle()

            val successState = expectMostRecentItem()
            assertFalse(successState.isSaving)
            assertTrue(successState.saveSuccess)
            assertTrue(successState.syncedWithHealthConnect)
        }
    }

    @Test
    @DisplayName("Debe mostrar error si se intenta guardar con la lista vacía")
    fun testConfirmAndSaveEmptyFails() = runTest {
        viewModel.onEvent(FoodScanReviewEvent.OnConfirmAndSave)

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
        assertNotNull(state.errorMessage)
        assertEquals("Agrega al menos un alimento para poder guardar tu comida.", state.errorMessage)
    }
}
