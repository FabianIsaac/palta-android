package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.repository.FoodVisionAnalyzer

/**
 * Caso de uso que coordina el análisis de una imagen de comida delegando en el
 * analizador de visión inyectado (local o en la nube).
 */
class AnalyzeFoodImageUseCase(
    private val visionAnalyzer: FoodVisionAnalyzer
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<DetectedMealResult> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("La imagen capturada no contiene datos válidos."))
        }
        return visionAnalyzer.analyzeImage(imageBytes)
    }
}
