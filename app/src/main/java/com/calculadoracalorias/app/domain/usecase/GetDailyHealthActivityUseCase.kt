package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyHealthActivity
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import java.time.LocalDate

/**
 * Caso de uso para obtener el gasto calórico activo y pasos diarios registrados en Health Connect.
 * Si la sincronización no está habilitada o faltan permisos, retorna un objeto con valores en cero de forma segura.
 */
class GetDailyHealthActivityUseCase(
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend operator fun invoke(
        date: LocalDate,
        isSyncEnabled: Boolean = true
    ): DailyHealthActivity {
        if (!isSyncEnabled) {
            return DailyHealthActivity(date = date)
        }

        val isAvailable = healthConnectRepository.isHealthConnectAvailable()
        if (!isAvailable) {
            return DailyHealthActivity(date = date)
        }

        val hasPermissions = healthConnectRepository.hasActivityPermissions()
        if (!hasPermissions) {
            return DailyHealthActivity(date = date)
        }

        return healthConnectRepository.getDailyActivity(date)
            .getOrDefault(DailyHealthActivity(date = date))
    }
}
