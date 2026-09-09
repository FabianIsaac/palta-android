package com.calculadoracalorias.app.data.repository

import com.calculadoracalorias.app.data.local.dao.SupplementDao
import com.calculadoracalorias.app.data.local.dao.SupplementLogDao
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.domain.repository.SupplementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate

/**
 * Implementación local basada en Room para el catálogo de suplementos y registros diarios de toma.
 */
class LocalSupplementRepository(
    private val supplementDao: SupplementDao,
    private val supplementLogDao: SupplementLogDao
) : SupplementRepository {

    private suspend fun seedPreconfiguredIfEmpty() {
        try {
            val entities = Supplement.PRECONFIGURED_SUPPLEMENTS.map { it.toEntity() }
            supplementDao.insertAll(entities)
        } catch (_: Exception) {
            // Ignorar errores de siembra en caso de concurrencia
        }
    }

    override fun getAllSupplements(): Flow<List<Supplement>> {
        return supplementDao.getAllSupplements()
            .onStart { seedPreconfiguredIfEmpty() }
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getActiveSupplements(): Flow<List<Supplement>> {
        return supplementDao.getActiveSupplements()
            .onStart { seedPreconfiguredIfEmpty() }
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getSupplementsForDate(date: LocalDate): Flow<List<Supplement>> {
        val dateString = date.toString()
        return combine(
            supplementDao.getActiveSupplements().onStart { seedPreconfiguredIfEmpty() },
            supplementLogDao.getLogsForDate(dateString)
        ) { activeEntities, logs ->
            val takenIds = logs.map { it.supplementId }.toSet()
            activeEntities.map { entity ->
                entity.toDomain(isTakenToday = takenIds.contains(entity.id))
            }
        }
    }

    override fun getSupplementsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Map<LocalDate, List<Supplement>>> {
        val startStr = startDate.toString()
        val endStr = endDate.toString()
        return combine(
            supplementDao.getActiveSupplements().onStart { seedPreconfiguredIfEmpty() },
            supplementLogDao.getLogsBetweenDates(startStr, endStr)
        ) { activeEntities, logs ->
            val logsByDate = logs.groupBy { it.date }
            val dates = mutableListOf<LocalDate>()
            var curr = startDate
            while (!curr.isAfter(endDate)) {
                dates.add(curr)
                curr = curr.plusDays(1)
            }
            dates.associateWith { date ->
                val dayTakenIds = logsByDate[date.toString()]?.map { it.supplementId }?.toSet().orEmpty()
                activeEntities.map { entity ->
                    entity.toDomain(isTakenToday = dayTakenIds.contains(entity.id))
                }
            }
        }
    }

    override suspend fun toggleSupplementTaken(
        date: LocalDate,
        supplementId: String,
        isTaken: Boolean
    ): Result<Unit> {
        return try {
            val dateString = date.toString()
            if (isTaken) {
                supplementLogDao.insertLog(
                    SupplementLogEntity(
                        date = dateString,
                        supplementId = supplementId,
                        takenTimestamp = System.currentTimeMillis()
                    )
                )
            } else {
                supplementLogDao.deleteLog(date = dateString, supplementId = supplementId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveSupplement(supplement: Supplement): Result<Unit> {
        return try {
            supplementDao.insertOrUpdate(supplement.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleSupplementActive(supplementId: String, isActive: Boolean): Result<Unit> {
        return try {
            supplementDao.updateActiveStatus(supplementId, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSupplement(supplementId: String): Result<Unit> {
        return try {
            supplementDao.deleteById(supplementId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SupplementEntity.toDomain(isTakenToday: Boolean = false): Supplement = Supplement(
        id = id,
        name = name,
        dosageDescription = dosageDescription,
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        isActive = isActive,
        isCustom = isCustom,
        isTakenToday = isTakenToday
    )

    private fun Supplement.toEntity(): SupplementEntity = SupplementEntity(
        id = id,
        name = name,
        dosageDescription = dosageDescription,
        calories = calories,
        proteinGrams = proteinGrams,
        carbsGrams = carbsGrams,
        fatGrams = fatGrams,
        isActive = isActive,
        isCustom = isCustom
    )
}
