package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.SupplementNutritionEstimate
import com.calculadoracalorias.app.domain.repository.SupplementNutritionAnalyzer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EstimateSupplementNutritionUseCaseTest {

    private val remoteAnalyzer: SupplementNutritionAnalyzer = mockk()

    @Test
    @DisplayName("Debe retornar error si la descripción está vacía")
    fun testBlankDescriptionFails() = runTest {
        val useCase = EstimateSupplementNutritionUseCase(remoteAnalyzer)
        val result = useCase("   ")
        assertTrue(result.isFailure)
        assertEquals("Ingresa el nombre o descripción del suplemento.", result.exceptionOrNull()?.message)
    }

    @Test
    @DisplayName("Debe retornar el resultado remoto cuando el analizador tiene éxito")
    fun testRemoteAnalyzerSuccess() = runTest {
        val expected = SupplementNutritionEstimate(
            name = "Proteína Whey Vainilla",
            dosageDescription = "1 scoop (30g)",
            calories = 120.0,
            proteinGrams = 25.0,
            carbsGrams = 2.0,
            fatGrams = 1.0
        )
        coEvery { remoteAnalyzer.estimateSupplement("Proteína Whey") } returns Result.success(expected)

        val useCase = EstimateSupplementNutritionUseCase(remoteAnalyzer)
        val result = useCase("Proteína Whey")

        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    @DisplayName("Debe recurrir al fallback offline cuando el analizador remoto falla")
    fun testFallbackOfflineWhenRemoteFails() = runTest {
        coEvery { remoteAnalyzer.estimateSupplement(any()) } returns Result.failure(RuntimeException("Network error"))

        val useCase = EstimateSupplementNutritionUseCase(remoteAnalyzer)

        // Creatina (sin calorías)
        val creatinaResult = useCase("creatina monohidrato")
        assertTrue(creatinaResult.isSuccess)
        val creatina = creatinaResult.getOrThrow()
        assertEquals("5g", creatina.dosageDescription)
        assertEquals(0.0, creatina.calories)
        assertEquals(0.0, creatina.proteinGrams)
        assertEquals(0.0, creatina.carbsGrams)
        assertEquals(0.0, creatina.fatGrams)

        // Citrato de magnesio (sin calorías)
        val magnesioResult = useCase("citrato de magnesio 400mg")
        assertTrue(magnesioResult.isSuccess)
        val magnesio = magnesioResult.getOrThrow()
        assertEquals("400mg", magnesio.dosageDescription)
        assertEquals(0.0, magnesio.calories)

        // Multivitamínico (sin calorías)
        val multiResult = useCase("multivitamínico diario")
        assertTrue(multiResult.isSuccess)
        val multi = multiResult.getOrThrow()
        assertEquals("1 comprimido", multi.dosageDescription)
        assertEquals(0.0, multi.calories)

        // Omega 3 (con grasa y calorías)
        val omegaResult = useCase("omega 3")
        assertTrue(omegaResult.isSuccess)
        val omega = omegaResult.getOrThrow()
        assertEquals("2 cápsulas", omega.dosageDescription)
        assertEquals(18.0, omega.calories)
        assertEquals(2.0, omega.fatGrams)
        assertEquals(0.0, omega.proteinGrams)

        // Proteína (con proteína y calorías)
        val wheyResult = useCase("proteína whey isolada")
        assertTrue(wheyResult.isSuccess)
        val whey = wheyResult.getOrThrow()
        assertEquals("1 scoop (30g)", whey.dosageDescription)
        assertEquals(120.0, whey.calories)
        assertEquals(24.0, whey.proteinGrams)
        assertEquals(2.0, whey.carbsGrams)
        assertEquals(1.5, whey.fatGrams)
    }

    @Test
    @DisplayName("Debe operar offline directamente cuando no se provee analizador remoto")
    fun testPureOfflineWithoutRemote() = runTest {
        val useCase = EstimateSupplementNutritionUseCase(remoteAnalyzer = null)

        val result = useCase("creatina creapure")
        assertTrue(result.isSuccess)
        val item = result.getOrThrow()
        assertEquals("5g", item.dosageDescription)
        assertEquals(0.0, item.calories)

        val unknownResult = useCase("extracto de alcachofa")
        assertTrue(unknownResult.isSuccess)
        val unknownItem = unknownResult.getOrThrow()
        assertEquals("1 porción", unknownItem.dosageDescription)
        assertEquals(0.0, unknownItem.calories)
    }
}
