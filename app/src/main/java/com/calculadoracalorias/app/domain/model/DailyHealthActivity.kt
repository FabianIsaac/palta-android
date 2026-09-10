package com.calculadoracalorias.app.domain.model

import java.time.LocalDate

/**
 * Representa la actividad física y gasto energético activo registrado en Health Connect para una fecha determinada.
 *
 * @property date Fecha de la actividad.
 * @property burnedCalories Calorías activas quemadas por ejercicio o movimiento físico (kcal).
 * @property stepsCount Conteo total de pasos registrados para el día.
 */
data class DailyHealthActivity(
    val date: LocalDate,
    val burnedCalories: Double = 0.0,
    val stepsCount: Long = 0L
)
