package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de dominio para obtener el presupuesto calórico y de macronutrientes del usuario.
 */
interface DailyBudgetRepository {
    fun getDailyBudget(): Flow<DailyMacroBudget>
}
