package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.NutritionLabelScanResult

/**
 * Interfaz de repositorio/analizador para la extracción de información nutricional
 * desde imágenes de etiquetas o envases.
 */
interface NutritionLabelAnalyzer {
    suspend fun analyzeNutritionLabel(imageBytes: ByteArray): Result<NutritionLabelScanResult>
}
