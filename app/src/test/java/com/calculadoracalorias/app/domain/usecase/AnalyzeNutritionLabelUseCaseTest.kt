package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.NutritionLabelScanResult
import com.calculadoracalorias.app.domain.repository.NutritionLabelAnalyzer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AnalyzeNutritionLabelUseCaseTest {

    private val analyzer: NutritionLabelAnalyzer = mockk()

    @Test
    @DisplayName("Debe retornar fallo cuando el arreglo de bytes está vacío")
    fun testEmptyBytesFails() = runTest {
        val useCase = AnalyzeNutritionLabelUseCase(analyzer)
        val result = useCase(byteArrayOf())
        assertTrue(result.isFailure)
        assertEquals("La imagen capturada no contiene datos válidos.", result.exceptionOrNull()?.message)
    }

    @Test
    @DisplayName("Debe retornar el resultado exitoso del analizador de etiquetas nutricionales")
    fun testSuccessfulLabelAnalysis() = runTest {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        val expected = NutritionLabelScanResult(
            productName = "Iso Whey 100%",
            servingDescription = "1 scoop (33g)",
            servingGrams = 33.0,
            calories = 110.0,
            proteinGrams = 25.0,
            carbsGrams = 1.0,
            fatGrams = 1.0
        )
        coEvery { analyzer.analyzeNutritionLabel(imageBytes) } returns Result.success(expected)

        val useCase = AnalyzeNutritionLabelUseCase(analyzer)
        val result = useCase(imageBytes)

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    @DisplayName("Debe propagar el fallo devuelto por el analizador")
    fun testAnalyzerErrorPropagated() = runTest {
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        coEvery { analyzer.analyzeNutritionLabel(imageBytes) } returns Result.failure(RuntimeException("Error al analizar"))

        val useCase = AnalyzeNutritionLabelUseCase(analyzer)
        val result = useCase(imageBytes)

        assertTrue(result.isFailure)
        assertEquals("Error al analizar", result.exceptionOrNull()?.message)
    }
}
