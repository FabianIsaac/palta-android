package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.data.remote.dto.OpenAiChatRequest
import com.calculadoracalorias.app.data.remote.dto.OpenAiChatResponse
import com.calculadoracalorias.app.data.remote.dto.OpenAiDetectedMealDto
import com.calculadoracalorias.app.data.remote.dto.OpenAiMessage
import com.calculadoracalorias.app.data.remote.dto.OpenAiTextPart
import com.calculadoracalorias.app.domain.model.AiCallLogEntry
import com.calculadoracalorias.app.domain.model.AiCallType
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.AiServiceException
import com.calculadoracalorias.app.domain.model.AiTechnicalDetails
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.HouseholdPortion
import com.calculadoracalorias.app.domain.model.HouseholdUnit
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer
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
import java.util.Locale
import java.util.UUID

/**
 * Analizador universal de comidas por lenguaje natural compatible con la API de OpenAI
 * (soporta NVIDIA NIM, Google Gemini OpenAI-endpoint, MiniMax y servidores personalizados/locales).
 */
open class OpenAiCompatibleMealAnalyzer(
    private val configProvider: () -> AiConfiguration,
    private val httpClient: HttpClient,
    private val catalogRepository: FoodCatalogRepository? = null,
    private val debugLogManager: AiDebugLogManager = AiDebugLogManager
) : NaturalLanguageMealAnalyzer {

    // Constructor de conveniencia para compatibilidad con llamadas directas
    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        catalogRepository: FoodCatalogRepository? = null,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-M2.7-highspeed"
    ) : this(
        configProvider = {
            AiConfiguration(
                provider = AiProvider.MINIMAX,
                apiKey = apiKeyProvider(),
                customEndpointUrl = endpointUrl,
                customTextModel = model
            )
        },
        httpClient = httpClient,
        catalogRepository = catalogRepository
    )

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun analyzeTextDescription(description: String): Result<DetectedMealResult> {
        val trimmed = description.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa una descripción de tu comida."))
        }

        val config = configProvider()

        var remoteError: Throwable? = null
        if (config.provider.isCloud && config.apiKey.isNotBlank()) {
            val remoteResult = analyzeWithCloudLlm(trimmed, config)
            if (remoteResult.isSuccess) {
                return remoteResult
            } else {
                remoteError = remoteResult.exceptionOrNull()
            }
        }

        // Fallback al catálogo local si no hay clave, no es nube o falló la llamada remota
        if (catalogRepository != null) {
            val localResult = analyzeWithLocalCatalog(trimmed)
            if (localResult.isSuccess) {
                val technicalError = (remoteError as? AiServiceException)?.technicalDetails
                return Result.success(
                    localResult.getOrThrow().copy(technicalError = technicalError)
                )
            }
        }

        return if (remoteError != null) {
            Result.failure(remoteError)
        } else if (config.provider.isCloud && config.apiKey.isBlank()) {
            Result.failure(IllegalStateException("Ingresa tu clave de API en Ajustes para usar este proveedor de IA."))
        } else {
            Result.failure(RuntimeException("No se pudo interpretar la descripción de los alimentos."))
        }
    }

    private suspend fun analyzeWithCloudLlm(description: String, config: AiConfiguration): Result<DetectedMealResult> {
        val startTime = System.currentTimeMillis()
        return try {
            val systemPrompt = """
                Eres un asistente nutricional experto adaptado a Chile.
                El usuario describirá lo que comió o bebió en lenguaje cotidiano chileno (ej: "un café con leche y una marraqueta con palta", "dos huevos revueltos", "2 fajitas con carne molida, choclo y lechuga", "marraqueta con ave mayo").

                REGLAS ESTRICTAS DE DESCOMPOSICIÓN DE INGREDIENTES:
                1. REGLA OBLIGATORIA PARA PASTAS, RELLENOS Y MEZCLAS CON SALSAS:
                   - Si el usuario menciona una preparación compuesta o pasta donde se mezcla una proteína o vegetal con una salsa o aderezo calórico (ej: ave mayo, atún mayo, huevo con mayo, papas mayo, sándwiches con salsa, ensaladas con aliño pesado):
                     a) NUNCA combines la salsa con la proteína o base en un solo ítem.
                     b) NUNCA omitas la salsa: la mayonesa y los aderezos grasos aportan alta densidad calórica (~680 kcal/100g).
                     c) DEBES generar SIEMPRE dos o más ítems independientes en detected_items:
                        * La proteína o base: ej. "Pechuga de pollo cocida desmenuzada" (~60g - 80g) o "Atún al agua" o "Huevo cocido picado".
                        * La salsa o grasa: ej. "Mayonesa" (~15g - 25g, 1 a 2 cucharadas soperas, ~680 kcal/100g, 75g grasa).
                2. DESCOMPOSICIÓN DE PLATOS COMPUESTOS O ARMADOS:
                   - Si el usuario menciona una preparación armada detallando sus componentes (ej: fajitas, tacos, sándwiches, ensaladas, bowls, burritos), NUNCA crees un solo ítem genérico combinado.
                   - DEBES generar un ítem independiente en detected_items para:
                     a) La base del plato (ej: "Tortillas de fajita", "Pan marraqueta", "Masa de taco").
                     b) CADA proteína, vegetal, relleno, salsa o aderezo nombrado explícitamente (ej: "Carne molida", "Choclo", "Tomate picado", "Lechuga", "Yogurt griego", "Tajín", "Mayonesa").
                3. ESTIMACIÓN COHERENTE DE PORCIONES:
                   - Si el usuario indica cantidad para el conjunto (ej. "2 fajitas con..."), reparte porciones realistas que correspondan a esa cantidad total:
                     * Base: 2 unidades de tortilla (~80g en total, 40g c/u).
                     * Proteína: Porción típica de relleno (~100g de carne molida o pollo).
                     * Vegetales: ~30g a 50g por vegetal mencionado.
                     * Salsas/aderezos: ~30g a 50g (ej. 2 cucharadas de yogurt griego o mayonesa).
                     * Condimentos: ~2g a 5g (ej. 1 cucharadita de tajín).
                4. PLATOS TÍPICOS CERRADOS SIN INGREDIENTES DETALLADOS:
                   - Si el usuario solo nombra un plato tradicional sin detallar ingredientes (ej: "una cazuela de ave", "un plato de porotos con riendas"), manténlo como un solo ítem consolidado.
                5. BEBIDAS E INFUSIONES:
                   - Si es una infusión simple sin azúcar (café negro, té), las calorías son insignificantes (~2 kcal). Si tiene azúcar (1 cdta ~ 5g, 20 kcal) o endulzante, regístrala como ítem separado.
                6. CATEGORÍA:
                   - Sugerir la categoría de comida chilena más apropiada: "Desayuno", "Almuerzo", "Once / Cena", o "Colaciones".

                Responde EXCLUSIVAMENTE con un JSON válido con la siguiente estructura (sin texto adicional ni comillas invertidas de código):
                {
                  "suggested_meal_category": "Once / Cena",
                  "detected_items": [
                    {
                      "name": "Tortillas de fajita",
                      "serving_grams": 80.0,
                      "calories_per_100g": 300.0,
                      "protein_per_100g": 8.0,
                      "carbs_per_100g": 52.0,
                      "fat_per_100g": 6.0,
                      "confidence": 0.95,
                      "household_unit": "unidad",
                      "household_quantity": 2.0
                    },
                    {
                      "name": "Carne molida cocida",
                      "serving_grams": 100.0,
                      "calories_per_100g": 250.0,
                      "protein_per_100g": 26.0,
                      "carbs_per_100g": 0.0,
                      "fat_per_100g": 15.0,
                      "confidence": 0.95,
                      "household_unit": "porción",
                      "household_quantity": 1.0
                    }
                  ]
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
                        content = listOf(
                            OpenAiTextPart("Interpreta esta descripción de comida y extrae sus alimentos, porciones y nutrientes: \"$description\"")
                        )
                    )
                ),
                temperature = 0.1f
            )

            val httpResponse = httpClient.post(config.effectiveEndpointUrl) {
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val duration = System.currentTimeMillis() - startTime
            val statusCode = httpResponse.status.value

            if (!httpResponse.status.isSuccess()) {
                val errorBody = try { httpResponse.body<String>() } catch (_: Exception) { "Error de red" }
                val technicalDetails = AiTechnicalDetails(
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    httpStatus = statusCode,
                    errorBody = errorBody,
                    durationMs = duration
                )
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.MEAL_TEXT,
                        provider = config.provider,
                        model = config.effectiveTextModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = description,
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = false,
                        rawResponse = errorBody,
                        errorMessage = "HTTP $statusCode: $errorBody"
                    )
                )
                logError("Fallo en llamada a IA (${config.provider.displayName}, HTTP $statusCode): $errorBody")
                return Result.failure(
                    AiServiceException(
                        "Error al consultar el proveedor de IA (${config.provider.displayName}, HTTP $statusCode): $errorBody",
                        technicalDetails
                    )
                )
            }

            val chatResponse = httpResponse.body<OpenAiChatResponse>()
            val rawContent = chatResponse.choices.firstOrNull()?.message?.content
            if (rawContent == null) {
                val technicalDetails = AiTechnicalDetails(
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    httpStatus = statusCode,
                    errorBody = "Respuesta vacía o sin elecciones",
                    durationMs = duration
                )
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.MEAL_TEXT,
                        provider = config.provider,
                        model = config.effectiveTextModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = description,
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = false,
                        errorMessage = "La respuesta de la IA no contiene opciones de respuesta."
                    )
                )
                logError("Respuesta de IA sin contenido (${config.provider.displayName})")
                return Result.failure(
                    AiServiceException("La respuesta de la IA no contiene opciones de respuesta.", technicalDetails)
                )
            }

            val mealDto = parseMealDto(rawContent)
            val detectedResult = mapDtoToResult(mealDto, config.provider.toVisionSource())

            debugLogManager.log(
                AiCallLogEntry(
                    callType = AiCallType.MEAL_TEXT,
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    promptSummary = description,
                    httpStatus = statusCode,
                    durationMs = duration,
                    isSuccess = true,
                    rawResponse = rawContent
                )
            )
            logInfo("Llamada a IA exitosa (${config.provider.displayName} / ${config.effectiveTextModel}) en ${duration}ms")

            Result.success(detectedResult)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val technicalDetails = AiTechnicalDetails(
                provider = config.provider,
                model = config.effectiveTextModel,
                endpointUrl = config.effectiveEndpointUrl,
                httpStatus = null,
                errorBody = null,
                exceptionMessage = e.message ?: e.toString(),
                durationMs = duration
            )
            debugLogManager.log(
                AiCallLogEntry(
                    callType = AiCallType.MEAL_TEXT,
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    promptSummary = description,
                    httpStatus = null,
                    durationMs = duration,
                    isSuccess = false,
                    errorMessage = e.message ?: e.toString()
                )
            )
            logError("Excepción al consultar IA (${config.provider.displayName}): ${e.message}", e)
            Result.failure(
                AiServiceException(
                    "Error de conexión con la IA (${config.provider.displayName}): ${e.message}",
                    technicalDetails,
                    e
                )
            )
        }
    }

    open suspend fun testConnectivity(configOverride: AiConfiguration? = null): Result<AiTechnicalDetails> {
        val config = configOverride ?: configProvider()
        if (!config.provider.isCloud) {
            val details = AiTechnicalDetails(
                provider = config.provider,
                model = "On-Device",
                endpointUrl = "local",
                httpStatus = 200,
                durationMs = 0L,
                errorBody = null
            )
            return Result.success(details)
        }

        if (config.apiKey.isBlank()) {
            val details = AiTechnicalDetails(
                provider = config.provider,
                model = config.effectiveTextModel,
                endpointUrl = config.effectiveEndpointUrl,
                httpStatus = null,
                errorBody = "Clave de API no configurada",
                exceptionMessage = "Ingresa tu clave de API en Ajustes para probar la conexión.",
                durationMs = 0L
            )
            return Result.failure(
                AiServiceException("Ingresa tu clave de API en Ajustes para probar la conexión.", details)
            )
        }

        val startTime = System.currentTimeMillis()
        return try {
            val request = OpenAiChatRequest(
                model = config.effectiveTextModel,
                messages = listOf(
                    OpenAiMessage(
                        role = "user",
                        content = listOf(OpenAiTextPart("ping"))
                    )
                ),
                temperature = 0.0f
            )

            val httpResponse = httpClient.post(config.effectiveEndpointUrl) {
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val duration = System.currentTimeMillis() - startTime
            val statusCode = httpResponse.status.value
            val rawBody = try { httpResponse.body<String>() } catch (_: Exception) { "" }

            if (httpResponse.status.isSuccess()) {
                val details = AiTechnicalDetails(
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    httpStatus = statusCode,
                    durationMs = duration
                )
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.CONNECTIVITY_TEST,
                        provider = config.provider,
                        model = config.effectiveTextModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = "Prueba de conectividad (Ping)",
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = true,
                        rawResponse = rawBody
                    )
                )
                logInfo("Prueba de conectividad exitosa (${config.provider.displayName}): HTTP $statusCode en ${duration}ms")
                Result.success(details)
            } else {
                val details = AiTechnicalDetails(
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    httpStatus = statusCode,
                    errorBody = rawBody,
                    durationMs = duration
                )
                debugLogManager.log(
                    AiCallLogEntry(
                        callType = AiCallType.CONNECTIVITY_TEST,
                        provider = config.provider,
                        model = config.effectiveTextModel,
                        endpointUrl = config.effectiveEndpointUrl,
                        promptSummary = "Prueba de conectividad (Ping)",
                        httpStatus = statusCode,
                        durationMs = duration,
                        isSuccess = false,
                        rawResponse = rawBody,
                        errorMessage = "HTTP $statusCode: $rawBody"
                    )
                )
                logError("Prueba de conectividad fallida (${config.provider.displayName}): HTTP $statusCode - $rawBody")
                Result.failure(
                    AiServiceException("Error HTTP $statusCode al conectar con ${config.provider.displayName}", details)
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val details = AiTechnicalDetails(
                provider = config.provider,
                model = config.effectiveTextModel,
                endpointUrl = config.effectiveEndpointUrl,
                httpStatus = null,
                errorBody = null,
                exceptionMessage = e.message ?: e.toString(),
                durationMs = duration
            )
            debugLogManager.log(
                AiCallLogEntry(
                    callType = AiCallType.CONNECTIVITY_TEST,
                    provider = config.provider,
                    model = config.effectiveTextModel,
                    endpointUrl = config.effectiveEndpointUrl,
                    promptSummary = "Prueba de conectividad (Ping)",
                    httpStatus = null,
                    durationMs = duration,
                    isSuccess = false,
                    errorMessage = e.message ?: e.toString()
                )
            )
            logError("Excepción en prueba de conectividad (${config.provider.displayName}): ${e.message}", e)
            Result.failure(
                AiServiceException("No se pudo establecer conexión: ${e.message}", details, e)
            )
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
        } ?: MealCategory.DESAYUNO

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

    private suspend fun analyzeWithLocalCatalog(description: String): Result<DetectedMealResult> {
        val normalized = description.lowercase(Locale.ROOT)
        val catalog = catalogRepository ?: return Result.failure(IllegalStateException("Sin catálogo local"))

        val matchedItems = mutableListOf<ScannedFoodItem>()

        val queryKeywords = listOf(
            "café con leche" to "Café con leche",
            "café cortado" to "Café cortado",
            "café negro" to "Café negro",
            "café solo" to "Café negro",
            "café" to "Café negro",
            "té con leche" to "Café con leche",
            "té negro" to "Té",
            "té verde" to "Té",
            "té" to "Té",
            "azúcar" to "Azúcar blanca",
            "endulzante" to "Endulzante",
            "marraqueta" to "Marraqueta",
            "hallulla" to "Hallulla",
            "fajitas" to "Tortillas de fajita",
            "fajita" to "Tortillas de fajita",
            "tacos" to "Tortillas de fajita",
            "taco" to "Tortillas de fajita",
            "carne molida" to "Carne molida cocida",
            "choclo" to "Choclo desgranado",
            "lechuga" to "Lechuga picada",
            "yogurt griego" to "Yogurt griego",
            "yogur griego" to "Yogurt griego",
            "tajín" to "Tajín",
            "tajin" to "Tajín",
            "tomate" to "Tomate picado",
            "palta" to "Palta hass",
            "huevo revuelto" to "Huevo revuelto",
            "huevos revueltos" to "Huevo revuelto",
            "huevo" to "Huevo revuelto",
            "arroz" to "Arroz blanco cocido",
            "pollo" to "Pechuga de pollo a la plancha",
            "cazuela" to "Cazuela de vacuno",
            "porotos" to "Porotos granados",
            "pastel de choclo" to "Pastel de choclo",
            "avena" to "Avena en hojuelas",
            "manzana" to "Manzana",
            "plátano" to "Plátano",
            "leche descremada" to "Leche descremada",
            "leche entera" to "Leche entera",
            "leche" to "Leche descremada"
        )

        val globalMultiplier = when {
            normalized.contains("dos ") || normalized.contains("2 ") -> 2.0
            normalized.contains("tres ") || normalized.contains("3 ") -> 3.0
            normalized.contains("medio ") || normalized.contains("media ") || normalized.contains("1/2 ") -> 0.5
            else -> 1.0
        }
        val isCompoundDish = normalized.contains("fajita") || normalized.contains("taco") || normalized.contains("sandwich") || normalized.contains("sándwich")

        val addedLabels = mutableSetOf<String>()
        for ((keyword, targetLabel) in queryKeywords) {
            if (normalized.contains(keyword) && !addedLabels.contains(targetLabel)) {
                val matched = catalog.findBestMatch(targetLabel)
                if (matched != null) {
                    val itemMultiplier = if (isCompoundDish && targetLabel != "Tortillas de fajita") {
                        1.0
                    } else {
                        globalMultiplier
                    }

                    val adjusted = matched.copy(
                        servingGrams = matched.servingGrams * itemMultiplier,
                        householdPortion = matched.householdPortion?.let { hp ->
                            hp.copy(
                                quantity = hp.quantity * itemMultiplier,
                                equivalentGrams = hp.equivalentGrams * itemMultiplier
                            )
                        }
                    )
                    matchedItems.add(adjusted)
                    addedLabels.add(targetLabel)
                }
            }
        }

        if (matchedItems.isEmpty()) {
            return Result.failure(NoSuchElementException("No se encontraron alimentos coincidentes en el catálogo local."))
        }

        val suggestedCategory = when {
            normalized.contains("almuerzo") || normalized.contains("cazuela") || normalized.contains("porotos") || normalized.contains("pollo") -> MealCategory.ALMUERZO
            normalized.contains("once") || normalized.contains("cena") || normalized.contains("fajita") || normalized.contains("taco") -> MealCategory.ONCE_CENA
            normalized.contains("desayuno") || normalized.contains("café") || normalized.contains("té") || normalized.contains("marraqueta") -> MealCategory.DESAYUNO
            else -> MealCategory.DESAYUNO
        }

        return Result.success(
            DetectedMealResult(
                items = matchedItems,
                suggestedMealType = suggestedCategory,
                analysisSource = VisionSource.LOCAL_DEVICE
            )
        )
    }

    companion object {
        private const val TAG = "AiMealAnalyzer"

        private fun logInfo(message: String) {
            try {
                android.util.Log.i(TAG, message)
            } catch (_: Throwable) {
                println("[$TAG] $message")
            }
        }

        private fun logError(message: String, throwable: Throwable? = null) {
            try {
                android.util.Log.e(TAG, message, throwable)
            } catch (_: Throwable) {
                System.err.println("[$TAG] $message: ${throwable?.message}")
            }
        }
    }
}
