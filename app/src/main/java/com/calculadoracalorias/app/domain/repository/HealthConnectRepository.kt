package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.MealCategory

/**
 * Contrato de dominio para interactuar con Android Health Connect.
 * Permite consultar disponibilidad del SDK, estado de permisos y exportar registros nutricionales.
 */
interface HealthConnectRepository {
    suspend fun isHealthConnectAvailable(): Boolean
    suspend fun hasWriteNutritionPermission(): Boolean
    suspend fun writeNutritionRecord(
        mealName: String,
        mealCategory: MealCategory,
        timestamp: Long,
        calories: Double,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double
    ): Result<String>
}
