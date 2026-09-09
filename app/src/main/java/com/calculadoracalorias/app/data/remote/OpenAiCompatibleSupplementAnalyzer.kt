package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatResponse
import com.calculadoracalorias.app.data.remote.dto.OpenAiMessage
import com.calculadoracalorias.app.data.remote.dto.OpenAiSupplementEstimateDto
import com.calculadoracalorias.app.data.remote.dto.OpenAiTextPart
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.SupplementNutritionEstimate
import com.calculadoracalorias.app.domain.repository.SupplementNutritionAnalyzer
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

/**
 * Analizador universal de información nutricional de suplementos deportivos
 * compatible con proveedores de IA basados en OpenAI Chat Completions.
 */
open class OpenAiCompatibleSupplementAnalyzer(
    private val configProvider: () -> AiConfiguration,
    private val httpClient: HttpClient
) : SupplementNutritionAnalyzer {

    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-Text-01"
    ) : this(
        configProvider = {
            AiConfiguration(
                provider = AiProvider.MINIMAX,
                apiKey = apiKeyProvider(),
                customEndpointUrl = endpointUrl,
                customTextModel = model
            )
        },
        httpClient = httpClient
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun estimateSupplement(query: String): Result<SupplementNutritionEstimate> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa un suplemento a analizar."))
        }

        val config = configProvider()
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Ingresa tu clave de API en Ajustes para usar este proveedor de IA."))
        }

        return try {
            val systemPrompt = """
                Eres un asistente experto en nutrición y suplementación deportiva.
                El usuario ingresará el nombre o descripción de un suplemento (ej: "Creatina monohidrato", "Whey Protein Isolate", "Citrato de magnesio", "Omega 3").
                
                Debes determinar de forma precisa:
                1. "name": Nombre formal del suplemento en español.
                2. "dosage_description": Dosis sugerida típica (ej: "5g", "1 scoop (30g)", "2 cápsulas", "400mg", "1 comprimido").
                3. "calories": Calorías por esa dosis (Double). Si es vitamina, mineral, creatina u aminoácido no calórico, DEBE ser 0.0.
                4. "protein_grams": Proteína en gramos por esa dosis (Double).
                5. "carbs_grams": Carbohidratos en gramos por esa dosis (Double).
                6. "fat_grams": Grasas en gramos por esa dosis (Double).
                
                Responde ÚNICAMENTE con un objeto JSON válido con la siguiente estructura, sin formato Markdown ni explicaciones adicionales:
                {
                  "name": "Creatina Monohidrato",
                  "dosage_description": "5g",
                  "calories": 0.0,
                  "protein_grams": 0.0,
                  "carbs_grams": 0.0,
                  "fat_grams": 0.0
                }
            """.trimIndent()

            val request = OpenAiChatRequest(
                model = config.effectiveTextModel,
                messages = listOf(
                    OpenAiMessage(
                        role = "system",
                        content = listOf(OpenAiTextPart(systemPrompt))
                    ),
                    OpenAiMessage(
                        role = "user",
                        content = listOf(OpenAiTextPart("Suplemento a evaluar: $trimmed"))
                    )
                ),
                temperature = 0.1f
            )

            val httpResponse = httpClient.post(config.effectiveEndpointUrl) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(request)
            }

            if (!httpResponse.status.isSuccess()) {
                return Result.failure(RuntimeException("Error en proveedor de IA (${config.provider.displayName}): HTTP ${httpResponse.status.value}"))
            }

            val responseBody = httpResponse.body<OpenAiChatResponse>()
            val choice = responseBody.choices.firstOrNull()
                ?: return Result.failure(RuntimeException("Respuesta vacía del proveedor de IA."))

            val content = LlmResponseSanitizer.sanitizeJsonResponse(choice.message.content)
            val parsedDto = jsonParser.decodeFromString<OpenAiSupplementEstimateDto>(content)

            Result.success(
                SupplementNutritionEstimate(
                    name = parsedDto.name?.takeIf { it.isNotBlank() } ?: trimmed,
                    dosageDescription = parsedDto.dosageDescription?.takeIf { it.isNotBlank() } ?: "1 porción",
                    calories = parsedDto.calories.coerceAtLeast(0.0),
                    proteinGrams = parsedDto.proteinGrams.coerceAtLeast(0.0),
                    carbsGrams = parsedDto.carbsGrams.coerceAtLeast(0.0),
                    fatGrams = parsedDto.fatGrams.coerceAtLeast(0.0)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
