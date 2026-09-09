package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.data.local.LocalFoodCatalogRepository
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.VisionSource
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OpenAiCompatibleMealAnalyzerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val catalogRepository = LocalFoodCatalogRepository()
    private lateinit var analyzer: OpenAiCompatibleMealAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = OpenAiCompatibleMealAnalyzer(
            configProvider = {
                AiConfiguration(
                    provider = AiProvider.NVIDIA_NIM,
                    apiKey = "nvapi-test-key"
                )
            },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )
    }

    @Test
    @DisplayName("Debe deserializar JSON estructurado con porciones caseras correctamente")
    fun testParseMealDtoWithHouseholdUnits() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Desayuno",
              "detected_items": [
                {
                  "name": "Café negro",
                  "serving_grams": 200.0,
                  "calories_per_100g": 2.0,
                  "protein_per_100g": 0.1,
                  "carbs_per_100g": 0.2,
                  "fat_per_100g": 0.0,
                  "confidence": 0.98,
                  "household_unit": "taza",
                  "household_quantity": 1.0
                },
                {
                  "name": "Marraqueta",
                  "serving_grams": 100.0,
                  "calories_per_100g": 265.0,
                  "protein_per_100g": 8.5,
                  "carbs_per_100g": 54.0,
                  "fat_per_100g": 1.2,
                  "confidence": 0.95,
                  "household_unit": "unidad",
                  "household_quantity": 1.0
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)

        assertEquals("Desayuno", dto.suggestedMealCategory)
        assertEquals(2, dto.detectedItems.size)
        assertEquals("Café negro", dto.detectedItems[0].name)
        assertEquals(200.0, dto.detectedItems[0].servingGrams)
        assertEquals("taza", dto.detectedItems[0].householdUnit)
    }

    @Test
    @DisplayName("Debe sanitizar etiquetas de razonamiento think de DeepSeek R1 / Qwen y bloques markdown")
    fun testParseMealDtoWithThinkTagsAndMarkdown() {
        val rawLlmResponse = """
            <think>
            El usuario pidió marraqueta con palta y huevo.
            Calculamos calorías de marraqueta (~265 kcal/100g), palta (~160 kcal/100g) y huevo revuelto (~150 kcal/100g).
            Formateamos JSON estricto.
            </think>
            ```json
            {
              "suggested_meal_category": "Once / Cena",
              "detected_items": [
                {
                  "name": "Marraqueta",
                  "serving_grams": 100.0,
                  "calories_per_100g": 265.0,
                  "protein_per_100g": 8.5,
                  "carbs_per_100g": 54.0,
                  "fat_per_100g": 1.2,
                  "confidence": 0.95
                },
                {
                  "name": "Palta hass",
                  "serving_grams": 50.0,
                  "calories_per_100g": 160.0,
                  "protein_per_100g": 2.0,
                  "carbs_per_100g": 8.5,
                  "fat_per_100g": 14.7,
                  "confidence": 0.92
                }
              ]
            }
            ```
        """.trimIndent()

        val dto = analyzer.parseMealDto(rawLlmResponse)

        assertEquals("Once / Cena", dto.suggestedMealCategory)
        assertEquals(2, dto.detectedItems.size)
        assertEquals("Marraqueta", dto.detectedItems[0].name)
        assertEquals("Palta hass", dto.detectedItems[1].name)
    }

    @Test
    @DisplayName("mapDtoToResult debe asignar la fuente de visión correcta del proveedor activo")
    fun testMapDtoToResultWithProviderSource() {
        val dto = analyzer.parseMealDto("""{"suggested_meal_category": "Almuerzo", "detected_items": []}""")
        val result = analyzer.mapDtoToResult(dto, VisionSource.NVIDIA_NIM)

        assertEquals(MealCategory.ALMUERZO, result.suggestedMealType)
        assertEquals(VisionSource.NVIDIA_NIM, result.analysisSource)
    }

    @Test
    @DisplayName("Fallback local cuando no hay clave API configurada")
    fun testLocalFallbackWhenNoApiKey() = runTest {
        val analyzerWithoutKey = OpenAiCompatibleMealAnalyzer(
            configProvider = {
                AiConfiguration(
                    provider = AiProvider.NVIDIA_NIM,
                    apiKey = ""
                )
            },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )

        val result = analyzerWithoutKey.analyzeTextDescription("Dos huevos revueltos con marraqueta")
        assertTrue(result.isSuccess)
        val meal = result.getOrNull()
        assertNotNull(meal)
        assertEquals(VisionSource.LOCAL_DEVICE, meal?.analysisSource)
    }
}
