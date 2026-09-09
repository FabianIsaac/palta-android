package com.calculadoracalorias.app.data.repository

import androidx.room.withTransaction
import com.calculadoracalorias.app.data.local.AppDatabase
import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.dao.SupplementDao
import com.calculadoracalorias.app.data.local.dao.SupplementLogDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.model.backup.BackupMealEntry
import com.calculadoracalorias.app.domain.model.backup.BackupMealItem
import com.calculadoracalorias.app.domain.model.backup.BackupMealTimeWindows
import com.calculadoracalorias.app.domain.model.backup.BackupPreferences
import com.calculadoracalorias.app.domain.model.backup.BackupSupplement
import com.calculadoracalorias.app.domain.model.backup.BackupSupplementLog
import com.calculadoracalorias.app.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalTime

class RoomBackupRepository(
    private val appDatabase: AppDatabase,
    private val mealDao: MealDao,
    private val supplementDao: SupplementDao,
    private val supplementLogDao: SupplementLogDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appVersionName: String = "1.1"
) : BackupRepository {

    override suspend fun exportBackupPayload(): BackupDataPayload {
        val mealsWithItems = mealDao.getAllMealsWithItems()
        val backupMeals = mealsWithItems.map { mealWithItems ->
            val items = mealWithItems.items.map { item ->
                val totalCal = (item.caloriesPer100g * item.servingGrams) / 100.0
                val totalProt = (item.proteinPer100g * item.servingGrams) / 100.0
                val totalCarb = (item.carbsPer100g * item.servingGrams) / 100.0
                val totalFat = (item.fatPer100g * item.servingGrams) / 100.0

                BackupMealItem(
                    id = item.id,
                    name = item.name,
                    portionGrams = item.servingGrams,
                    calories = totalCal,
                    proteinGrams = totalProt,
                    carbsGrams = totalCarb,
                    fatGrams = totalFat,
                    householdMeasure = null,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g
                )
            }

            BackupMealEntry(
                id = mealWithItems.meal.id,
                category = mealWithItems.meal.category,
                timestamp = mealWithItems.meal.timestamp,
                totalCalories = mealWithItems.meal.totalCalories,
                totalProteinGrams = mealWithItems.meal.totalProtein,
                totalCarbsGrams = mealWithItems.meal.totalCarbs,
                totalFatGrams = mealWithItems.meal.totalFat,
                notes = null,
                rawDescription = mealWithItems.meal.rawDescription,
                isPendingAiRefinement = mealWithItems.meal.isPendingAiRefinement,
                items = items
            )
        }

        val supplements = supplementDao.getAllSupplementsList().map { supp ->
            BackupSupplement(
                id = supp.id,
                name = supp.name,
                dosageDescription = supp.dosageDescription,
                calories = supp.calories,
                proteinGrams = supp.proteinGrams,
                carbsGrams = supp.carbsGrams,
                fatGrams = supp.fatGrams,
                isActive = supp.isActive,
                isCustom = supp.isCustom,
                createdAt = supp.createdAt
            )
        }

        val supplementLogs = supplementLogDao.getAllLogs().map { log ->
            BackupSupplementLog(
                date = log.date,
                supplementId = log.supplementId,
                takenTimestamp = log.takenTimestamp
            )
        }

        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        val backupPreferences = BackupPreferences(
            targetCalories = prefs.targetCalories,
            targetProteinGrams = prefs.targetProteinGrams,
            targetCarbsGrams = prefs.targetCarbsGrams,
            targetFatGrams = prefs.targetFatGrams,
            mealTimeWindows = BackupMealTimeWindows(
                breakfastStartMinute = prefs.mealTimeWindows.breakfastStart.toMinuteOfDay(),
                breakfastEndMinute = prefs.mealTimeWindows.breakfastEnd.toMinuteOfDay(),
                lunchStartMinute = prefs.mealTimeWindows.lunchStart.toMinuteOfDay(),
                lunchEndMinute = prefs.mealTimeWindows.lunchEnd.toMinuteOfDay(),
                dinnerStartMinute = prefs.mealTimeWindows.dinnerStart.toMinuteOfDay(),
                dinnerEndMinute = prefs.mealTimeWindows.dinnerEnd.toMinuteOfDay()
            )
        )

        return BackupDataPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appVersionName = appVersionName,
            meals = backupMeals,
            supplements = supplements,
            supplementLogs = supplementLogs,
            preferences = backupPreferences
        )
    }

    override suspend fun importBackupPayload(payload: BackupDataPayload) {
        appDatabase.withTransaction {
            val mealsWithItems = payload.meals.map { bMeal ->
                val mealEntity = MealEntryEntity(
                    id = bMeal.id,
                    category = bMeal.category,
                    timestamp = bMeal.timestamp,
                    totalCalories = bMeal.totalCalories,
                    totalProtein = bMeal.totalProteinGrams,
                    totalCarbs = bMeal.totalCarbsGrams,
                    totalFat = bMeal.totalFatGrams,
                    rawDescription = bMeal.rawDescription,
                    isPendingAiRefinement = bMeal.isPendingAiRefinement
                )
                val itemEntities = bMeal.items.map { bItem ->
                    val portion = bItem.portionGrams.coerceAtLeast(1.0)
                    val calPer100 = if (bItem.caloriesPer100g > 0.0) bItem.caloriesPer100g else (bItem.calories / portion) * 100.0
                    val protPer100 = if (bItem.proteinPer100g > 0.0) bItem.proteinPer100g else (bItem.proteinGrams / portion) * 100.0
                    val carbsPer100 = if (bItem.carbsPer100g > 0.0) bItem.carbsPer100g else (bItem.carbsGrams / portion) * 100.0
                    val fatPer100 = if (bItem.fatPer100g > 0.0) bItem.fatPer100g else (bItem.fatGrams / portion) * 100.0

                    MealFoodItemEntity(
                        id = bItem.id,
                        mealEntryId = bMeal.id,
                        name = bItem.name,
                        servingGrams = portion,
                        caloriesPer100g = calPer100,
                        proteinPer100g = protPer100,
                        carbsPer100g = carbsPer100,
                        fatPer100g = fatPer100
                    )
                }
                Pair(mealEntity, itemEntities)
            }
            mealDao.restoreMealsWithItems(mealsWithItems)

            if (payload.supplements.isNotEmpty()) {
                val supplementEntities = payload.supplements.map { supp ->
                    SupplementEntity(
                        id = supp.id,
                        name = supp.name,
                        dosageDescription = supp.dosageDescription,
                        calories = supp.calories,
                        proteinGrams = supp.proteinGrams,
                        carbsGrams = supp.carbsGrams,
                        fatGrams = supp.fatGrams,
                        isActive = supp.isActive,
                        isCustom = supp.isCustom,
                        createdAt = if (supp.createdAt > 0L) supp.createdAt else System.currentTimeMillis()
                    )
                }
                supplementDao.deleteAllSupplements()
                supplementDao.insertOrReplaceAll(supplementEntities)
            }

            if (payload.supplementLogs.isNotEmpty()) {
                val logEntities = payload.supplementLogs.map { log ->
                    SupplementLogEntity(
                        date = log.date,
                        supplementId = log.supplementId,
                        takenTimestamp = log.takenTimestamp
                    )
                }
                supplementLogDao.deleteAllLogs()
                supplementLogDao.insertAllLogs(logEntities)
            }
        }

        val windows = MealTimeWindows(
            breakfastStart = minuteToLocalTime(payload.preferences.mealTimeWindows.breakfastStartMinute),
            breakfastEnd = minuteToLocalTime(payload.preferences.mealTimeWindows.breakfastEndMinute),
            lunchStart = minuteToLocalTime(payload.preferences.mealTimeWindows.lunchStartMinute),
            lunchEnd = minuteToLocalTime(payload.preferences.mealTimeWindows.lunchEndMinute),
            dinnerStart = minuteToLocalTime(payload.preferences.mealTimeWindows.dinnerStartMinute),
            dinnerEnd = minuteToLocalTime(payload.preferences.mealTimeWindows.dinnerEndMinute)
        )

        userPreferencesRepository.restorePreferences(
            targetCalories = payload.preferences.targetCalories,
            targetProtein = payload.preferences.targetProteinGrams,
            targetCarbs = payload.preferences.targetCarbsGrams,
            targetFat = payload.preferences.targetFatGrams,
            windows = windows
        )
    }

    override fun getLastBackupTimestamp(): Flow<Long?> =
        userPreferencesRepository.getLastBackupTimestamp()

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        userPreferencesRepository.setLastBackupTimestamp(timestamp)
    }

    private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
    private fun minuteToLocalTime(minutes: Int): LocalTime =
        LocalTime.of((minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))
}
