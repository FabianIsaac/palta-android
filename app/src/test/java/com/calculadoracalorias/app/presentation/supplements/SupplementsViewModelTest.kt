package com.calculadoracalorias.app.presentation.supplements

import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.domain.model.SupplementNutritionEstimate
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import com.calculadoracalorias.app.domain.usecase.EstimateSupplementNutritionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupplementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val supplementRepository: SupplementRepository = mockk()
    private val estimateSupplementNutritionUseCase: EstimateSupplementNutritionUseCase = mockk()

    private val sampleSupplements = listOf(
        Supplement(
            id = "creatina",
            name = "Creatina Monohidrato",
            dosageDescription = "5g",
            calories = 0.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 0.0,
            isActive = true,
            isCustom = false
        ),
        Supplement(
            id = "omega_3",
            name = "Omega 3",
            dosageDescription = "2 cápsulas",
            calories = 18.0,
            proteinGrams = 0.0,
            carbsGrams = 0.0,
            fatGrams = 2.0,
            isActive = false,
            isCustom = false
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { supplementRepository.getAllSupplements() } returns flowOf(sampleSupplements)
        every { supplementRepository.getSupplementIntakeCounts() } returns flowOf(emptyMap())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("Debe cargar la lista de suplementos inicial correctamente")
    fun testInitialLoadSupplements() = runTest(testDispatcher) {
        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.supplements.size)
        assertEquals("Creatina Monohidrato", viewModel.uiState.value.supplements[0].name)
    }

    @Test
    @DisplayName("Debe estimar macros con IA y llenar los campos del formulario")
    fun testEstimateMacros() = runTest(testDispatcher) {
        val estimate = SupplementNutritionEstimate(
            name = "Proteína Whey",
            dosageDescription = "1 scoop (30g)",
            calories = 120.0,
            proteinGrams = 24.0,
            carbsGrams = 2.0,
            fatGrams = 1.5
        )
        coEvery { estimateSupplementNutritionUseCase("Proteína Whey") } returns Result.success(estimate)

        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        viewModel.onEvent(SupplementsEvent.OnInputNameChanged("Proteína Whey"))
        viewModel.onEvent(SupplementsEvent.OnEstimateClicked)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEstimating)
        assertEquals("1 scoop (30g)", viewModel.uiState.value.inputDosage)
        assertEquals("120.0", viewModel.uiState.value.inputCalories)
        assertEquals("24.0", viewModel.uiState.value.inputProtein)
        assertEquals("2.0", viewModel.uiState.value.inputCarbs)
        assertEquals("1.5", viewModel.uiState.value.inputFat)
    }

    @Test
    @DisplayName("Debe guardar un nuevo suplemento personalizado y limpiar los campos")
    fun testSaveCustomSupplement() = runTest(testDispatcher) {
        coEvery { supplementRepository.saveSupplement(any()) } returns Result.success(Unit)

        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        viewModel.onEvent(SupplementsEvent.OnInputNameChanged("Colágeno Hidrolizado"))
        viewModel.onEvent(SupplementsEvent.OnInputDosageChanged("10g"))
        viewModel.onEvent(SupplementsEvent.OnInputCaloriesChanged("36"))
        viewModel.onEvent(SupplementsEvent.OnInputProteinChanged("9"))
        viewModel.onEvent(SupplementsEvent.OnInputCarbsChanged("0"))
        viewModel.onEvent(SupplementsEvent.OnInputFatChanged("0"))

        viewModel.onEvent(SupplementsEvent.OnSaveClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            supplementRepository.saveSupplement(match {
                it.name == "Colágeno Hidrolizado" &&
                it.dosageDescription == "10g" &&
                it.calories == 36.0 &&
                it.proteinGrams == 9.0 &&
                it.isCustom
            })
        }

        // Formulario reseteado y mensaje de éxito presente
        assertEquals("", viewModel.uiState.value.inputName)
        assertEquals("Suplemento guardado con éxito.", viewModel.uiState.value.successMessage)
    }

    @Test
    @DisplayName("Debe alternar el estado activo de un suplemento existente")
    fun testToggleActive() = runTest(testDispatcher) {
        coEvery { supplementRepository.toggleSupplementActive("creatina", false) } returns Result.success(Unit)

        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        viewModel.onEvent(SupplementsEvent.OnToggleActive("creatina", false))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            supplementRepository.toggleSupplementActive("creatina", false)
        }
    }

    @Test
    @DisplayName("Debe eliminar un suplemento del catálogo")
    fun testDeleteSupplement() = runTest(testDispatcher) {
        coEvery { supplementRepository.deleteSupplement("custom_123") } returns Result.success(Unit)

        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        viewModel.onEvent(SupplementsEvent.OnDeleteSupplement("custom_123"))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            supplementRepository.deleteSupplement("custom_123")
        }
    }

    @Test
    @DisplayName("Debe conmutar el colapso/expansión de sugerencias populares")
    fun testToggleSuggestionsExpanded() = runTest(testDispatcher) {
        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.areSuggestionsExpanded)

        viewModel.onEvent(SupplementsEvent.OnToggleSuggestionsExpanded)
        assertTrue(viewModel.uiState.value.areSuggestionsExpanded)

        viewModel.onEvent(SupplementsEvent.OnToggleSuggestionsExpanded)
        assertFalse(viewModel.uiState.value.areSuggestionsExpanded)
    }

    @Test
    @DisplayName("Debe observar el mapa de tomas históricas")
    fun testObserveIntakeCounts() = runTest(testDispatcher) {
        val counts = mapOf("creatina" to 14, "omega_3" to 5)
        every { supplementRepository.getSupplementIntakeCounts() } returns flowOf(counts)

        val viewModel = SupplementsViewModel(supplementRepository, estimateSupplementNutritionUseCase)
        advanceUntilIdle()

        assertEquals(14, viewModel.uiState.value.intakeCounts["creatina"])
        assertEquals(5, viewModel.uiState.value.intakeCounts["omega_3"])
    }

    @Test
    @DisplayName("Debe escanear imagen de tabla nutricional y autocompletar campos")
    fun testScanNutritionLabelSuccess() = runTest(testDispatcher) {
        val labelAnalyzerUseCase: com.calculadoracalorias.app.domain.usecase.AnalyzeNutritionLabelUseCase = mockk()
        val scanResult = com.calculadoracalorias.app.domain.model.NutritionLabelScanResult(
            productName = "Iso Whey",
            servingDescription = "1 scoop (33g)",
            servingGrams = 33.0,
            calories = 110.0,
            proteinGrams = 25.0,
            carbsGrams = 1.0,
            fatGrams = 1.0
        )
        val imageBytes = byteArrayOf(1, 2, 3)
        coEvery { labelAnalyzerUseCase(imageBytes) } returns Result.success(scanResult)

        val viewModel = SupplementsViewModel(
            supplementRepository = supplementRepository,
            estimateSupplementNutritionUseCase = estimateSupplementNutritionUseCase,
            analyzeNutritionLabelUseCase = labelAnalyzerUseCase
        )
        advanceUntilIdle()

        viewModel.onEvent(SupplementsEvent.OnScanImageSelected(imageBytes))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isScanningLabel)
        assertEquals("Iso Whey", viewModel.uiState.value.inputName)
        assertEquals("1 scoop (33g)", viewModel.uiState.value.inputDosage)
        assertEquals("110.0", viewModel.uiState.value.inputCalories)
        assertEquals("25.0", viewModel.uiState.value.inputProtein)
        assertEquals("1.0", viewModel.uiState.value.inputCarbs)
        assertEquals("1.0", viewModel.uiState.value.inputFat)
        assertEquals("Tabla nutricional analizada con éxito.", viewModel.uiState.value.successMessage)
    }
}
