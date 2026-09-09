package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import java.time.Instant
import java.time.ZoneId

data class SaveMealResult(
    val mealId: Long,
    val healthConnectRecordId: String?,
    val syncedWithHealthConnect: Boolean,
    val isConsolidated: Boolean = false
)

/**
 * Caso de uso responsable de persistir una comida confirmada en la base de datos local (Room)
 * y sincronizar automáticamente con Google Health Connect si los permisos están concedidos.
 *
 * Implementa consolidación inteligente: si existe una comida previa en la misma categoría
 * dentro de una ventana de 30 minutos, une los alimentos y actualiza el registro existente.
 *
 * Implementa degradación agraciada (graceful degradation): si Health Connect no está disponible
 * o el permiso no está otorgado, la comida se guarda localmente sin interrumpir al usuario.
 */
class SaveMealWithHealthSyncUseCase(
    private val mealRepository: MealRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val updateMealWithHealthSyncUseCase: UpdateMealWithHealthSyncUseCase = UpdateMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
) {
    companion object {
        const val CONSOLIDATION_WINDOW_MS = 30 * 60 * 1000L
    }

    suspend operator fun invoke(
        mealName: String,
        category: MealCategory,
        timestamp: Long,
        items: List<ScannedFoodItem>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        rawDescription: String? = null,
        isPendingAiRefinement: Boolean = false
    ): Result<SaveMealResult> {
        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("No se pueden registrar comidas sin alimentos."))
        }

        // 1. Detección de consolidación inteligente (ventana de 30 minutos)
        try {
            val localDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

            val recentMealResult = mealRepository.getMostRecentMealForCategory(
                category = category,
                startTime = startOfDay,
                endTime = endOfDay
            )

            val recentMeal = recentMealResult.getOrNull()
            if (recentMeal != null) {
                val timeDifference = timestamp - recentMeal.timestamp
                if (timeDifference in 0..CONSOLIDATION_WINDOW_MS) {
                    // CONSOLIDAR: Unir alimentos existentes con los nuevos alimentos
                    val consolidatedItems = recentMeal.items + items
                    val updateResult = updateMealWithHealthSyncUseCase(
                        mealId = recentMeal.id,
                        category = category,
                        timestamp = recentMeal.timestamp,
                        items = consolidatedItems,
                        mealName = mealName.ifBlank { category.displayName },
                        existingHealthConnectRecordId = recentMeal.healthConnectRecordId
                    )

                    return updateResult.fold(
                        onSuccess = { res ->
                            Result.success(
                                SaveMealResult(
                                    mealId = res.mealId,
                                    healthConnectRecordId = res.healthConnectRecordId,
                                    syncedWithHealthConnect = res.syncedWithHealthConnect,
                                    isConsolidated = true
                                )
                            )
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                }
            }
        } catch (_: Exception) {
            // Degradación agraciada: si la verificación falla, se procede como comida independiente
        }

        // 2. Registro independiente: Cálculo de resumen y sincronización Health Connect
        val summary = NutritionSummary.fromItems(items)
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
            healthRecordId = null
            isSynced = false
        }

        // Persistencia obligatoria en Room
        val localSaveResult = mealRepository.saveMeal(
            category = category,
            timestamp = timestamp,
            items = items,
            healthConnectRecordId = healthRecordId,
            rawDescription = rawDescription,
            isPendingAiRefinement = isPendingAiRefinement
        )

        return localSaveResult.fold(
            onSuccess = { mealId ->
                Result.success(
                    SaveMealResult(
                        mealId = mealId,
                        healthConnectRecordId = healthRecordId,
                        syncedWithHealthConnect = isSynced,
                        isConsolidated = false
                    )
                )
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
