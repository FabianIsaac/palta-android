package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.SupplementNutritionEstimate

/**
 * Contrato para estimar automáticamente el aporte nutricional de un suplemento
 * a partir de su nombre o descripción en lenguaje natural.
 */
interface SupplementNutritionAnalyzer {
    suspend fun estimateSupplement(query: String): Result<SupplementNutritionEstimate>
}
