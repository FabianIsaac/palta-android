package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.AiConfiguration
import io.ktor.client.HttpClient

/**
 * Analizador de visión de MiniMax.
 * Extiende OpenAiCompatibleVisionAnalyzer para compatibilidad directa y soporte multiproveedor.
 */
class MiniMaxVisionAnalyzer : OpenAiCompatibleVisionAnalyzer {
    constructor(
        configProvider: () -> AiConfiguration,
        httpClient: HttpClient
    ) : super(configProvider, httpClient)

    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-M3"
    ) : super(apiKeyProvider, httpClient, endpointUrl, model)
}
