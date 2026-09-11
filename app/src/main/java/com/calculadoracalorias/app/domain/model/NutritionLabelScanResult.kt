package com.calculadoracalorias.app.domain.model

/**
 * Modelo de dominio que representa los datos extraídos del análisis visual
 * de una tabla nutricional de alimento o suplemento.
 */
data class NutritionLabelScanResult(
    val productName: String? = null,
    val servingDescription: String,
    val servingGrams: Double? = null,
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
)
