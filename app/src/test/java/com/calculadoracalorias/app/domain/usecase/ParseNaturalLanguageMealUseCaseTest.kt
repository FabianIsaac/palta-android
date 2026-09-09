package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ParseNaturalLanguageMealUseCaseTest {

    private val analyzer: NaturalLanguageMealAnalyzer = mockk()
    private lateinit var useCase: ParseNaturalLanguageMealUseCase

    @BeforeEach
    fun setUp() {
        useCase = ParseNaturalLanguageMealUseCase(analyzer)
    }

    @Test
    @DisplayName("Debe retornar error si la descripción está vacía o solo contiene espacios")
    fun testBlankDescriptionFails() = runTest {
        val emptyResult = useCase("")
        assertTrue(emptyResult.isFailure)
        assertEquals("Ingresa una descripción de tu comida.", emptyResult.exceptionOrNull()?.message)

        val spacesResult = useCase("    ")
        assertTrue(spacesResult.isFailure)
        assertEquals("Ingresa una descripción de tu comida.", spacesResult.exceptionOrNull()?.message)

        coVerify(exactly = 0) { analyzer.analyzeTextDescription(any()) }
    }

    @Test
    @DisplayName("Debe recortar espacios y delegar al analizador exitosamente")
    fun testValidDescriptionSuccess() = runTest {
        val expectedResult = DetectedMealResult(
            items = listOf(
                ScannedFoodItem(
                    id = "item-1",
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

        coEvery { analyzer.analyzeTextDescription("Un café con leche y una marraqueta") } returns Result.success(expectedResult)

        val result = useCase("   Un café con leche y una marraqueta   ")

        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        coVerify(exactly = 1) { analyzer.analyzeTextDescription("Un café con leche y una marraqueta") }
    }

    @Test
    @DisplayName("Debe propagar los errores retornados por el analizador")
    fun testAnalyzerErrorPropagated() = runTest {
        val exception = RuntimeException("Error al conectar con el servidor")
        coEvery { analyzer.analyzeTextDescription(any()) } returns Result.failure(exception)

        val result = useCase("Taza de té")

        assertTrue(result.isFailure)
        assertEquals("Error al conectar con el servidor", result.exceptionOrNull()?.message)
    }
}
