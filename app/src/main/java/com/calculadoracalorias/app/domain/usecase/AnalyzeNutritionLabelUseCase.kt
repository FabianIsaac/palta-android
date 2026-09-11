package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.NutritionLabelScanResult
import com.calculadoracalorias.app.domain.repository.NutritionLabelAnalyzer

/**
 * Caso de uso que valida y coordina el análisis visual de una tabla nutricional.
 */
class AnalyzeNutritionLabelUseCase(
    private val analyzer: NutritionLabelAnalyzer
) {
    suspend operator fun invoke(imageBytes: ByteArray): Result<NutritionLabelScanResult> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("La imagen capturada no contiene datos válidos."))
        }
        return analyzer.analyzeNutritionLabel(imageBytes)
    }
}
