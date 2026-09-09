package com.calculadoracalorias.app.data.repository

import com.calculadoracalorias.app.data.local.dao.SupplementDao
import com.calculadoracalorias.app.data.local.dao.SupplementLogDao
import com.calculadoracalorias.app.data.local.entity.SupplementEntity
import com.calculadoracalorias.app.data.local.entity.SupplementLogEntity
import com.calculadoracalorias.app.domain.model.Supplement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalSupplementRepositoryTest {

    private val supplementDao: SupplementDao = mockk()
    private val supplementLogDao: SupplementLogDao = mockk()
    private lateinit var repository: LocalSupplementRepository
    private val testDate = LocalDate.of(2026, 9, 8)

    @BeforeEach
    fun setUp() {
        coEvery { supplementDao.insertAll(any()) } returns Unit
        repository = LocalSupplementRepository(supplementDao, supplementLogDao)
    }

    @Test
    @DisplayName("Debe reflejar isTakenToday=true cuando existe registro en base de datos")
    fun testGetSupplementsForDateWithExistingLog() = runBlocking {
        val dateStr = testDate.toString()
        val mockEntities = listOf(
            SupplementEntity(
                id = "omega_3",
                name = "Omega 3",
                dosageDescription = "2 cápsulas",
                calories = 18.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 2.0,
                isActive = true,
                isCustom = false
            )
        )
        val mockLogs = listOf(
            SupplementLogEntity(date = dateStr, supplementId = "omega_3", takenTimestamp = 123456789L)
        )
        every { supplementDao.getActiveSupplements() } returns flowOf(mockEntities)
        every { supplementLogDao.getLogsForDate(dateStr) } returns flowOf(mockLogs)

        val supplements = repository.getSupplementsForDate(testDate).first()

        assertEquals(1, supplements.size)
        assertEquals("omega_3", supplements.first().id)
        assertTrue(supplements.first().isTakenToday)
    }

    @Test
    @DisplayName("Debe reflejar isTakenToday=false cuando no existe registro en base de datos")
    fun testGetSupplementsForDateWithoutLog() = runBlocking {
        val dateStr = testDate.toString()
        val mockEntities = listOf(
            SupplementEntity(
                id = "creatina",
                name = "Creatina",
                dosageDescription = "5g",
                calories = 0.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 0.0,
                isActive = true,
                isCustom = false
            )
        )
        every { supplementDao.getActiveSupplements() } returns flowOf(mockEntities)
        every { supplementLogDao.getLogsForDate(dateStr) } returns flowOf(emptyList())

        val supplements = repository.getSupplementsForDate(testDate).first()

        assertEquals(1, supplements.size)
        assertFalse(supplements.first().isTakenToday)
    }

    @Test
    @DisplayName("toggleSupplementTaken con isTaken=true debe insertar log")
    fun testToggleSupplementTakenTrue() = runBlocking {
        val dateStr = testDate.toString()
        coEvery { supplementLogDao.insertLog(any()) } returns Unit

        val result = repository.toggleSupplementTaken(testDate, "omega_3", true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            supplementLogDao.insertLog(match { it.date == dateStr && it.supplementId == "omega_3" })
        }
    }

    @Test
    @DisplayName("toggleSupplementTaken con isTaken=false debe eliminar log")
    fun testToggleSupplementTakenFalse() = runBlocking {
        val dateStr = testDate.toString()
        coEvery { supplementLogDao.deleteLog(dateStr, "omega_3") } returns Unit

        val result = repository.toggleSupplementTaken(testDate, "omega_3", false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            supplementLogDao.deleteLog(dateStr, "omega_3")
        }
    }

    @Test
    @DisplayName("saveSupplement debe llamar a insertOrUpdate en SupplementDao")
    fun testSaveSupplement() = runBlocking {
        coEvery { supplementDao.insertOrUpdate(any()) } returns Unit

        val supplement = Supplement(
            id = "custom_1",
            name = "Whey Isolate",
            dosageDescription = "30g",
            calories = 120.0,
            proteinGrams = 25.0,
            carbsGrams = 1.0,
            fatGrams = 1.0,
            isActive = true,
            isCustom = true
        )

        val result = repository.saveSupplement(supplement)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            supplementDao.insertOrUpdate(match { it.id == "custom_1" && it.name == "Whey Isolate" })
        }
    }

    @Test
    @DisplayName("toggleSupplementActive debe llamar a updateActiveStatus")
    fun testToggleSupplementActive() = runBlocking {
        coEvery { supplementDao.updateActiveStatus("omega_3", false) } returns Unit

        val result = repository.toggleSupplementActive("omega_3", false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            supplementDao.updateActiveStatus("omega_3", false)
        }
    }

    @Test
    @DisplayName("deleteSupplement debe llamar a deleteById")
    fun testDeleteSupplement() = runBlocking {
        coEvery { supplementDao.deleteById("custom_1") } returns Unit

        val result = repository.deleteSupplement("custom_1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            supplementDao.deleteById("custom_1")
        }
    }
}
