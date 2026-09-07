package com.calculadoracalorias.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.response.InsertRecordsResponse
import com.calculadoracalorias.app.domain.model.MealCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AndroidHealthConnectRepositoryTest {

    private val context: Context = mockk(relaxed = true)
    private val healthConnectClient: HealthConnectClient = mockk(relaxed = true)
    private val permissionController: PermissionController = mockk(relaxed = true)

    private lateinit var repository: AndroidHealthConnectRepository

    private var currentSdkStatus: Int = HealthConnectClient.SDK_AVAILABLE

    @BeforeEach
    fun setUp() {
        every { healthConnectClient.permissionController } returns permissionController

        repository = AndroidHealthConnectRepository(
            context = context,
            sdkStatusProvider = { currentSdkStatus },
            healthConnectClientProvider = { healthConnectClient }
        )
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    @DisplayName("Debe verificar correctamente el estado de disponibilidad del SDK de Health Connect")
    fun testIsHealthConnectAvailable() = runBlocking {
        currentSdkStatus = HealthConnectClient.SDK_AVAILABLE
        assertTrue(repository.isHealthConnectAvailable())

        currentSdkStatus = HealthConnectClient.SDK_UNAVAILABLE
        assertFalse(repository.isHealthConnectAvailable())
    }

    @Test
    @DisplayName("Debe reportar permisos concedidos cuando WRITE_NUTRITION está presente")
    fun testHasWriteNutritionPermissionGranted() = runBlocking {
        val writePerm = HealthPermission.getWritePermission(NutritionRecord::class)
        coEvery { permissionController.getGrantedPermissions() } returns setOf(writePerm)

        val hasPerm = repository.hasWriteNutritionPermission()

        assertTrue(hasPerm)
    }

    @Test
    @DisplayName("Debe reportar permisos denegados cuando WRITE_NUTRITION no está presente")
    fun testHasWriteNutritionPermissionDenied() = runBlocking {
        coEvery { permissionController.getGrantedPermissions() } returns emptySet()

        val hasPerm = repository.hasWriteNutritionPermission()

        assertFalse(hasPerm)
    }

    @Test
    @DisplayName("Debe escribir NutritionRecord en Health Connect y retornar el recordId")
    fun testWriteNutritionRecordSuccess() = runBlocking {
        val mockResponse: InsertRecordsResponse = mockk()
        every { mockResponse.recordIdsList } returns listOf("test-hc-record-uuid-789")

        coEvery {
            healthConnectClient.insertRecords(any())
        } returns mockResponse

        val result = repository.writeNutritionRecord(
            mealName = "Almuerzo casero",
            mealCategory = MealCategory.ALMUERZO,
            timestamp = 1700000000000L,
            calories = 650.0,
            proteinGrams = 45.0,
            carbsGrams = 60.0,
            fatGrams = 20.0
        )

        assertTrue(result.isSuccess)
        assertEquals("test-hc-record-uuid-789", result.getOrThrow())

        coVerify(exactly = 1) {
            healthConnectClient.insertRecords(match { records ->
                records.size == 1 && records.first() is NutritionRecord
            })
        }
    }

    @Test
    @DisplayName("Debe retornar fallo cuando el cliente de Health Connect no está disponible")
    fun testWriteNutritionRecordWhenClientNull() = runBlocking {
        val repoWithoutClient = AndroidHealthConnectRepository(
            context = context,
            healthConnectClientProvider = { null }
        )

        val result = repoWithoutClient.writeNutritionRecord(
            mealName = "Desayuno",
            mealCategory = MealCategory.DESAYUNO,
            timestamp = 1700000000000L,
            calories = 300.0,
            proteinGrams = 15.0,
            carbsGrams = 30.0,
            fatGrams = 10.0
        )

        assertTrue(result.isFailure)
        assertEquals("Health Connect no está disponible en este dispositivo.", result.exceptionOrNull()?.message)
    }
}
