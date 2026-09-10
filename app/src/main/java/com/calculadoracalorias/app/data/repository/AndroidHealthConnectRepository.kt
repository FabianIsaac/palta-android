package com.calculadoracalorias.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.calculadoracalorias.app.domain.model.DailyHealthActivity
import com.calculadoracalorias.app.domain.model.HealthWeightRecord
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import java.time.Instant
import java.time.LocalDate
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

    val activityPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    val weightPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class)
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

    override suspend fun hasActivityPermissions(): Boolean {
        val client = healthConnectClientProvider() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(activityPermissions)
    }

    override suspend fun hasWeightPermission(): Boolean {
        val client = healthConnectClientProvider() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(weightPermissions)
    }

    override suspend fun getDailyActivity(date: LocalDate): Result<DailyHealthActivity> {
        val client = healthConnectClientProvider()
            ?: return Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo."))

        return try {
            val zoneId = ZoneId.systemDefault()
            val startTime = date.atStartOfDay(zoneId).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(zoneId).toInstant()

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        StepsRecord.COUNT_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val burnedCalories = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
            val stepsCount = response[StepsRecord.COUNT_TOTAL] ?: 0L

            Result.success(
                DailyHealthActivity(
                    date = date,
                    burnedCalories = burnedCalories,
                    stepsCount = stepsCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLatestWeight(): Result<HealthWeightRecord?> {
        val client = healthConnectClientProvider()
            ?: return Result.failure(IllegalStateException("Health Connect no está disponible en este dispositivo."))

        return try {
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.before(Instant.now()),
                ascendingOrder = false,
                pageSize = 1
            )
            val response = client.readRecords(request)
            val latestRecord = response.records.firstOrNull()

            val result = latestRecord?.let {
                HealthWeightRecord(
                    weightKg = it.weight.inKilograms,
                    recordedAt = it.time
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
