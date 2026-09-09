package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DayCalorieMetric
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.PeriodStatistics
import com.calculadoracalorias.app.domain.model.StatisticsRange
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Caso de uso para calcular estadísticas nutricionales agregadas en un rango de fechas.
 */
class GetPeriodStatisticsUseCase(
    private val mealRepository: MealRepository,
    private val dailyBudgetRepository: DailyBudgetRepository,
    private val supplementRepository: SupplementRepository,
    private val calculateDailyStreakUseCase: CalculateDailyStreakUseCase
) {

    operator fun invoke(
        range: StatisticsRange,
        endDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<PeriodStatistics> {
        val totalDays = range.days
        val startDate = endDate.minusDays((totalDays - 1).toLong())
        val startEpoch = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpoch = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val mealsFlow = mealRepository.getMealsForDay(startEpoch, endEpoch)
        val budgetFlow = dailyBudgetRepository.getDailyBudget()
        val supplementsFlow = supplementRepository.getSupplementsForDateRange(startDate, endDate)
        val streakFlow = calculateDailyStreakUseCase(currentDate = endDate, zoneId = zoneId)

        return combine(mealsFlow, budgetFlow, supplementsFlow, streakFlow) { meals, budget, supplementsByDate, streak ->
            val mealsByDate = meals.groupBy { meal ->
                Instant.ofEpochMilli(meal.timestamp).atZone(zoneId).toLocalDate()
            }

            val dayMetrics = mutableListOf<DayCalorieMetric>()
            var habitCompletedDays = 0
            var sumCalories = 0.0
            var sumProtein = 0.0
            var sumCarbs = 0.0
            var sumFat = 0.0

            var totalExpectedDoses = 0
            var totalTakenDoses = 0

            val today = LocalDate.now()
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dayMeals = mealsByDate[currentDate].orEmpty()
                val daySupplements = supplementsByDate[currentDate].orEmpty()

                val mealsCalories = dayMeals.sumOf { it.summary.totalCalories }
                val mealsProtein = dayMeals.sumOf { it.summary.totalProtein }
                val mealsCarbs = dayMeals.sumOf { it.summary.totalCarbs }
                val mealsFat = dayMeals.sumOf { it.summary.totalFat }

                val takenSupplements = daySupplements.filter { it.isTakenToday }
                val suppCalories = takenSupplements.sumOf { it.calories }
                val suppProtein = takenSupplements.sumOf { it.proteinGrams }
                val suppCarbs = takenSupplements.sumOf { it.carbsGrams }
                val suppFat = takenSupplements.sumOf { it.fatGrams }

                totalExpectedDoses += daySupplements.size
                totalTakenDoses += takenSupplements.size

                val dayTotalCalories = round(mealsCalories + suppCalories, 1)
                val dayTotalProtein = round(mealsProtein + suppProtein, 2)
                val dayTotalCarbs = round(mealsCarbs + suppCarbs, 2)
                val dayTotalFat = round(mealsFat + suppFat, 2)

                sumCalories += dayTotalCalories
                sumProtein += dayTotalProtein
                sumCarbs += dayTotalCarbs
                sumFat += dayTotalFat

                val hasBreakfast = dayMeals.any { it.category == MealCategory.DESAYUNO }
                val hasLunch = dayMeals.any { it.category == MealCategory.ALMUERZO }
                val hasDinner = dayMeals.any { it.category == MealCategory.ONCE_CENA }
                if (hasBreakfast && hasLunch && hasDinner) {
                    habitCompletedDays++
                }

                dayMetrics.add(
                    DayCalorieMetric(
                        date = currentDate,
                        calories = dayTotalCalories,
                        targetCalories = budget.targetCalories,
                        isToday = (currentDate == today)
                    )
                )

                currentDate = currentDate.plusDays(1)
            }

            val avgCalories = round(sumCalories / totalDays, 1)
            val avgProtein = round(sumProtein / totalDays, 1)
            val avgCarbs = round(sumCarbs / totalDays, 1)
            val avgFat = round(sumFat / totalDays, 1)

            val adherencePercentage = if (totalExpectedDoses > 0) {
                round((totalTakenDoses.toDouble() / totalExpectedDoses.toDouble()) * 100.0, 1)
            } else {
                0.0
            }

            PeriodStatistics(
                range = range,
                startDate = startDate,
                endDate = endDate,
                averageDailyCalories = avgCalories,
                targetDailyCalories = budget.targetCalories,
                averageProteinGrams = avgProtein,
                averageCarbsGrams = avgCarbs,
                averageFatGrams = avgFat,
                habitCompletedDaysCount = habitCompletedDays,
                totalPeriodDays = totalDays,
                currentStreakDays = streak.currentStreakDays,
                supplementAdherencePercentage = adherencePercentage,
                dailyCalorieMetrics = dayMetrics
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
