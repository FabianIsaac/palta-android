package com.calculadoracalorias.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Resultado completo devuelto tras el análisis de visión computacional de una imagen de comida.
 */
data class DetectedMealResult(
    val items: List<ScannedFoodItem>,
    val suggestedMealType: MealCategory,
    val analysisSource: VisionSource,
    val technicalError: AiTechnicalDetails? = null
) {
    val totalCalories: Double
        get() = roundToDecimals(items.sumOf { it.totalCalories }, 1)

    val totalProtein: Double
        get() = roundToDecimals(items.sumOf { it.totalProtein }, 2)

    val totalCarbs: Double
        get() = roundToDecimals(items.sumOf { it.totalCarbs }, 2)

    val totalFat: Double
        get() = roundToDecimals(items.sumOf { it.totalFat }, 2)

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
