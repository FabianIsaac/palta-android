package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer

/**
 * Caso de uso responsable de consultar las comidas pendientes de refinamiento por IA
 * (registradas en modo offline o fallback) y procesarlas cuando se dispone de conexión.
 */
class RefinePendingMealsUseCase(
    private val mealRepository: MealRepository,
    private val analyzer: NaturalLanguageMealAnalyzer,
    private val healthConnectRepository: HealthConnectRepository? = null
) {
    suspend operator fun invoke(): Result<List<MealEntry>> {
        val pendingMeals = mealRepository.getPendingRefinementMeals().getOrElse { return Result.failure(it) }
        val refined = mutableListOf<MealEntry>()

        for (meal in pendingMeals) {
            val text = meal.rawDescription?.trim()
            if (text.isNullOrBlank()) continue

            val analysisResult = analyzer.analyzeTextDescription(text)
            if (analysisResult.isSuccess) {
                val detected = analysisResult.getOrThrow()
                if (detected.items.isNotEmpty()) {
                    val summary = NutritionSummary.fromItems(detected.items)
                    val healthRecordId = meal.healthConnectRecordId

                    if (healthConnectRepository != null && !healthRecordId.isNullOrBlank()) {
                        try {
                            if (healthConnectRepository.isHealthConnectAvailable() && healthConnectRepository.hasWriteNutritionPermission()) {
                                healthConnectRepository.updateNutritionRecord(
                                    recordId = healthRecordId,
                                    mealName = meal.category.displayName,
                                    mealCategory = meal.category,
                                    timestamp = meal.timestamp,
                                    calories = summary.totalCalories,
                                    proteinGrams = summary.totalProtein,
                                    carbsGrams = summary.totalCarbs,
                                    fatGrams = summary.totalFat
                                )
                            }
                        } catch (_: Exception) {
                            // Degradación agraciada
                        }
                    }

                    val updatedMeal = meal.copy(
                        items = detected.items,
                        summary = summary,
                        isPendingAiRefinement = false
                    )
                    val updateResult = mealRepository.updateMeal(updatedMeal)
                    if (updateResult.isSuccess) {
                        refined.add(updatedMeal)
                    }
                }
            }
        }
        return Result.success(refined)
    }
}
