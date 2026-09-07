package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository

data class SaveMealResult(
    val mealId: Long,
    val healthConnectRecordId: String?,
    val syncedWithHealthConnect: Boolean
)

/**
 * Caso de uso responsable de persistir una comida confirmada en la base de datos local (Room)
 * y sincronizar automáticamente con Google Health Connect si los permisos están concedidos.
 *
 * Implementa degradación agraciada (graceful degradation): si Health Connect no está disponible
 * o el permiso no está otorgado, la comida se guarda localmente sin interrumpir al usuario.
 */
class SaveMealWithHealthSyncUseCase(
    private val mealRepository: MealRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(
        mealName: String,
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>
    ): Result<SaveMealResult> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("No se pueden registrar comidas sin alimentos."))
        }

        val summary = NutritionSummary.fromItems(items)

        // Intento de sincronización con Health Connect si el SDK está disponible y tiene permisos
        var healthRecordId: String? = null
        var isSynced = false

        try {
            val isAvailable = healthConnectRepository.isHealthConnectAvailable()
            val hasPermission = isAvailable && healthConnectRepository.hasWriteNutritionPermission()

            if (hasPermission) {
                val syncResult = healthConnectRepository.writeNutritionRecord(
                    mealName = mealName.ifBlank { category.displayName },
                    mealCategory = category,
                    timestamp = timestamp,
                    calories = summary.totalCalories,
                    proteinGrams = summary.totalProtein,
                    carbsGrams = summary.totalCarbs,
                    fatGrams = summary.totalFat
                )
                if (syncResult.isSuccess) {
                    healthRecordId = syncResult.getOrNull()
                    isSynced = true
                }
            }
        } catch (_: Exception) {
            // Manejo silencioso: la persistencia local es prioritaria
            healthRecordId = null
            isSynced = false
        }

        // Persistencia obligatoria en Room
        val localSaveResult = mealRepository.saveMeal(
            category = category,
            timestamp = timestamp,
            items = items,
            healthConnectRecordId = healthRecordId
        )

        return localSaveResult.fold(
            onSuccess = { mealId ->
                Result.success(
                    SaveMealResult(
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
