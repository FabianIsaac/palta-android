package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository

data class UpdateMealResult(
    val mealId: Long,
    val healthConnectRecordId: String?,
    val syncedWithHealthConnect: Boolean
)

/**
 * Caso de uso para actualizar una comida previamente registrada (sus porciones, alimentos o categoría)
 * y sincronizar las modificaciones en Google Health Connect.
 */
class UpdateMealWithHealthSyncUseCase(
    private val mealRepository: MealRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(
        mealId: Long,
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        mealName: String = "",
        existingHealthConnectRecordId: String? = null
    ): Result<UpdateMealResult> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("No se pueden guardar comidas sin alimentos."))
        }

        if (items.any { it.servingGrams <= 0.0 }) {
            return Result.failure(IllegalArgumentException("Las porciones de los alimentos deben ser mayores a 0 gramos."))
        }

        val summary = NutritionSummary.fromItems(items)
        var healthRecordId = existingHealthConnectRecordId
        var isSynced = false

        // Si no se proveyó el id de Health Connect explícitamente, consultamos si la comida ya tenía uno
        if (healthRecordId == null) {
            val existingMeal = mealRepository.getMealById(mealId).getOrNull()
            healthRecordId = existingMeal?.healthConnectRecordId
        }

        try {
            val isAvailable = healthConnectRepository.isHealthConnectAvailable()
            val hasPermission = isAvailable && healthConnectRepository.hasWriteNutritionPermission()

            if (hasPermission) {
                if (!healthRecordId.isNullOrBlank()) {
                    val updateResult = healthConnectRepository.updateNutritionRecord(
                        recordId = healthRecordId,
                        mealName = mealName.ifBlank { category.displayName },
                        mealCategory = category,
                        timestamp = timestamp,
                        calories = summary.totalCalories,
                        proteinGrams = summary.totalProtein,
                        carbsGrams = summary.totalCarbs,
                        fatGrams = summary.totalFat
                    )
                    isSynced = updateResult.isSuccess
                } else {
                    val writeResult = healthConnectRepository.writeNutritionRecord(
                        mealName = mealName.ifBlank { category.displayName },
                        mealCategory = category,
                        timestamp = timestamp,
                        calories = summary.totalCalories,
                        proteinGrams = summary.totalProtein,
                        carbsGrams = summary.totalCarbs,
                        fatGrams = summary.totalFat
                    )
                    if (writeResult.isSuccess) {
                        healthRecordId = writeResult.getOrNull()
                        isSynced = true
                    }
                }
            }
        } catch (_: Exception) {
            // Degradación agraciada: la persistencia local se mantiene
            isSynced = false
        }

        val updateLocalResult = mealRepository.updateMeal(
            mealId = mealId,
            category = category,
            timestamp = timestamp,
            items = items,
            healthConnectRecordId = healthRecordId
        )

        return updateLocalResult.fold(
            onSuccess = {
                Result.success(
                    UpdateMealResult(
                        mealId = mealId,
                        healthConnectRecordId = healthRecordId,
                        syncedWithHealthConnect = isSynced
                    )
                )
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
