package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.data.local.LocalFoodCatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LocalLiteRtVisionAnalyzerTest {

    private val catalogRepository = LocalFoodCatalogRepository()
    private lateinit var analyzer: LocalLiteRtVisionAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = LocalLiteRtVisionAnalyzer(catalogRepository)
    }

    @Test
    @DisplayName("Debe fallar si los bytes de imagen están vacíos")
    fun testEmptyBytesFails() = runTest {
        val result = analyzer.analyzeImage(byteArrayOf())
        assertTrue(result.isFailure)
        assertEquals("Los datos de la imagen están vacíos.", result.exceptionOrNull()?.message)
    }

    @Test
    @DisplayName("No debe inventar pollo ni arroz cuando no hay detección con confianza suficiente")
    fun testNoFabricatedFoodItemsReturned() = runTest {
        // Bytes de JPEG ficticio
        val fakeJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val result = analyzer.analyzeImage(fakeJpegBytes)

        assertTrue(result.isFailure)
        assertEquals(
            "No pudimos identificar con certeza tu comida. Puedes buscarla por nombre o describirla.",
            result.exceptionOrNull()?.message
        )
    }
}
