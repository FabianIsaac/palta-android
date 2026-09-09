package com.calculadoracalorias.app.domain.model

import java.time.LocalDate

/**
 * Resumen de hábitos y consumo para cada celda del calendario mensual.
 */
data class DayHabitSummary(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasBreakfast: Boolean,
    val hasLunch: Boolean,
    val hasDinner: Boolean,
    val totalCalories: Double,
    val targetCalories: Double,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val supplementsTakenCount: Int,
    val totalSupplementsCount: Int
) {
    val isHabitCompleted: Boolean
        get() = hasBreakfast && hasLunch && hasDinner

    val hasSupplementsTaken: Boolean
        get() = supplementsTakenCount > 0
}
