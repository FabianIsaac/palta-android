package com.calculadoracalorias.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealFoodItemEntity>)

    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getMealsBetweenTimestamps(startTime: Long, endTime: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_food_items WHERE mealEntryId = :mealId")
    suspend fun getItemsForMeal(mealId: Long): List<MealFoodItemEntity>

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
}
