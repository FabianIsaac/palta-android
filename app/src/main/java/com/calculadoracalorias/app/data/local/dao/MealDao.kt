package com.calculadoracalorias.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.relation.MealWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealFoodItemEntity>)

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getMealsBetweenTimestamps(startTime: Long, endTime: Long): Flow<List<MealEntryEntity>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getMealsWithItemsBetween(startTime: Long, endTime: Long): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :mealId")
    suspend fun getMealWithItemsById(mealId: Long): MealWithItems?

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE (category = :categoryName OR category = :categoryDisplayName) AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentMealWithItemsForCategory(
        categoryName: String,
        categoryDisplayName: String,
        startTime: Long,
        endTime: Long
    ): MealWithItems?

    @Query("SELECT * FROM meal_food_items WHERE mealEntryId = :mealId")
    suspend fun getItemsForMeal(mealId: Long): List<MealFoodItemEntity>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE isPendingAiRefinement = 1 ORDER BY timestamp ASC")
    suspend fun getPendingRefinementMealsWithItems(): List<MealWithItems>

    @Query("DELETE FROM meal_food_items WHERE mealEntryId = :mealId")
    suspend fun deleteItemsForMeal(mealId: Long)

    @Query("UPDATE meal_entries SET healthConnectRecordId = :recordId WHERE id = :mealId")
    suspend fun updateHealthConnectRecordId(mealId: Long, recordId: String)

    @Query("DELETE FROM meal_entries WHERE id = :mealId")
    suspend fun deleteMeal(mealId: Long)

    @Transaction
    suspend fun insertMealWithItems(
        meal: MealEntryEntity,
        itemsProvider: (Long) -> List<MealFoodItemEntity>
    ): Long {
        val mealId = insertMeal(meal)
        val items = itemsProvider(mealId)
        insertMealItems(items)
        return mealId
    }

    @Transaction
    suspend fun updateMealWithItems(
        meal: MealEntryEntity,
        items: List<MealFoodItemEntity>
    ) {
        insertMeal(meal)
        deleteItemsForMeal(meal.id)
        insertMealItems(items)
    }

    @Transaction
    @Query("SELECT * FROM meal_entries ORDER BY timestamp ASC")
    suspend fun getAllMealsWithItems(): List<MealWithItems>

    @Query("DELETE FROM meal_food_items")
    suspend fun deleteAllMealFoodItems()

    @Query("DELETE FROM meal_entries")
    suspend fun deleteAllMealEntries()

    @Transaction
    suspend fun restoreMealsWithItems(mealsWithItems: List<Pair<MealEntryEntity, List<MealFoodItemEntity>>>) {
        deleteAllMealFoodItems()
        deleteAllMealEntries()
        for ((meal, items) in mealsWithItems) {
            val insertedMealId = insertMeal(meal)
            val updatedItems = items.map { it.copy(mealEntryId = insertedMealId) }
            insertMealItems(updatedItems)
        }
    }
}
