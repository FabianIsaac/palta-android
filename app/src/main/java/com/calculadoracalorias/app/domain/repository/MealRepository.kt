package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem

/**
 * Contrato de dominio para persistencia y consulta de comidas registradas.
 */
interface MealRepository {
    suspend fun saveMeal(
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String? = null
    ): Result<Long>
}
