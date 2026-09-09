package com.calculadoracalorias.app.domain.model

/**
 * Representa el estado de racha de días consecutivos cumplidos y el avance del día de hoy
 * en las tres comidas principales chilenas (Desayuno, Almuerzo, Once / Cena).
 */
data class DailyStreak(
    val currentStreakDays: Int = 0,
    val hasBreakfastToday: Boolean = false,
    val hasLunchToday: Boolean = false,
    val hasDinnerToday: Boolean = false
) {
    val isTodayCompleted: Boolean
        get() = hasBreakfastToday && hasLunchToday && hasDinnerToday

    val missingMealsToday: List<MealCategory>
        get() = buildList {
            if (!hasBreakfastToday) add(MealCategory.DESAYUNO)
            if (!hasLunchToday) add(MealCategory.ALMUERZO)
            if (!hasDinnerToday) add(MealCategory.ONCE_CENA)
        }
}
