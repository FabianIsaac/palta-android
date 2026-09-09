package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.Supplement
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de repositorio para el catálogo de suplementos y el registro diario de toma.
 */
interface SupplementRepository {

    /**
     * Flujo de todos los suplementos configurados en el catálogo (activos e inactivos).
     */
    fun getAllSupplements(): Flow<List<Supplement>>

    /**
     * Flujo de suplementos actualmente activos en la rutina.
     */
    fun getActiveSupplements(): Flow<List<Supplement>>

    /**
     * Flujo de suplementos activos para la fecha especificada con su estado de toma diario.
     */
    fun getSupplementsForDate(date: LocalDate): Flow<List<Supplement>>

    /**
     * Flujo de suplementos activos para cada fecha en el rango especificado con su estado de toma.
     */
    fun getSupplementsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<Supplement>>>

    /**
     * Alterna o establece el estado de toma de un suplemento en la fecha especificada.
     */
    suspend fun toggleSupplementTaken(date: LocalDate, supplementId: String, isTaken: Boolean): Result<Unit>

    /**
     * Guarda o actualiza un suplemento en el catálogo persistente.
     */
    suspend fun saveSupplement(supplement: Supplement): Result<Unit>

    /**
     * Activa o desactiva un suplemento en la rutina del usuario.
     */
    suspend fun toggleSupplementActive(supplementId: String, isActive: Boolean): Result<Unit>

    /**
     * Elimina un suplemento del catálogo persistente.
     */
    suspend fun deleteSupplement(supplementId: String): Result<Unit>
}
