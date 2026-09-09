package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository

/**
 * Caso de uso para eliminar una comida de la base de datos local y revocar/borrar su registro en Health Connect.
 */
class DeleteMealWithHealthSyncUseCase(
    private val mealRepository: MealRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(mealId: Long): Result<Unit> {
        val meal = mealRepository.getMealById(mealId).getOrNull()

        meal?.healthConnectRecordId?.takeIf { it.isNotBlank() }?.let { recordId ->
            try {
                val isAvailable = healthConnectRepository.isHealthConnectAvailable()
                if (isAvailable && healthConnectRepository.hasWriteNutritionPermission()) {
                    healthConnectRepository.deleteNutritionRecord(recordId)
                }
            } catch (_: Exception) {
                // Degradación agraciada: continuar con la eliminación local
            }
        }

        return mealRepository.deleteMeal(mealId)
    }
}
