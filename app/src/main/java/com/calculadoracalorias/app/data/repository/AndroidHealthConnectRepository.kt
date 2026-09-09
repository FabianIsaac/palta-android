package com.calculadoracalorias.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import java.time.Instant
import java.time.ZoneId

class AndroidHealthConnectRepository(
    private val context: Context,
    private val sdkStatusProvider: () -> Int = { HealthConnectClient.getSdkStatus(context) },
    private val healthConnectClientProvider: () -> HealthConnectClient? = {
        if (sdkStatusProvider() == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }
) : HealthConnectRepository {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(NutritionRecord::class)
    )

    override suspend fun isHealthConnectAvailable(): Boolean {
        return sdkStatusProvider() == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun hasWriteNutritionPermission(): Boolean {
        val client = healthConnectClientProvider() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    override suspend fun writeNutritionRecord(
        mealName: String,
        mealCategory: MealCategory,
        timestamp: Long,
        calories: Double,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double
    ): Result<String> {
        val client = healthConnectClientProvider()
            ?: return Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo."))

        return try {
            val startInstant = Instant.ofEpochMilli(timestamp)
            val endInstant = startInstant.plusSeconds(60)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val mappedMealType = when (mealCategory) {
                MealCategory.DESAYUNO -> MealType.MEAL_TYPE_BREAKFAST
                MealCategory.ALMUERZO -> MealType.MEAL_TYPE_LUNCH
                MealCategory.ONCE_CENA -> MealType.MEAL_TYPE_DINNER
                MealCategory.COLACIONES -> MealType.MEAL_TYPE_SNACK
            }

            val record = NutritionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories(calories.coerceAtLeast(0.0)),
                protein = Mass.grams(proteinGrams.coerceAtLeast(0.0)),
                totalCarbohydrate = Mass.grams(carbsGrams.coerceAtLeast(0.0)),
                totalFat = Mass.grams(fatGrams.coerceAtLeast(0.0)),
                name = mealName.ifBlank { mealCategory.displayName },
                mealType = mappedMealType
            )

            val response = client.insertRecords(listOf(record))
            val recordId = response.recordIdsList.firstOrNull()
                ?: return Result.failure(RuntimeException("No se recibió identificador tras registrar en Health Connect."))

            Result.success(recordId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNutritionRecord(
        recordId: String,
        mealName: String,
        mealCategory: MealCategory,
        timestamp: Long,
        calories: Double,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double
    ): Result<Unit> {
        val client = healthConnectClientProvider()
            ?: return Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo."))

        return try {
            val startInstant = Instant.ofEpochMilli(timestamp)
            val endInstant = startInstant.plusSeconds(60)
            val zoneOffset = ZoneId.systemDefault().rules.getOffset(startInstant)

            val mappedMealType = when (mealCategory) {
                MealCategory.DESAYUNO -> MealType.MEAL_TYPE_BREAKFAST
                MealCategory.ALMUERZO -> MealType.MEAL_TYPE_LUNCH
                MealCategory.ONCE_CENA -> MealType.MEAL_TYPE_DINNER
                MealCategory.COLACIONES -> MealType.MEAL_TYPE_SNACK
            }

            val record = NutritionRecord(
                metadata = Metadata(id = recordId),
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories(calories.coerceAtLeast(0.0)),
                protein = Mass.grams(proteinGrams.coerceAtLeast(0.0)),
                totalCarbohydrate = Mass.grams(carbsGrams.coerceAtLeast(0.0)),
                totalFat = Mass.grams(fatGrams.coerceAtLeast(0.0)),
                name = mealName.ifBlank { mealCategory.displayName },
                mealType = mappedMealType
            )

            client.updateRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNutritionRecord(recordId: String): Result<Unit> {
        val client = healthConnectClientProvider()
            ?: return Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo."))

        return try {
            client.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = listOf(recordId),
                clientRecordIdsList = emptyList()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
