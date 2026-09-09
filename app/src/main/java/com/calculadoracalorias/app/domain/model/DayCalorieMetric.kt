package com.calculadoracalorias.app.domain.model

import java.time.LocalDate

/**
 * Métricas calóricas de un día específico para representar barras de consumo.
 */
data class DayCalorieMetric(
    val date: LocalDate,
    val calories: Double,
    val targetCalories: Double,
    val isToday: Boolean = false
)
