package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.DetectedMealResult

/**
 * Abstracción de dominio para el motor de análisis visual de alimentos.
 * Permite implementar tanto el clasificador local on-device como el servicio multimodal en la nube (MiniMax).
 */
interface FoodVisionAnalyzer {
    suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult>
}
