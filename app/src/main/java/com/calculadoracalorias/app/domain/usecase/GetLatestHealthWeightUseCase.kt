package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.HealthWeightRecord
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository

/**
 * Caso de uso para consultar el registro de peso más reciente desde Health Connect,
 * registrado por balanzas inteligentes o apps de salud.
 */
class GetLatestHealthWeightUseCase(
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(): Result<HealthWeightRecord?> {
        val isAvailable = healthConnectRepository.isHealthConnectAvailable()
        if (!isAvailable) {
            return Result.success(null)
        }

        val hasPermission = healthConnectRepository.hasWeightPermission()
        if (!hasPermission) {
            return Result.failure(SecurityException("Permiso de peso no concedido en Health Connect."))
        }

        return healthConnectRepository.getLatestWeight()
    }
}
