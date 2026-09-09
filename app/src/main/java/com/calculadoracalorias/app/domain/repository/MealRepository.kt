package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para persistencia y consulta de comidas registradas.
 */
interface MealRepository {
    suspend fun saveMeal(
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String? = null,
        rawDescription: String? = null,
        isPendingAiRefinement: Boolean = false
    ): Result<Long>

    fun getMealsForDay(startOfDayTimestamp: Long, endOfDayTimestamp: Long): Flow<List<MealEntry>>

    suspend fun getMealById(mealId: Long): Result<MealEntry?>

    suspend fun getMostRecentMealForCategory(
        category: MealCategory,
        startTime: Long,
        endTime: Long
    ): Result<MealEntry?>

    suspend fun updateMeal(
        mealId: Long,
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String?,
        rawDescription: String? = null,
        isPendingAiRefinement: Boolean = false
    ): Result<Unit>

    suspend fun updateMeal(meal: MealEntry): Result<Unit> = updateMeal(
        mealId = meal.id,
        category = meal.category,
        timestamp = meal.timestamp,
        items = meal.items,
        healthConnectRecordId = meal.healthConnectRecordId,
        rawDescription = meal.rawDescription,
        isPendingAiRefinement = meal.isPendingAiRefinement
    )

    suspend fun deleteMeal(mealId: Long): Result<Unit>

    suspend fun getPendingRefinementMeals(): Result<List<MealEntry>>
}
