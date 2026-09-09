package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.data.remote.LlmResponseSanitizer
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatResponse
import com.calculadoracalorias.app.data.remote.dto.OpenAiDetectedMealDto
import com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrl
import com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrlPart
import com.calculadoracalorias.app.data.remote.dto.OpenAiMessage
import com.calculadoracalorias.app.data.remote.dto.OpenAiTextPart
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.HouseholdPortion
import com.calculadoracalorias.app.domain.model.HouseholdUnit
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

/**
 * Analizador universal de fotos de comida compatible con la especificación multimodal de OpenAI
 * (compatible con NVIDIA NIM Llama-3.2-Vision, Google Gemini OpenAI endpoint, MiniMax y Custom).
 */
open class OpenAiCompatibleVisionAnalyzer(
    private val configProvider: () -> AiConfiguration,
    private val httpClient: HttpClient
) : FoodVisionAnalyzer {

    // Constructor de conveniencia retrocompatible
    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-M3"
    ) : this(
        configProvider = {
            AiConfiguration(
                provider = AiProvider.MINIMAX,
                apiKey = apiKeyProvider(),
                customEndpointUrl = endpointUrl,
                customVisionModel = model
            )
        },
        httpClient = httpClient
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult> {
        val config = configProvider()
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            val message = if (config.provider == AiProvider.MINIMAX) {
                "No se ha configurado una clave de API para MiniMax Vision."
            } else {
                "Ingresa tu clave de API en Ajustes para usar este proveedor de IA."
            }
            return Result.failure(IllegalStateException(message))
        }

        return try {
            val base64Image = Base64.getEncoder().encodeToString(imageBytes)
            val dataUrl = "data:image/jpeg;base64,$base64Image"

            val systemPrompt = """
                Eres un asistente experto en nutrición y reconocimiento de comidas, especializado en gastronomía, bebidas y porciones de Chile.
                Analiza la imagen adjunta e identifica con precisión todos los alimentos y bebidas visibles en platos, tazas, tazones, pocillos o vasos (incluso si la taza o vaso es de vidrio o transparente).
                Presta especial atención a bebidas calientes o frías e infusiones cotidianas: café negro, café con leche, té, leche, jugos, así como a desayunos y onces chilenas (marraqueta, hallulla, palta hass, huevos revueltos, etc.).
                Si observas una taza con líquido oscuro, reconócelo como café negro o té; si tiene color café claro, como café con leche o té con leche.
                Debes responder EXCLUSIVAMENTE con un objeto JSON válido (sin explicaciones, comentarios ni bloques adicionales) con esta estructura:
                {
                  "suggested_meal_category": "Desayuno", // o "Almuerzo", "Once / Cena", "Colaciones"
                  "detected_items": [
                    {
                      "name": "Nombre en español de Chile (ej. Café negro, Café con leche, Té, Marraqueta, Palta hass)",
                      "serving_grams": 200.0,
                      "calories_per_100g": 2.0,
                      "protein_per_100g": 0.1,
                      "carbs_per_100g": 0.2,
                      "fat_per_100g": 0.0,
                      "confidence": 0.95,
                      "household_unit": "taza",
                      "household_quantity": 1.0
                    }
                  ]
                }
            """.trimIndent()

            val request = OpenAiChatRequest(
                model = config.effectiveVisionModel,
                messages = listOf(
                    OpenAiMessage(
                        role = "system",
                        content = listOf(OpenAiTextPart(systemPrompt))
                    ),
                    OpenAiMessage(
                        role = "user",
                        content = listOf(
                            OpenAiTextPart("Identifica los alimentos y estima sus porciones en gramos para esta comida:"),
                            OpenAiImageUrlPart(OpenAiImageUrl(url = dataUrl))
                        )
                    )
                ),
                temperature = 0.1f
            )

            val httpResponse = httpClient.post(config.effectiveEndpointUrl) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                val errorBody = try { httpResponse.body<String>() } catch (_: Exception) { "Error de red" }
                return Result.failure(
                    RuntimeException("Error al consultar ${config.provider.displayName} (HTTP ${httpResponse.status.value}): $errorBody")
                )
            }

            val chatResponse = httpResponse.body<OpenAiChatResponse>()
            val rawContent = chatResponse.choices.firstOrNull()?.message?.content
                ?: return Result.failure(RuntimeException("La respuesta del proveedor de IA no contiene opciones de respuesta."))

            val mealDto = parseMealDto(rawContent)
            val detectedResult = mapDtoToResult(mealDto, config.provider.toVisionSource())

            Result.success(detectedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseMealDto(rawContent: String): OpenAiDetectedMealDto {
        val sanitized = LlmResponseSanitizer.sanitizeJsonResponse(rawContent)
        return jsonParser.decodeFromString(OpenAiDetectedMealDto.serializer(), sanitized)
    }

    internal fun mapDtoToResult(
        dto: OpenAiDetectedMealDto,
        source: VisionSource = VisionSource.MINIMAX_CLOUD
    ): DetectedMealResult {
        val category = dto.suggestedMealCategory?.let {
            MealCategory.fromDisplayName(it)
        } ?: MealCategory.ALMUERZO

        val items = dto.detectedItems.map { itemDto ->
            val unit = itemDto.householdUnit?.let { HouseholdUnit.fromDisplayName(it) }
            val quantity = itemDto.householdQuantity ?: 1.0
            val servingGrams = itemDto.servingGrams.coerceAtLeast(0.0)

            val householdPortion = if (unit != null) {
                HouseholdPortion(
                    unit = unit,
                    quantity = quantity,
                    equivalentGrams = servingGrams
                )
            } else null

            ScannedFoodItem(
                id = UUID.randomUUID().toString(),
                name = itemDto.name,
                servingGrams = servingGrams,
                caloriesPer100g = itemDto.caloriesPer100g.coerceAtLeast(0.0),
                proteinPer100g = itemDto.proteinPer100g.coerceAtLeast(0.0),
                carbsPer100g = itemDto.carbsPer100g.coerceAtLeast(0.0),
                fatPer100g = itemDto.fatPer100g.coerceAtLeast(0.0),
                confidence = itemDto.confidence.coerceIn(0.0f, 1.0f),
                householdPortion = householdPortion
            )
        }

        return DetectedMealResult(
            items = items,
            suggestedMealType = category,
            analysisSource = source
        )
    }
}
