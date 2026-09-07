package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.FoodVisionAnalyzer

/**
 * Fábrica dinámica para seleccionar el analizador de visión óptimo
 * según la conectividad a internet, configuración de API Key y preferencias del usuario.
 */
class FoodVisionAnalyzerFactory(
    private val localAnalyzer: LocalLiteRtVisionAnalyzer,
    private val miniMaxAnalyzer: MiniMaxVisionAnalyzer
) {
    fun getAnalyzer(
        preferredSource: VisionSource,
        isOnline: Boolean,
        hasApiKey: Boolean
    ): FoodVisionAnalyzer {
        return if (preferredSource == VisionSource.MINIMAX_CLOUD && isOnline && hasApiKey) {
            // Se utiliza un analizador resiliente con fallback automático al analizador local si falla la red
            FallbackVisionAnalyzer(
                primaryAnalyzer = miniMaxAnalyzer,
                fallbackAnalyzer = localAnalyzer
            )
        } else {
            localAnalyzer
        }
    }

    private class FallbackVisionAnalyzer(
        private val primaryAnalyzer: FoodVisionAnalyzer,
        private val fallbackAnalyzer: FoodVisionAnalyzer
    ) : FoodVisionAnalyzer {
        override suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult> {
            val primaryResult = primaryAnalyzer.analyzeImage(imageBytes)
            return if (primaryResult.isSuccess) {
                primaryResult
            } else {
                // Degradación elegante al analizador local offline
                fallbackAnalyzer.analyzeImage(imageBytes)
            }
        }
    }
}
