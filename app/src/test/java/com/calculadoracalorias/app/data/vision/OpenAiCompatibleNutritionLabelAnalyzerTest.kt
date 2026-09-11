package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OpenAiCompatibleNutritionLabelAnalyzerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var analyzer: OpenAiCompatibleNutritionLabelAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = OpenAiCompatibleNutritionLabelAnalyzer(
            configProvider = {
                AiConfiguration(
                    provider = AiProvider.MINIMAX,
                    apiKey = "test-api-key"
                )
            },
            httpClient = httpClient
        )
    }

    @Test
    @DisplayName("Debe fallar si la clave de API está vacía")
    fun testEmptyApiKeyFails() = runTest {
        val emptyConfigAnalyzer = OpenAiCompatibleNutritionLabelAnalyzer(
            configProvider = {
                AiConfiguration(
                    provider = AiProvider.MINIMAX,
                    apiKey = ""
                )
            },
            httpClient = httpClient
        )

        val result = emptyConfigAnalyzer.analyzeNutritionLabel(byteArrayOf(1, 2, 3))
        assertTrue(result.isFailure)
        assertEquals("No se ha configurado una clave de API para MiniMax Vision.", result.exceptionOrNull()?.message)
    }
}
