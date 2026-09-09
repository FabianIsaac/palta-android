package com.calculadoracalorias.app.domain.model

/**
 * Catálogo de proveedores de Inteligencia Artificial soportados por la aplicación.
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultEndpointUrl: String,
    val defaultTextModel: String,
    val defaultVisionModel: String,
    val isCloud: Boolean = true
) {
    LOCAL(
        id = "local",
        displayName = "En el dispositivo (LiteRT sin conexión)",
        defaultEndpointUrl = "",
        defaultTextModel = "",
        defaultVisionModel = "",
        isCloud = false
    ),
    NVIDIA_NIM(
        id = "nvidia",
        displayName = "NVIDIA NIM (Gratis / Llama 3.2 Vision)",
        defaultEndpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
        defaultTextModel = "meta/llama-3.3-70b-instruct",
        defaultVisionModel = "meta/llama-3.2-11b-vision-instruct"
    ),
    GOOGLE_GEMINI(
        id = "gemini",
        displayName = "Google Gemini (Gratis / Flash)",
        defaultEndpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultTextModel = "gemini-1.5-flash",
        defaultVisionModel = "gemini-1.5-flash"
    ),
    MINIMAX(
        id = "minimax",
        displayName = "MiniMax Cloud",
        defaultEndpointUrl = "https://api.minimaxi.chat/v1/chat/completions",
        defaultTextModel = "MiniMax-Text-01",
        defaultVisionModel = "MiniMax-M3"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Personalizado (Compatible con OpenAI)",
        defaultEndpointUrl = "https://api.openai.com/v1/chat/completions",
        defaultTextModel = "gpt-4o-mini",
        defaultVisionModel = "gpt-4o-mini"
    );

    fun toVisionSource(): VisionSource = when (this) {
        LOCAL -> VisionSource.LOCAL_DEVICE
        NVIDIA_NIM -> VisionSource.NVIDIA_NIM
        GOOGLE_GEMINI -> VisionSource.GOOGLE_GEMINI
        MINIMAX -> VisionSource.MINIMAX_CLOUD
        CUSTOM -> VisionSource.CUSTOM_CLOUD
    }

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NVIDIA_NIM
    }
}
