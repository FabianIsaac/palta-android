package com.calculadoracalorias.app.domain.model

import java.time.LocalDate

/**
 * Consolidado de estadísticas agregadas y métricas para un rango temporal determinado.
 */
data class PeriodStatistics(
    val range: StatisticsRange,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val averageDailyCalories: Double,
    val targetDailyCalories: Double,
    val averageProteinGrams: Double,
    val averageCarbsGrams: Double,
    val averageFatGrams: Double,
    val habitCompletedDaysCount: Int,
    val totalPeriodDays: Int,
    val currentStreakDays: Int,
    val supplementAdherencePercentage: Double,
    val dailyCalorieMetrics: List<DayCalorieMetric>
)
