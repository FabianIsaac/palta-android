package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.data.remote.AiDebugLogManager
import com.calculadoracalorias.app.data.remote.LlmResponseSanitizer
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatResponse
import com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrl
import com.calculadoracalorias.app.data.remote.dto.OpenAiImageUrlPart
import com.calculadoracalorias.app.data.remote.dto.OpenAiMessage
import com.calculadoracalorias.app.data.remote.dto.OpenAiNutritionLabelDto
import com.calculadoracalorias.app.data.remote.dto.OpenAiTextPart
import com.calculadoracalorias.app.domain.model.AiCallLogEntry
import com.calculadoracalorias.app.domain.model.AiCallType
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.NutritionLabelScanResult
import com.calculadoracalorias.app.domain.repository.NutritionLabelAnalyzer
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

/**
 * Analizador multimodal de tablas nutricionales compatible con proveedores de IA basados en OpenAI Chat Completions.
 */
open class OpenAiCompatibleNutritionLabelAnalyzer(
    private val configProvider: () -> AiConfiguration,
    private val httpClient: HttpClient,
    private val debugLogManager: AiDebugLogManager = AiDebugLogManager
) : NutritionLabelAnalyzer {

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

    override suspend fun analyzeNutritionLabel(imageBytes: ByteArray): Result<NutritionLabelScanResult> {
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
                Eres un asistente experto en rotulado nutricional y análisis de tablas de alimentos y suplementos.
                Analiza la imagen de la etiqueta nutricional o envase adjunto y extrae la información relevante.
                
                REGLAS IMPORTANTES:
                1. PRIORIDAD EN PORCIÓN: Si la tabla muestra valores "Por 100g" y "Por Porción", debes extraer ESTRICTAMENTE los valores por porción de consumo habitual.
                2. FILTRADO DE RUIDO: Ignora porcentajes de valor diario (% DV), vitaminas, minerales y sodio. Enfócate en calorías (kcal) y macronutrientes en gramos (proteínas, carbohidratos, grasas).
                3. NOMBRE DEL PRODUCTO: Si el título o marca del producto es visible en el envase o etiqueta, inclúyelo en "product_name".
                4. DESCRIPCIÓN DE PORCIÓN: Indica la porción de forma clara en "serving_description" (ej: "1 scoop (33g)", "1 porción (30g)", "2 tabletas").
                
                Debes responder EXCLUSIVAMENTE con un objeto JSON válido (sin explicaciones, comentarios ni bloques Markdown adicionales) con esta estructura exacta:
                {
                  "product_name": "Proteína Whey Vainilla",
                  "serving_description": "1 scoop (33g)",
                  "serving_grams": 33.0,
                  "calories": 110.0,
                  "protein": 25.0,
                  "carbs": 1.0,
                  "fat": 1.0
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
                            OpenAiTextPart("Analiza esta tabla nutricional y extrae los datos por porción:"),
                            OpenAiImageUrlPart(OpenAiImageUrl(url = dataUrl))
                        )
                    )
                ),
                temperature = 0.1f
            )

            val startTime = System.currentTimeMillis()
            val httpResponse = httpClient.post(config.effectiveEndpointUrl) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val duration = System.currentTimeMillis() - startTime
            val statusCode = httpResponse.status.value

            if (!httpResponse.status.isSuccess()) {
                val errorBody = try { httpResponse.body<String>() } catch (_: Exception) { "Error de red" }
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.NUTRITION_LABEL,
                        provider = config.provider,
                        model = config.effectiveVisionModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = "[Etiqueta nutricional: ${imageBytes.size} bytes]",
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = false,
                        rawResponse = errorBody,
                        errorMessage = "HTTP $statusCode: $errorBody"
                    )
                )
                return Result.failure(
                    RuntimeException("Error al consultar ${config.provider.displayName} (HTTP $statusCode): $errorBody")
                )
            }

            val chatResponse = httpResponse.body<OpenAiChatResponse>()
            val rawContent = chatResponse.choices.firstOrNull()?.message?.content
            if (rawContent == null) {
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.NUTRITION_LABEL,
                        provider = config.provider,
                        model = config.effectiveVisionModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = "[Etiqueta nutricional: ${imageBytes.size} bytes]",
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = false,
                        errorMessage = "La respuesta del proveedor de IA no contiene datos."
                    )
                )
                return Result.failure(RuntimeException("La respuesta del proveedor de IA no contiene datos."))
            }

            val sanitized = LlmResponseSanitizer.sanitizeJsonResponse(rawContent)
            val dto = jsonParser.decodeFromString<OpenAiNutritionLabelDto>(sanitized)

            val scanResult = NutritionLabelScanResult(
                productName = dto.productName?.takeIf { it.isNotBlank() },
                servingDescription = dto.servingDescription?.takeIf { it.isNotBlank() } ?: "1 porción",
                servingGrams = dto.servingGrams?.coerceAtLeast(0.0),
                calories = dto.calories.coerceAtLeast(0.0),
                proteinGrams = dto.protein.coerceAtLeast(0.0),
                carbsGrams = dto.carbs.coerceAtLeast(0.0),
                fatGrams = dto.fat.coerceAtLeast(0.0)
            )

            debugLogManager.log(
                AiCallLogEntry(
                    callType = AiCallType.NUTRITION_LABEL,
                    provider = config.provider,
                    model = config.effectiveVisionModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    promptSummary = "[Etiqueta nutricional: ${imageBytes.size} bytes]",
                    httpStatus = statusCode,
                    durationMs = duration,
                    isSuccess = true,
                    rawResponse = rawContent
                )
            )

            Result.success(scanResult)
        } catch (e: Exception) {
            debugLogManager.log(
                AiCallLogEntry(
                    callType = AiCallType.NUTRITION_LABEL,
                    provider = config.provider,
                    model = config.effectiveVisionModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    promptSummary = "[Etiqueta nutricional: ${imageBytes.size} bytes]",
                    httpStatus = null,
                    durationMs = 0L,
                    isSuccess = false,
                    errorMessage = e.message ?: e.toString()
                )
            )
            Result.failure(e)
        }
    }
}
