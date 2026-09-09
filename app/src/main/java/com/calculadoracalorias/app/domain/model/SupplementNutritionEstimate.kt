package com.calculadoracalorias.app.domain.model

/**
 * Estimación de aporte nutricional y dosis recomendada para un suplemento.
 */
data class SupplementNutritionEstimate(
    val name: String,
    val dosageDescription: String,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0
)
