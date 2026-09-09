package com.calculadoracalorias.app.data.repository

import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.relation.MealWithItems
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMealRepository(
    private val mealDao: MealDao
) : MealRepository {

    override suspend fun saveMeal(
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String?,
        rawDescription: String?,
        isPendingAiRefinement: Boolean
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
                totalFat = summary.totalFat,
                rawDescription = rawDescription,
                isPendingAiRefinement = isPendingAiRefinement
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

    override fun getMealsForDay(
        startOfDayTimestamp: Long,
        endOfDayTimestamp: Long
    ): Flow<List<MealEntry>> {
        return mealDao.getMealsWithItemsBetween(startOfDayTimestamp, endOfDayTimestamp).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMealById(mealId: Long): Result<MealEntry?> {
        return try {
            val mealWithItems = mealDao.getMealWithItemsById(mealId)
            Result.success(mealWithItems?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMostRecentMealForCategory(
        category: MealCategory,
        startTime: Long,
        endTime: Long
    ): Result<MealEntry?> {
        return try {
            val mealWithItems = mealDao.getMostRecentMealWithItemsForCategory(
                categoryName = category.name,
                categoryDisplayName = category.displayName,
                startTime = startTime,
                endTime = endTime
            )
            Result.success(mealWithItems?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMeal(
        mealId: Long,
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        healthConnectRecordId: String?,
        rawDescription: String?,
        isPendingAiRefinement: Boolean
    ): Result<Unit> {
        return try {
            val summary = NutritionSummary.fromItems(items)

            val mealEntity = MealEntryEntity(
                id = mealId,
                category = category.name,
                timestamp = timestamp,
                healthConnectRecordId = healthConnectRecordId,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat,
                rawDescription = rawDescription,
                isPendingAiRefinement = isPendingAiRefinement
            )

            val itemEntities = items.map { item ->
                MealFoodItemEntity(
                    mealEntryId = mealId,
                    name = item.name,
                    servingGrams = item.servingGrams,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g
                )
            }

            mealDao.updateMealWithItems(mealEntity, itemEntities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeal(mealId: Long): Result<Unit> {
        return try {
            mealDao.deleteMeal(mealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingRefinementMeals(): Result<List<MealEntry>> {
        return try {
            val list = mealDao.getPendingRefinementMealsWithItems()
            Result.success(list.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun MealWithItems.toDomain(): MealEntry {
        val domainItems = items.map { item ->
            ScannedFoodItem(
                id = item.id.toString(),
                name = item.name,
                servingGrams = item.servingGrams,
                caloriesPer100g = item.caloriesPer100g,
                proteinPer100g = item.proteinPer100g,
                carbsPer100g = item.carbsPer100g,
                fatPer100g = item.fatPer100g
            )
        }

        val parsedCategory = try {
            MealCategory.valueOf(meal.category)
        } catch (_: Exception) {
            MealCategory.fromDisplayName(meal.category)
        }

        val domainSummary = NutritionSummary(
            totalCalories = meal.totalCalories,
            totalProtein = meal.totalProtein,
            totalCarbs = meal.totalCarbs,
            totalFat = meal.totalFat
        )

        return MealEntry(
            id = meal.id,
            category = parsedCategory,
            timestamp = meal.timestamp,
            items = domainItems,
            summary = domainSummary,
            healthConnectRecordId = meal.healthConnectRecordId,
            rawDescription = meal.rawDescription,
            isPendingAiRefinement = meal.isPendingAiRefinement
        )
    }
}
