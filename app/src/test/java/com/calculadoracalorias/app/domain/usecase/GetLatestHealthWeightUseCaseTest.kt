package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.HealthWeightRecord
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class GetLatestHealthWeightUseCaseTest {

    private val healthConnectRepository: HealthConnectRepository = mockk()
    private lateinit var useCase: GetLatestHealthWeightUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetLatestHealthWeightUseCase(healthConnectRepository)
    }

    @Test
    @DisplayName("Debe retornar null exitosamente si Health Connect no está disponible")
    fun testWhenHealthConnectUnavailableReturnsNull() = runBlocking {
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns false

        val result = useCase()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    @DisplayName("Debe retornar fallo SecurityException si no tiene permiso de lectura de peso")
    fun testWhenNoWeightPermissionReturnsFailure() = runBlocking {
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWeightPermission() } returns false

        val result = useCase()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    @DisplayName("Debe retornar el registro de peso exitosamente cuando está disponible")
    fun testWhenWeightRecordExistsReturnsRecord() = runBlocking {
        val now = Instant.now()
        val expectedRecord = HealthWeightRecord(weightKg = 74.8, recordedAt = now)

        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWeightPermission() } returns true
        coEvery { healthConnectRepository.getLatestWeight() } returns Result.success(expectedRecord)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(74.8, result.getOrNull()?.weightKg)
        assertEquals(now, result.getOrNull()?.recordedAt)
    }
}
