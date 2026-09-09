package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.DailySummary
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

/**
 * Caso de uso para obtener el resumen consolidado del día, integrando presupuesto calórico
 * y comidas registradas divididas por categorías culturales chilenas.
 */
class GetDailyMealSummaryUseCase(
    private val mealRepository: MealRepository,
    private val dailyBudgetRepository: DailyBudgetRepository
) {
    operator fun invoke(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<DailySummary> {
        val startOfDayTimestamp = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDayTimestamp = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val mealsFlow: Flow<List<MealEntry>> = mealRepository.getMealsForDay(
            startOfDayTimestamp = startOfDayTimestamp,
            endOfDayTimestamp = endOfDayTimestamp
        )

        val budgetFlow: Flow<DailyMacroBudget> = dailyBudgetRepository.getDailyBudget()

        return combine(mealsFlow, budgetFlow) { meals, budget ->
            val totalCalories = round(meals.sumOf { it.summary.totalCalories }, 1)
            val totalProtein = round(meals.sumOf { it.summary.totalProtein }, 2)
            val totalCarbs = round(meals.sumOf { it.summary.totalCarbs }, 2)
            val totalFat = round(meals.sumOf { it.summary.totalFat }, 2)

            val consumed = NutritionSummary(
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat
            )

            val remainingCalories = round(budget.targetCalories - totalCalories, 1)

            val mealsByCategory = MealCategory.entries.associateWith { category ->
                meals.filter { it.category == category }
            }

            DailySummary(
                dateEpochDay = date.toEpochDay(),
                budget = budget,
                consumed = consumed,
                remainingCalories = remainingCalories,
                mealsByCategory = mealsByCategory
            )
        }
    }

    private fun round(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
