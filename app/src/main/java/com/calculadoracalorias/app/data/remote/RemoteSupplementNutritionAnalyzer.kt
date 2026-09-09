package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.domain.model.AiConfiguration
import io.ktor.client.HttpClient

/**
 * Analizador remoto de información nutricional para suplementos.
 * Extiende OpenAiCompatibleSupplementAnalyzer para compatibilidad directa y soporte multiproveedor.
 */
class RemoteSupplementNutritionAnalyzer : OpenAiCompatibleSupplementAnalyzer {
    constructor(
        configProvider: () -> AiConfiguration,
        httpClient: HttpClient
    ) : super(configProvider, httpClient)

    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-Text-01"
    ) : super(apiKeyProvider, httpClient, endpointUrl, model)
}
