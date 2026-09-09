package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DayHabitSummary
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Caso de uso para obtener la grilla mensual del calendario con el estado de hábitos,
 * consumo nutricional y suplementos para cada día (Lunes a Domingo).
 */
class GetMonthlyCalendarHabitsUseCase(
    private val mealRepository: MealRepository,
    private val dailyBudgetRepository: DailyBudgetRepository,
    private val supplementRepository: SupplementRepository
) {

    operator fun invoke(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<List<DayHabitSummary>> {
        val firstDayOfMonth = yearMonth.atDay(1)
        val leadingDays = (firstDayOfMonth.dayOfWeek.value - 1).toLong()
        val gridStartDate = firstDayOfMonth.minusDays(leadingDays)

        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val trailingDays = ((7 - lastDayOfMonth.dayOfWeek.value) % 7).toLong()
        val gridEndDate = lastDayOfMonth.plusDays(trailingDays)

        val startEpoch = gridStartDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endEpoch = gridEndDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        val mealsFlow = mealRepository.getMealsForDay(startEpoch, endEpoch)
        val budgetFlow = dailyBudgetRepository.getDailyBudget()
        val supplementsFlow = supplementRepository.getSupplementsForDateRange(gridStartDate, gridEndDate)

        return combine(mealsFlow, budgetFlow, supplementsFlow) { meals, budget, supplementsByDate ->
            val mealsByDate = meals.groupBy { meal ->
                Instant.ofEpochMilli(meal.timestamp).atZone(zoneId).toLocalDate()
            }

            val today = LocalDate.now()
            val daySummaries = mutableListOf<DayHabitSummary>()
            var curr = gridStartDate

            while (!curr.isAfter(gridEndDate)) {
                val dayMeals = mealsByDate[curr].orEmpty()
                val daySupplements = supplementsByDate[curr].orEmpty()

                val hasBreakfast = dayMeals.any { it.category == MealCategory.DESAYUNO }
                val hasLunch = dayMeals.any { it.category == MealCategory.ALMUERZO }
                val hasDinner = dayMeals.any { it.category == MealCategory.ONCE_CENA }

                val mealsCalories = dayMeals.sumOf { it.summary.totalCalories }
                val mealsProtein = dayMeals.sumOf { it.summary.totalProtein }
                val mealsCarbs = dayMeals.sumOf { it.summary.totalCarbs }
                val mealsFat = dayMeals.sumOf { it.summary.totalFat }

                val takenSupplements = daySupplements.filter { it.isTakenToday }
                val suppCalories = takenSupplements.sumOf { it.calories }
                val suppProtein = takenSupplements.sumOf { it.proteinGrams }
                val suppCarbs = takenSupplements.sumOf { it.carbsGrams }
                val suppFat = takenSupplements.sumOf { it.fatGrams }

                val totalCalories = round(mealsCalories + suppCalories, 1)
                val totalProtein = round(mealsProtein + suppProtein, 2)
                val totalCarbs = round(mealsCarbs + suppCarbs, 2)
                val totalFat = round(mealsFat + suppFat, 2)

                daySummaries.add(
                    DayHabitSummary(
                        date = curr,
                        isCurrentMonth = (curr.year == yearMonth.year && curr.month == yearMonth.month),
                        isToday = (curr == today),
                        hasBreakfast = hasBreakfast,
                        hasLunch = hasLunch,
                        hasDinner = hasDinner,
                        totalCalories = totalCalories,
                        targetCalories = budget.targetCalories,
                        totalProteinGrams = totalProtein,
                        totalCarbsGrams = totalCarbs,
                        totalFatGrams = totalFat,
                        supplementsTakenCount = takenSupplements.size,
                        totalSupplementsCount = daySupplements.size
                    )
                )

                curr = curr.plusDays(1)
            }

            daySummaries
        }
    }

    private fun round(value: Double, decimals: Int): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value.toString())
            .setScale(decimals, RoundingMode.HALF_UP)
            .toDouble()
    }
}
