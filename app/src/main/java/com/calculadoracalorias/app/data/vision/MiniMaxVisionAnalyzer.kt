package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.data.remote.dto.MiniMaxChatRequest
import com.calculadoracalorias.app.data.remote.dto.MiniMaxChatResponse
import com.calculadoracalorias.app.data.remote.dto.MiniMaxDetectedMealDto
import com.calculadoracalorias.app.data.remote.dto.MiniMaxImageUrl
import com.calculadoracalorias.app.data.remote.dto.MiniMaxImageUrlPart
import com.calculadoracalorias.app.data.remote.dto.MiniMaxMessage
import com.calculadoracalorias.app.data.remote.dto.MiniMaxTextPart
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.FoodVisionAnalyzer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.util.Base64
import java.util.UUID

class MiniMaxVisionAnalyzer(
    private val apiKeyProvider: () -> String,
    private val httpClient: HttpClient,
    private val endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions"
) : FoodVisionAnalyzer {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult> {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("No se ha configurado una clave de API para MiniMax Vision."))
        }

        return try {
            val base64Image = Base64.getEncoder().encodeToString(imageBytes)
            val dataUrl = "data:image/jpeg;base64,$base64Image"

            val systemPrompt = """
                Eres un asistente experto en nutrición y reconocimiento de comidas, especializado en gastronomía y porciones de Chile.
                Analiza la imagen adjunta e identifica los alimentos visibles en el plato.
                Debes responder EXCLUSIVAMENTE con un objeto JSON válido (sin explicaciones ni texto adicional) con esta estructura:
                {
                  "suggested_meal_category": "Almuerzo", // o "Desayuno", "Once / Cena", "Colaciones"
                  "detected_items": [
                    {
                      "name": "Nombre del alimento en español de Chile (ej. Marraqueta, Palta hass)",
                      "serving_grams": 150.0,
                      "calories_per_100g": 165.0,
                      "protein_per_100g": 31.0,
                      "carbs_per_100g": 0.0,
                      "fat_per_100g": 3.6,
                      "confidence": 0.95
                    }
                  ]
                }
            """.trimIndent()

            val request = MiniMaxChatRequest(
                model = "MiniMax-VL-01",
                messages = listOf(
                    MiniMaxMessage(
                        role = "system",
                        content = listOf(MiniMaxTextPart(systemPrompt))
                    ),
                    MiniMaxMessage(
                        role = "user",
                        content = listOf(
                            MiniMaxTextPart("Identifica los alimentos y estima sus porciones en gramos para esta comida:"),
                            MiniMaxImageUrlPart(MiniMaxImageUrl(url = dataUrl))
                        )
                    )
                ),
                temperature = 0.1f
            )

            val httpResponse = httpClient.post(endpointUrl) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                val errorBody = try { httpResponse.body<String>() } catch (_: Exception) { "Error de red" }
                return Result.failure(
                    RuntimeException("Error al consultar MiniMax Vision (HTTP ${httpResponse.status.value}): $errorBody")
                )
            }

            val chatResponse = httpResponse.body<MiniMaxChatResponse>()
            val rawContent = chatResponse.choices.firstOrNull()?.message?.content
                ?: return Result.failure(RuntimeException("La respuesta de MiniMax no contiene opciones de respuesta."))

            val mealDto = parseMealDto(rawContent)
            val detectedResult = mapDtoToResult(mealDto)

            Result.success(detectedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseMealDto(rawContent: String): MiniMaxDetectedMealDto {
        val cleaned = rawContent
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val jsonStartIndex = cleaned.indexOf('{')
        val jsonEndIndex = cleaned.lastIndexOf('}')
        val jsonContent = if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
            cleaned.substring(jsonStartIndex, jsonEndIndex + 1)
        } else {
            cleaned
        }

        return jsonParser.decodeFromString(MiniMaxDetectedMealDto.serializer(), jsonContent)
    }

    internal fun mapDtoToResult(dto: MiniMaxDetectedMealDto): DetectedMealResult {
        val category = dto.suggestedMealCategory?.let {
            MealCategory.fromDisplayName(it)
        } ?: MealCategory.ALMUERZO

        val items = dto.detectedItems.map { itemDto ->
            ScannedFoodItem(
                id = UUID.randomUUID().toString(),
                name = itemDto.name,
                servingGrams = itemDto.servingGrams.coerceAtLeast(0.0),
                caloriesPer100g = itemDto.caloriesPer100g.coerceAtLeast(0.0),
                proteinPer100g = itemDto.proteinPer100g.coerceAtLeast(0.0),
                carbsPer100g = itemDto.carbsPer100g.coerceAtLeast(0.0),
                fatPer100g = itemDto.fatPer100g.coerceAtLeast(0.0),
                confidence = itemDto.confidence.coerceIn(0.0f, 1.0f)
            )
        }

        return DetectedMealResult(
            items = items,
            suggestedMealType = category,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )
    }
}
