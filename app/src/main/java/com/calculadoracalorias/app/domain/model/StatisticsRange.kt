package com.calculadoracalorias.app.domain.model

/**
 * Rangos temporales configurables para el análisis de estadísticas nutricionales e historial.
 */
enum class StatisticsRange(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 días"),
    LAST_14_DAYS(14, "14 días"),
    LAST_30_DAYS(30, "30 días")
}
