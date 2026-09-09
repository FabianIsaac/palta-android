package com.calculadoracalorias.app.domain.model

/**
 * Objeto de configuración activa para llamadas a modelos de Inteligencia Artificial.
 */
data class AiConfiguration(
    val provider: AiProvider = AiProvider.NVIDIA_NIM,
    val apiKey: String = "",
    val customEndpointUrl: String = "",
    val customTextModel: String = "",
    val customVisionModel: String = ""
) {
    val effectiveEndpointUrl: String
        get() = if (provider == AiProvider.CUSTOM && customEndpointUrl.isNotBlank()) {
            customEndpointUrl.trim()
        } else {
            provider.defaultEndpointUrl
        }

    val effectiveTextModel: String
        get() = if (provider == AiProvider.CUSTOM && customTextModel.isNotBlank()) {
            customTextModel.trim()
        } else {
            provider.defaultTextModel
        }

    val effectiveVisionModel: String
        get() = if (provider == AiProvider.CUSTOM && customVisionModel.isNotBlank()) {
            customVisionModel.trim()
        } else {
            provider.defaultVisionModel
        }
}
