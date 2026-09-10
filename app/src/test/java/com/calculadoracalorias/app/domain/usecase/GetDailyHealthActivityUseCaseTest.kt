package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DailyHealthActivity
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetDailyHealthActivityUseCaseTest {

    private val healthConnectRepository: HealthConnectRepository = mockk()
    private lateinit var useCase: GetDailyHealthActivityUseCase
    private val testDate: LocalDate = LocalDate.of(2026, 9, 9)

    @BeforeEach
    fun setUp() {
        useCase = GetDailyHealthActivityUseCase(healthConnectRepository)
    }

    @Test
    @DisplayName("Debe retornar ceros si la sincronización está deshabilitada por el usuario")
    fun testWhenSyncDisabledReturnsZeros() = runBlocking {
        val result = useCase(testDate, isSyncEnabled = false)

        assertEquals(testDate, result.date)
        assertEquals(0.0, result.burnedCalories)
        assertEquals(0L, result.stepsCount)

        coVerify(exactly = 0) { healthConnectRepository.isHealthConnectAvailable() }
        coVerify(exactly = 0) { healthConnectRepository.getDailyActivity(any()) }
    }

    @Test
    @DisplayName("Debe retornar ceros si Health Connect no está disponible en el dispositivo")
    fun testWhenHealthConnectNotAvailableReturnsZeros() = runBlocking {
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns false

        val result = useCase(testDate, isSyncEnabled = true)

        assertEquals(0.0, result.burnedCalories)
        assertEquals(0L, result.stepsCount)
        coVerify(exactly = 0) { healthConnectRepository.getDailyActivity(any()) }
    }

    @Test
    @DisplayName("Debe retornar ceros si los permisos de actividad no están concedidos")
    fun testWhenNoPermissionsReturnsZeros() = runBlocking {
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasActivityPermissions() } returns false

        val result = useCase(testDate, isSyncEnabled = true)

        assertEquals(0.0, result.burnedCalories)
        assertEquals(0L, result.stepsCount)
        coVerify(exactly = 0) { healthConnectRepository.getDailyActivity(any()) }
    }

    @Test
    @DisplayName("Debe retornar la actividad correcta si tiene permisos y datos disponibles")
    fun testWhenSuccessReturnsActivityData() = runBlocking {
        val expectedActivity = DailyHealthActivity(
            date = testDate,
            burnedCalories = 385.5,
            stepsCount = 8420L
        )
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasActivityPermissions() } returns true
        coEvery { healthConnectRepository.getDailyActivity(testDate) } returns Result.success(expectedActivity)

        val result = useCase(testDate, isSyncEnabled = true)

        assertEquals(385.5, result.burnedCalories)
        assertEquals(8420L, result.stepsCount)
    }

    @Test
    @DisplayName("Debe manejar fallos del repositorio de forma resiliente retornando valores por defecto")
    fun testWhenRepositoryFailsReturnsSafeDefaults() = runBlocking {
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasActivityPermissions() } returns true
        coEvery { healthConnectRepository.getDailyActivity(testDate) } returns Result.failure(RuntimeException("Error IPC"))

        val result = useCase(testDate, isSyncEnabled = true)

        assertEquals(testDate, result.date)
        assertEquals(0.0, result.burnedCalories)
        assertEquals(0L, result.stepsCount)
    }
}
