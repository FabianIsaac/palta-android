package com.calculadoracalorias.app.domain.model

/**
 * Representa una comida completa registrada con sus alimentos individuales y resumen nutricional.
 */
data class MealEntry(
    val id: Long,
    val category: MealCategory,
    val timestamp: Long,
    val items: List<ScannedFoodItem>,
    val summary: NutritionSummary,
    val healthConnectRecordId: String? = null,
    val rawDescription: String? = null,
    val isPendingAiRefinement: Boolean = false
)
