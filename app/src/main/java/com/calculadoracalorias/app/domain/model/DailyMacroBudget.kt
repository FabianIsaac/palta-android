package com.calculadoracalorias.app.domain.model

/**
 * Presupuesto u objetivo diario de calorías y macronutrientes.
 */
data class DailyMacroBudget(
    val targetCalories: Double = 2000.0,
    val targetProteinGrams: Double = 150.0,
    val targetCarbsGrams: Double = 200.0,
    val targetFatGrams: Double = 65.0
)
