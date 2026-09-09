package com.calculadoracalorias.app.domain.model

/**
 * Resumen consolidado del balance calórico y macronutrientes para un día específico.
 */
data class DailySummary(
    val dateEpochDay: Long,
    val budget: DailyMacroBudget,
    val consumed: NutritionSummary,
    val remainingCalories: Double,
    val mealsByCategory: Map<MealCategory, List<MealEntry>>
)
