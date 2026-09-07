package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.VisionSource
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MiniMaxVisionAnalyzerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var analyzer: MiniMaxVisionAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = MiniMaxVisionAnalyzer(
            apiKeyProvider = { "test-api-key-123" },
            httpClient = httpClient
        )
    }

    @Test
    @DisplayName("Debe deserializar correctamente un JSON estructurado de MiniMax")
    fun testParseMealDtoStrictJson() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Almuerzo",
              "detected_items": [
                {
                  "name": "Pechuga de pollo",
                  "serving_grams": 150.0,
                  "calories_per_100g": 165.0,
                  "protein_per_100g": 31.0,
                  "carbs_per_100g": 0.0,
                  "fat_per_100g": 3.6,
                  "confidence": 0.98
                },
                {
                  "name": "Palta hass",
                  "serving_grams": 80.0,
                  "calories_per_100g": 160.0,
                  "protein_per_100g": 2.0,
                  "carbs_per_100g": 9.0,
                  "fat_per_100g": 15.0,
                  "confidence": 0.92
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)

        assertEquals("Almuerzo", dto.suggestedMealCategory)
        assertEquals(2, dto.detectedItems.size)
        assertEquals("Pechuga de pollo", dto.detectedItems[0].name)
        assertEquals(150.0, dto.detectedItems[0].servingGrams)
        assertEquals(165.0, dto.detectedItems[0].caloriesPer100g)
        assertEquals(31.0, dto.detectedItems[0].proteinPer100g)
    }

    @Test
    @DisplayName("Debe limpiar y parsear JSON envuelto en bloques markdown ```json")
    fun testParseMealDtoWithMarkdownFence() {
        val markdownPayload = """
            ```json
            {
              "suggested_meal_category": "Once / Cena",
              "detected_items": [
                {
                  "name": "Marraqueta con palta",
                  "serving_grams": 120.0,
                  "calories_per_100g": 220.0,
                  "protein_per_100g": 6.0,
                  "carbs_per_100g": 32.0,
                  "fat_per_100g": 8.0,
                  "confidence": 0.95
                }
              ]
            }
            ```
        """.trimIndent()

        val dto = analyzer.parseMealDto(markdownPayload)

        assertEquals("Once / Cena", dto.suggestedMealCategory)
        assertEquals(1, dto.detectedItems.size)
        assertEquals("Marraqueta con palta", dto.detectedItems[0].name)
    }

    @Test
    @DisplayName("Debe mapear el DTO a DetectedMealResult con categorías y cálculo correcto")
    fun testMapDtoToResult() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Once / Cena",
              "detected_items": [
                {
                  "name": "Marraqueta",
                  "serving_grams": 100.0,
                  "calories_per_100g": 280.0,
                  "protein_per_100g": 9.0,
                  "carbs_per_100g": 56.0,
                  "fat_per_100g": 1.0,
                  "confidence": 0.95
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)
        val result = analyzer.mapDtoToResult(dto)

        assertEquals(MealCategory.ONCE_CENA, result.suggestedMealType)
        assertEquals(VisionSource.MINIMAX_CLOUD, result.analysisSource)
        assertEquals(1, result.items.size)
        val item = result.items[0]
        assertNotNull(item.id)
        assertEquals("Marraqueta", item.name)
        assertEquals(280.0, item.totalCalories)
        assertEquals(9.0, item.totalProtein)
        assertEquals(56.0, item.totalCarbs)
        assertEquals(1.0, item.totalFat)
    }

    @Test
    @DisplayName("Debe retornar fallo si la API Key está vacía")
    fun testAnalyzeWithEmptyApiKeyFails() = runBlocking {
        val unconfiguredAnalyzer = MiniMaxVisionAnalyzer(
            apiKeyProvider = { "" },
            httpClient = httpClient
        )

        val result = unconfiguredAnalyzer.analyzeImage(byteArrayOf(1, 2, 3))

        assertTrue(result.isFailure)
        assertEquals("No se ha configurado una clave de API para MiniMax Vision.", result.exceptionOrNull()?.message)
    }
}
