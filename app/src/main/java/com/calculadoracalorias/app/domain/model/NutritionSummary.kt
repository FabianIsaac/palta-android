package com.calculadoracalorias.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Resumen agregado de calorías y macronutrientes.
 */
data class NutritionSummary(
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0
) {
    companion object {
        fun fromItems(items: List<ScannedFoodItem>): NutritionSummary {
            val calories = round(items.sumOf { it.totalCalories }, 1)
            val protein = round(items.sumOf { it.totalProtein }, 2)
            val carbs = round(items.sumOf { it.totalCarbs }, 2)
            val fat = round(items.sumOf { it.totalFat }, 2)
            return NutritionSummary(
                totalCalories = calories,
                totalProtein = protein,
                totalCarbs = carbs,
                totalFat = fat
            )
        }

        private fun round(value: Double, decimals: Int): Double {
            if (value.isNaN() || value.isInfinite()) return 0.0
            return BigDecimal(value.toString())
                .setScale(decimals, RoundingMode.HALF_UP)
                .toDouble()
        }
    }
}
