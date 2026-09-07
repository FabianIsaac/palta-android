package com.calculadoracalorias.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Representa un alimento detectado o analizado con valores de referencia por 100g
 * y la porción estimada o ajustada en gramos.
 *
 * El cálculo de macronutrientes y calorías totales se realiza de forma proporcional:
 * total = (valorPor100g * porciónGramos) / 100
 */
data class ScannedFoodItem(
    val id: String,
    val name: String,
    val servingGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val confidence: Float = 1.0f
) {
    val totalCalories: Double
        get() = roundToDecimals((caloriesPer100g * servingGrams) / 100.0, 1)

    val totalProtein: Double
        get() = roundToDecimals((proteinPer100g * servingGrams) / 100.0, 2)

    val totalCarbs: Double
        get() = roundToDecimals((carbsPer100g * servingGrams) / 100.0, 2)

    val totalFat: Double
        get() = roundToDecimals((fatPer100g * servingGrams) / 100.0, 2)

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
