package com.calculadoracalorias.app.domain.model

import java.time.LocalTime

/**
 * Define las ventanas horarias configurables para las comidas del día en Chile.
 * Los valores por defecto se ajustan a las costumbres culturales del país.
 */
data class MealTimeWindows(
    val breakfastStart: LocalTime = LocalTime.of(6, 0),
    val breakfastEnd: LocalTime = LocalTime.of(11, 30),
    val lunchStart: LocalTime = LocalTime.of(11, 30),
    val lunchEnd: LocalTime = LocalTime.of(16, 0),
    val dinnerStart: LocalTime = LocalTime.of(16, 0),
    val dinnerEnd: LocalTime = LocalTime.of(22, 0)
) {
    /**
     * Infiere la categoría de comida chilena basada en la hora entregada.
     * Si no se especifica hora, se utiliza la hora actual del sistema.
     */
    fun detectCategory(time: LocalTime = LocalTime.now()): MealCategory {
        return when {
            !time.isBefore(breakfastStart) && time.isBefore(breakfastEnd) -> MealCategory.DESAYUNO
            !time.isBefore(lunchStart) && time.isBefore(lunchEnd) -> MealCategory.ALMUERZO
            !time.isBefore(dinnerStart) && time.isBefore(dinnerEnd) -> MealCategory.ONCE_CENA
            else -> MealCategory.COLACIONES
        }
    }
}
