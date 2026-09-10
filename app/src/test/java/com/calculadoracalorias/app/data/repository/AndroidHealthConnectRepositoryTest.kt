package com.calculadoracalorias.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.calculadoracalorias.app.domain.model.MealCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
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
    @DisplayName("Debe actualizar NutritionRecord en Health Connect")
    fun testUpdateNutritionRecordSuccess() = runBlocking {
        coEvery {
            healthConnectClient.updateRecords(any())
        } returns Unit

        val result = repository.updateNutritionRecord(
            recordId = "hc-update-uuid-101",
            mealName = "Desayuno modificado",
            mealCategory = MealCategory.DESAYUNO,
            timestamp = 1700000000000L,
            calories = 350.0,
            proteinGrams = 12.0,
            carbsGrams = 40.0,
            fatGrams = 10.0
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            healthConnectClient.updateRecords(match { records ->
                records.size == 1 && records.first().metadata.id == "hc-update-uuid-101"
            })
        }
    }

    @Test
    @DisplayName("Debe eliminar NutritionRecord en Health Connect")
    fun testDeleteNutritionRecordSuccess() = runBlocking {
        coEvery {
            healthConnectClient.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = listOf("hc-delete-uuid-202"),
                clientRecordIdsList = emptyList()
            )
        } returns Unit

        val result = repository.deleteNutritionRecord("hc-delete-uuid-202")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            healthConnectClient.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = listOf("hc-delete-uuid-202"),
                clientRecordIdsList = emptyList()
            )
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

    @Test
    @DisplayName("Debe verificar correctamente los permisos de actividad física")
    fun testHasActivityPermissions() = runBlocking {
        val activePerm = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        val stepsPerm = HealthPermission.getReadPermission(StepsRecord::class)

        coEvery { permissionController.getGrantedPermissions() } returns setOf(activePerm, stepsPerm)
        assertTrue(repository.hasActivityPermissions())

        coEvery { permissionController.getGrantedPermissions() } returns setOf(activePerm)
        assertFalse(repository.hasActivityPermissions())
    }

    @Test
    @DisplayName("Debe verificar correctamente el permiso de lectura de peso")
    fun testHasWeightPermission() = runBlocking {
        val weightPerm = HealthPermission.getReadPermission(WeightRecord::class)

        coEvery { permissionController.getGrantedPermissions() } returns setOf(weightPerm)
        assertTrue(repository.hasWeightPermission())

        coEvery { permissionController.getGrantedPermissions() } returns emptySet()
        assertFalse(repository.hasWeightPermission())
    }

    @Test
    @DisplayName("Debe obtener la actividad diaria agregando calorías y pasos exitosamente")
    fun testGetDailyActivitySuccess() = runBlocking {
        val aggregationResult = mockk<AggregationResult>(relaxed = true)
        every { aggregationResult[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL] } returns Energy.kilocalories(420.0)
        every { aggregationResult[StepsRecord.COUNT_TOTAL] } returns 7500L
        coEvery { healthConnectClient.aggregate(any()) } returns aggregationResult

        val testDate = LocalDate.of(2026, 9, 9)
        val result = repository.getDailyActivity(testDate)

        assertTrue(result.isSuccess)
        val activity = result.getOrNull()
        assertEquals(testDate, activity?.date)
        assertEquals(420.0, activity?.burnedCalories)
        assertEquals(7500L, activity?.stepsCount)
    }

    @Test
    @DisplayName("Debe obtener el peso más reciente correctamente")
    fun testGetLatestWeightSuccess() = runBlocking {
        val now = Instant.now()
        val weightRecord = mockk<WeightRecord>(relaxed = true)
        every { weightRecord.weight } returns Mass.kilograms(73.5)
        every { weightRecord.time } returns now

        val response = mockk<ReadRecordsResponse<WeightRecord>>(relaxed = true)
        every { response.records } returns listOf(weightRecord)
        coEvery { healthConnectClient.readRecords(any<androidx.health.connect.client.request.ReadRecordsRequest<WeightRecord>>()) } returns response

        val result = repository.getLatestWeight()

        assertTrue(result.isSuccess)
        assertEquals(73.5, result.getOrNull()?.weightKg)
        assertEquals(now, result.getOrNull()?.recordedAt)
    }

    @Test
    @DisplayName("Debe retornar null cuando no existen registros de peso en Health Connect")
    fun testGetLatestWeightEmpty() = runBlocking {
        val response = mockk<ReadRecordsResponse<WeightRecord>>(relaxed = true)
        every { response.records } returns emptyList()
        coEvery { healthConnectClient.readRecords(any<androidx.health.connect.client.request.ReadRecordsRequest<WeightRecord>>()) } returns response

        val result = repository.getLatestWeight()

        assertTrue(result.isSuccess)
        org.junit.jupiter.api.Assertions.assertNull(result.getOrNull())
    }
}
