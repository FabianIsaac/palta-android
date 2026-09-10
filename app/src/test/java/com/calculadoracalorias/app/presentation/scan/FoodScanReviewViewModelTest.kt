package com.calculadoracalorias.app.presentation.scan

import app.cash.turbine.test
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealResult
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
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

    @Test
    @DisplayName("Debe interpretar descripción en lenguaje natural y agregar los ítems a la revisión")
    fun testAnalyzeNaturalLanguageAddsItems() = runTest(testDispatcher) {
        val parseUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase = mockk()
        val viewModelWithNL = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseUseCase
        )

        val expectedDetected = DetectedMealResult(
            items = listOf(
                ScannedFoodItem(
                    id = "item-coffee",
                    name = "Café con leche",
                    servingGrams = 200.0,
                    caloriesPer100g = 35.0,
                    proteinPer100g = 3.4,
                    carbsPer100g = 4.8,
                    fatPer100g = 0.2
                )
            ),
            suggestedMealType = MealCategory.DESAYUNO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )

        coEvery { parseUseCase("Un café con leche") } returns Result.success(expectedDetected)

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage("Un café con leche"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModelWithNL.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isAnalyzingText)
        assertEquals(1, state.items.size)
        assertEquals("Café con leche", state.items.first().name)
        assertEquals(MealCategory.DESAYUNO, state.selectedCategory)
        assertEquals(70.0, state.totalCalories, 0.01)
    }

    @Test
    @DisplayName("Debe activar isLoading = true e isAnalyzingText = true inmediatamente al iniciar análisis de texto")
    fun testAnalyzeNaturalLanguageSetsLoadingTrueInitially() = runTest(testDispatcher) {
        val parseUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase = mockk()
        val viewModelWithNL = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseUseCase
        )

        coEvery { parseUseCase(any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(
                DetectedMealResult(
                    items = listOf(itemPollo),
                    suggestedMealType = MealCategory.ALMUERZO,
                    analysisSource = VisionSource.LOCAL_DEVICE
                )
            )
        }

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage("Un plato de pollo"))

        // Antes de que el delay termine
        assertTrue(viewModelWithNL.uiState.value.isLoading)
        assertTrue(viewModelWithNL.uiState.value.isAnalyzingText)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModelWithNL.uiState.value.isLoading)
        assertFalse(viewModelWithNL.uiState.value.isAnalyzingText)
    }

    @Test
    @DisplayName("Debe limpiar estrictamente el estado con resetState()")
    fun testResetStateClearsAllItemsAndResetsState() = runTest {
        viewModel.initializeWithResult(
            DetectedMealResult(
                items = listOf(itemPollo, itemPalta),
                suggestedMealType = MealCategory.ALMUERZO,
                analysisSource = VisionSource.LOCAL_DEVICE
            )
        )
        assertEquals(2, viewModel.uiState.value.items.size)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertEquals(0.0, state.totalCalories)
        assertEquals(0.0, state.totalProtein)
        assertFalse(state.isLoading)
        assertFalse(state.saveSuccess)
    }

    @Test
    @DisplayName("Debe reiniciar el estado tras descartar el éxito de guardado para no arrastrar alimentos previos")
    fun testDismissSuccessResetsStateSoNextEntryStartsClean() = runTest(testDispatcher) {
        viewModel.initializeWithResult(
            DetectedMealResult(
                items = listOf(itemPollo),
                suggestedMealType = MealCategory.ALMUERZO,
                analysisSource = VisionSource.MINIMAX_CLOUD
            )
        )

        coEvery {
            saveMealWithHealthSyncUseCase(any(), any(), any(), any())
        } returns Result.success(
            SaveMealResult(mealId = 1L, healthConnectRecordId = null, syncedWithHealthConnect = false)
        )

        viewModel.onEvent(FoodScanReviewEvent.OnConfirmAndSave)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)

        // El usuario o la navegación descartan el éxito
        viewModel.onEvent(FoodScanReviewEvent.OnDismissSuccess)

        val resetState = viewModel.uiState.value
        assertTrue(resetState.items.isEmpty())
        assertEquals(0.0, resetState.totalCalories)
        assertFalse(resetState.saveSuccess)
    }

    @Test
    @DisplayName("startImageAnalysis debe activar carga espaciosa y actualizar con el resultado detectado")
    fun testStartImageAnalysisShowsLoadingAndUpdatesWithResult() = runTest(testDispatcher) {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPalta),
            suggestedMealType = MealCategory.ONCE_CENA,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )

        viewModel.startImageAnalysis(
            imageBytes = byteArrayOf(1, 2, 3),
            category = MealCategory.ONCE_CENA,
            analyzeCall = {
                Result.success(detectedResult)
            }
        )

        // Verificamos que al finalizar la corrutina tenga el resultado
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.items.size)
        assertEquals("Palta hass", state.items.first().name)
        assertEquals(MealCategory.ONCE_CENA, state.selectedCategory)
    }

    @Test
    @DisplayName("Debe detectar comida previa en la misma categoría y establecer minutos transcurridos")
    fun testCheckRecentMealConsolidationSetsMinutesAgo() = runTest(testDispatcher) {
        val mockRepo: MealRepository = mockk()
        val now = 1700000000000L
        val recentMeal = MealEntry(
            id = 99L,
            category = MealCategory.ALMUERZO,
            timestamp = now - (12 * 60 * 1000L), // hace 12 minutos
            items = listOf(itemPollo),
            summary = NutritionSummary.fromItems(listOf(itemPollo))
        )

        coEvery {
            mockRepo.getMostRecentMealForCategory(
                category = MealCategory.ALMUERZO,
                startTime = any(),
                endTime = any()
            )
        } returns Result.success(recentMeal)

        val vmWithRepo = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            mealRepository = mockRepo
        )

        vmWithRepo.checkRecentMealConsolidation(MealCategory.ALMUERZO, currentTimestamp = now)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(12, vmWithRepo.uiState.value.consolidateTargetMealMinutesAgo)
    }

    @Test
    @DisplayName("Debe conservar lastRawDescription y habilitar canRetryTextAnalysis ante error en análisis de texto")
    fun testNaturalLanguageErrorPreservesLastRawDescription() = runTest(testDispatcher) {
        val parseUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase = mockk()
        val viewModelWithNL = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseUseCase
        )

        val input = "2 fajitas con pollo y lechuga"
        coEvery { parseUseCase(input) } returns Result.failure(RuntimeException("Error de conexión"))

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage(input))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModelWithNL.uiState.value
        assertFalse(state.isAnalyzingText)
        assertEquals("Error de conexión", state.errorMessage)
        assertEquals(input, state.lastRawDescription)
        assertTrue(state.canRetryTextAnalysis)
    }

    @Test
    @DisplayName("Debe reintentar análisis con OnRetryNaturalLanguage usando lastRawDescription")
    fun testRetryNaturalLanguageUsesLastRawDescription() = runTest(testDispatcher) {
        val parseUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase = mockk()
        val viewModelWithNL = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseUseCase
        )

        val input = "2 fajitas con pollo y lechuga"
        // Primer intento falla
        coEvery { parseUseCase(input) } returns Result.failure(RuntimeException("Timeout"))

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage(input))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModelWithNL.uiState.value.canRetryTextAnalysis)

        // Segundo intento con Reintentar tiene éxito
        val expected = DetectedMealResult(
            items = listOf(itemPollo),
            suggestedMealType = MealCategory.ONCE_CENA,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
        coEvery { parseUseCase(input) } returns Result.success(expected)

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnRetryNaturalLanguage)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModelWithNL.uiState.value
        assertFalse(state.isAnalyzingText)
        assertEquals(1, state.items.size)
        assertEquals(input, state.lastRawDescription)
        assertFalse(state.canRetryTextAnalysis)
    }

    @Test
    @DisplayName("Debe actualizar el nombre del ítem en FoodScanReviewViewModel sin alterar sus macronutrientes")
    fun testItemNameChanged() = runTest {
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo, itemPalta),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
        viewModel.initializeWithResult(detectedResult)

        viewModel.onEvent(FoodScanReviewEvent.OnItemNameChanged("item-1", "Pechuga grillada"))

        val state = viewModel.uiState.value
        assertEquals("Pechuga grillada", state.items.first { it.id == "item-1" }.name)
        assertEquals("Palta hass", state.items.first { it.id == "item-2" }.name)
        // Totales nutricionales intactos
        assertEquals(407.5, state.totalCalories, 0.01)
    }

    @Test
    @DisplayName("Debe persistir la comida con el timestamp derivado de targetDate cuando se confirma el guardado")
    fun testConfirmAndSaveUsesTargetDateTimestamp() = runTest(testDispatcher) {
        val yesterday = LocalDate.now().minusDays(1)
        val detectedResult = DetectedMealResult(
            items = listOf(itemPollo),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
        viewModel.initializeWithResult(detectedResult, targetDate = yesterday)

        val capturedTimestamp = slot<Long>()
        coEvery {
            saveMealWithHealthSyncUseCase(
                mealName = "Almuerzo",
                category = MealCategory.ALMUERZO,
                timestamp = capture(capturedTimestamp),
                items = any(),
                rawDescription = any(),
                isPendingAiRefinement = any()
            )
        } returns Result.success(
            SaveMealResult(mealId = 2L, healthConnectRecordId = null, syncedWithHealthConnect = false)
        )

        viewModel.onEvent(FoodScanReviewEvent.OnConfirmAndSave)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(capturedTimestamp.isCaptured)
        val capturedLocalDate = Instant.ofEpochMilli(capturedTimestamp.captured)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        assertEquals(yesterday, capturedLocalDate)
    }

    @Test
    @DisplayName("OnTargetDateChanged debe actualizar targetDate en el estado")
    fun testTargetDateChangedUpdatesState() = runTest {
        val target = LocalDate.now().minusDays(2)
        viewModel.onEvent(FoodScanReviewEvent.OnTargetDateChanged(target))
        assertEquals(target, viewModel.uiState.value.targetDate)
    }

    @Test
    @DisplayName("Debe exponer lastTechnicalError cuando ocurre un fallback local con error técnico previo")
    fun testLocalFallbackPreservesLastTechnicalError() = runTest(testDispatcher) {
        val parseUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase = mockk()
        val viewModelWithNL = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseUseCase
        )

        val technicalDetails = com.calculadoracalorias.app.domain.model.AiTechnicalDetails(
            provider = com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM,
            model = "meta/llama-3.3-70b-instruct",
            endpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
            httpStatus = 429,
            errorBody = "{\"error\": \"Quota exceeded\"}",
            durationMs = 120
        )

        val localResult = DetectedMealResult(
            items = listOf(itemPollo),
            suggestedMealType = MealCategory.ALMUERZO,
            analysisSource = VisionSource.LOCAL_DEVICE,
            technicalError = technicalDetails
        )

        val input = "pechuga de pollo"
        coEvery { parseUseCase(input) } returns Result.success(localResult)

        viewModelWithNL.onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage(input))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModelWithNL.uiState.value
        assertTrue(state.isPendingAiRefinement)
        assertTrue(state.canRetryTextAnalysis)
        assertNotNull(state.lastTechnicalError)
        assertEquals(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM, state.lastTechnicalError?.provider)
        assertEquals(429, state.lastTechnicalError?.httpStatus)
        assertEquals("{\"error\": \"Quota exceeded\"}", state.lastTechnicalError?.errorBody)
    }
}
