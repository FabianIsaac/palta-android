package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.VisionSource
import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OpenAiCompatibleVisionAnalyzerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private lateinit var analyzer: OpenAiCompatibleVisionAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = OpenAiCompatibleVisionAnalyzer(
            configProvider = {
                AiConfiguration(
                    provider = AiProvider.GOOGLE_GEMINI,
                    apiKey = "test-gemini-key"
                )
            },
            httpClient = httpClient
        )
    }

    @Test
    @DisplayName("Debe deserializar JSON válido y mapear porciones caseras")
    fun testParseMealDto() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Almuerzo",
              "detected_items": [
                {
                  "name": "Cazuela de ave",
                  "serving_grams": 350.0,
                  "calories_per_100g": 80.0,
                  "protein_per_100g": 6.5,
                  "carbs_per_100g": 7.0,
                  "fat_per_100g": 3.0,
                  "confidence": 0.96,
                  "household_unit": "plato",
                  "household_quantity": 1.0
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)
        assertEquals("Almuerzo", dto.suggestedMealCategory)
        assertEquals(1, dto.detectedItems.size)
        assertEquals("Cazuela de ave", dto.detectedItems[0].name)
        assertEquals(350.0, dto.detectedItems[0].servingGrams)
        assertEquals("plato", dto.detectedItems[0].householdUnit)
    }

    @Test
    @DisplayName("Debe limpiar bloques think y Markdown en respuestas de visión")
    fun testSanitizeVisionResponse() {
        val rawResponse = """
            <think>
            Vision analysis of image: Plate contains cazuela with corn, chicken breast, potato and squash.
            Estimating portions in Chilean standard.
            </think>
            ```json
            {
              "suggested_meal_category": "Almuerzo",
              "detected_items": [
                {
                  "name": "Cazuela de ave",
                  "serving_grams": 350.0,
                  "calories_per_100g": 80.0,
                  "protein_per_100g": 6.5,
                  "carbs_per_100g": 7.0,
                  "fat_per_100g": 3.0,
                  "confidence": 0.94
                }
              ]
            }
            ```
        """.trimIndent()

        val dto = analyzer.parseMealDto(rawResponse)
        val result = analyzer.mapDtoToResult(dto, VisionSource.GOOGLE_GEMINI)

        assertEquals(MealCategory.ALMUERZO, result.suggestedMealType)
        assertEquals(VisionSource.GOOGLE_GEMINI, result.analysisSource)
        assertEquals(1, result.items.size)
        assertEquals("Cazuela de ave", result.items[0].name)
    }

    @Test
    @DisplayName("Debe serializar request multimodal con OpenAI format")
    fun testMultimodalRequestStructure() {
        val request = com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest(
            model = "meta/llama-3.2-11b-vision-instruct",
            messages = listOf(
                com.calculadoracalorias.app.data.remote.dto.OpenAiMessage(
                    role = "user",
                    content = listOf(
                        com.calculadoracalorias.app.data.remote.dto.OpenAiTextPart("Analiza"),
                        com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrlPart(
                            com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrl("data:image/jpeg;base64,sample")
                        )
                    )
                )
            )
        )
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest.serializer(), request)

        assertTrue(encoded.contains("\"model\":\"meta/llama-3.2-11b-vision-instruct\""))
        assertTrue(encoded.contains("\"type\":\"image_url\""))
        assertTrue(encoded.contains("\"url\":\"data:image/jpeg;base64,sample\""))
    }
}
