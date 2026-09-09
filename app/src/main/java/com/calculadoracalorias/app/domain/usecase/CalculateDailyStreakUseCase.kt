package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Caso de uso para calcular la racha de "Días Buenos" consecutivos.
 * Un día calendario se clasifica como cumplido si cuenta con al menos un registro
 * en cada una de las 3 comidas principales chilenas: Desayuno, Almuerzo y Once / Cena.
 */
class CalculateDailyStreakUseCase(
    private val mealRepository: MealRepository
) {

    operator fun invoke(
        currentDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        lookbackDays: Long = 60
    ): Flow<DailyStreak> {
        val startOfLookback = currentDate.minusDays(lookbackDays).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfCurrentDay = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        return mealRepository.getMealsForDay(startOfLookback, endOfCurrentDay).map { meals ->
            calculateStreak(meals, currentDate, zoneId)
        }
    }

    fun calculateStreak(
        meals: List<MealEntry>,
        currentDate: LocalDate,
        zoneId: ZoneId
    ): DailyStreak {
        val mealsByDate = meals.groupBy { meal ->
            Instant.ofEpochMilli(meal.timestamp).atZone(zoneId).toLocalDate()
        }

        fun isDayCompleted(date: LocalDate): Boolean {
            val dayMeals = mealsByDate[date].orEmpty()
            val hasBreakfast = dayMeals.any { it.category == MealCategory.DESAYUNO }
            val hasLunch = dayMeals.any { it.category == MealCategory.ALMUERZO }
            val hasDinner = dayMeals.any { it.category == MealCategory.ONCE_CENA }
            return hasBreakfast && hasLunch && hasDinner
        }

        val todayMeals = mealsByDate[currentDate].orEmpty()
        val hasBreakfastToday = todayMeals.any { it.category == MealCategory.DESAYUNO }
        val hasLunchToday = todayMeals.any { it.category == MealCategory.ALMUERZO }
        val hasDinnerToday = todayMeals.any { it.category == MealCategory.ONCE_CENA }
        val isTodayCompleted = hasBreakfastToday && hasLunchToday && hasDinnerToday

        var streak = 0
        val startDate = if (isTodayCompleted) {
            streak = 1
            currentDate.minusDays(1)
        } else {
            currentDate.minusDays(1)
        }

        var checkDate = startDate
        while (isDayCompleted(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        return DailyStreak(
            currentStreakDays = streak,
            hasBreakfastToday = hasBreakfastToday,
            hasLunchToday = hasLunchToday,
            hasDinnerToday = hasDinnerToday
        )
    }
}
