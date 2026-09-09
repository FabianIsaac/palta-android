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
    private val cloudAnalyzer: FoodVisionAnalyzer
) {
    // Constructor de conveniencia retrocompatible
    constructor(
        localAnalyzer: LocalLiteRtVisionAnalyzer,
        miniMaxAnalyzer: MiniMaxVisionAnalyzer
    ) : this(localAnalyzer, miniMaxAnalyzer as FoodVisionAnalyzer)

    fun getAnalyzer(
        preferredSource: VisionSource,
        isOnline: Boolean,
        hasApiKey: Boolean
    ): FoodVisionAnalyzer {
        val isCloudPreferred = preferredSource != VisionSource.LOCAL_DEVICE
        return if (isCloudPreferred && isOnline && hasApiKey) {
            // Se utiliza un analizador resiliente con fallback automático al analizador local si falla la red
            FallbackVisionAnalyzer(
                primaryAnalyzer = cloudAnalyzer,
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
            if (primaryResult.isSuccess) {
                return primaryResult
            }

            val primaryError = primaryResult.exceptionOrNull()
            android.util.Log.w("FoodVision", "El analizador principal en la nube falló: ${primaryError?.message}", primaryError)

            val fallbackResult = fallbackAnalyzer.analyzeImage(imageBytes)
            return if (fallbackResult.isSuccess) {
                fallbackResult
            } else {
                // Si ambos fallan, priorizar el mensaje de error del analizador en la nube
                // para que el usuario conozca la causa real (clave inválida, cuota, error HTTP o timeout)
                Result.failure(primaryError ?: fallbackResult.exceptionOrNull() ?: RuntimeException("No pudimos analizar la imagen."))
            }
        }
    }
}
