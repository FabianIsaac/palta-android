package com.calculadoracalorias.app.domain.model

/**
 * Fuente del motor de análisis de visión por computadora.
 */
enum class VisionSource {
    LOCAL_DEVICE,
    MINIMAX_CLOUD,
    NVIDIA_NIM,
    GOOGLE_GEMINI,
    CUSTOM_CLOUD
}
