package com.calculadoracalorias.app.domain.model

import java.time.Instant

/**
 * Representa un registro de peso corporal obtenido desde balanzas inteligentes o aplicaciones de salud a través de Health Connect.
 *
 * @property weightKg Peso corporal registrado en kilogramos.
 * @property recordedAt Marca de tiempo exacta del pesaje.
 */
data class HealthWeightRecord(
    val weightKg: Double,
    val recordedAt: Instant
)
