package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import io.ktor.client.HttpClient

/**
 * Analizador remoto de lenguaje natural para comidas.
 * Extiende OpenAiCompatibleMealAnalyzer para compatibilidad directa y soporte multiproveedor.
 */
class RemoteNaturalLanguageMealAnalyzer : OpenAiCompatibleMealAnalyzer {
    constructor(
        configProvider: () -> AiConfiguration,
        httpClient: HttpClient,
        catalogRepository: FoodCatalogRepository? = null
    ) : super(configProvider, httpClient, catalogRepository)

    constructor(
        apiKeyProvider: () -> String,
        httpClient: HttpClient,
        catalogRepository: FoodCatalogRepository? = null,
        endpointUrl: String = "https://api.minimaxi.chat/v1/chat/completions",
        model: String = "MiniMax-Text-01"
    ) : super(apiKeyProvider, httpClient, catalogRepository, endpointUrl, model)
}
