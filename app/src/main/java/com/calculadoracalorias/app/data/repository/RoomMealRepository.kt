package com.calculadoracalorias.app.data.repository

import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.MealRepository

class RoomMealRepository(
    private val mealDao: MealDao
) : MealRepository {

    override suspend fun saveMeal(
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String?
    ): Result<Long> {
        return try {
            val summary = NutritionSummary.fromItems(items)

            val mealEntity = MealEntryEntity(
                category = category.name,
                timestamp = timestamp,
                healthConnectRecordId = healthConnectRecordId,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )

            val mealId = mealDao.insertMealWithItems(mealEntity) { generatedId ->
                items.map { item ->
                    MealFoodItemEntity(
                        mealEntryId = generatedId,
                        name = item.name,
                        servingGrams = item.servingGrams,
                        caloriesPer100g = item.caloriesPer100g,
                        proteinPer100g = item.proteinPer100g,
                        carbsPer100g = item.carbsPer100g,
                        fatPer100g = item.fatPer100g
                    )
                }
            }

            Result.success(mealId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
