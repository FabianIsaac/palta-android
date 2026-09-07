package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SaveMealWithHealthSyncUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val healthConnectRepository: HealthConnectRepository = mockk()
    private lateinit var useCase: SaveMealWithHealthSyncUseCase

    private val sampleItems = listOf(
        ScannedFoodItem(
            id = "chicken-1",
            name = "Pechuga de pollo a la plancha",
            servingGrams = 150.0,
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6
        ),
        ScannedFoodItem(
            id = "avocado-1",
            name = "Palta hass",
            servingGrams = 100.0,
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 9.0,
            fatPer100g = 15.0
        )
    )

    @BeforeEach
    fun setUp() {
        useCase = SaveMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
    }

    @Test
    @DisplayName("Debe persistir en Room y sincronizar con Health Connect cuando los permisos están concedidos")
    fun testSaveMealWithHealthConnectSyncSuccess() = runBlocking {
        val timestamp = 1700000000000L
        val healthRecordId = "hc-record-uuid-123"

        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery {
            healthConnectRepository.writeNutritionRecord(
                mealName = "Almuerzo",
                mealCategory = MealCategory.ALMUERZO,
                timestamp = timestamp,
                calories = any(),
                proteinGrams = any(),
                carbsGrams = any(),
                fatGrams = any()
            )
        } returns Result.success(healthRecordId)

        coEvery {
            mealRepository.saveMeal(
                category = MealCategory.ALMUERZO,
                timestamp = timestamp,
                items = sampleItems,
                healthConnectRecordId = healthRecordId
            )
        } returns Result.success(42L)

        val result = useCase(
            mealName = "Almuerzo",
            category = MealCategory.ALMUERZO,
            timestamp = timestamp,
            items = sampleItems
        )

        assertTrue(result.isSuccess)
        val saveResult = result.getOrThrow()
        assertEquals(42L, saveResult.mealId)
        assertEquals(healthRecordId, saveResult.healthConnectRecordId)
        assertTrue(saveResult.syncedWithHealthConnect)

        coVerify(exactly = 1) {
            healthConnectRepository.writeNutritionRecord(any(), any(), any(), any(), any(), any(), any())
            mealRepository.saveMeal(MealCategory.ALMUERZO, timestamp, sampleItems, healthRecordId)
        }
    }

    @Test
    @DisplayName("Debe degradar de forma agraciada y guardar en Room si Health Connect no está disponible")
    fun testSaveMealWhenHealthConnectUnavailable() = runBlocking {
        val timestamp = 1700000000000L

        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns false
        coEvery {
            mealRepository.saveMeal(
                category = MealCategory.ONCE_CENA,
                timestamp = timestamp,
                items = sampleItems,
                healthConnectRecordId = null
            )
        } returns Result.success(101L)

        val result = useCase(
            mealName = "Once / Cena",
            category = MealCategory.ONCE_CENA,
            timestamp = timestamp,
            items = sampleItems
        )

        assertTrue(result.isSuccess)
        val saveResult = result.getOrThrow()
        assertEquals(101L, saveResult.mealId)
        assertNull(saveResult.healthConnectRecordId)
        assertFalse(saveResult.syncedWithHealthConnect)

        coVerify(exactly = 0) {
            healthConnectRepository.writeNutritionRecord(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    @DisplayName("Debe degradar de forma agraciada si Health Connect lanza una excepción inesperada")
    fun testSaveMealWhenHealthConnectThrowsException() = runBlocking {
        val timestamp = 1700000000000L

        coEvery { healthConnectRepository.isHealthConnectAvailable() } throws RuntimeException("SDK DeadObjectException")
        coEvery {
            mealRepository.saveMeal(
                category = MealCategory.DESAYUNO,
                timestamp = timestamp,
                items = sampleItems,
                healthConnectRecordId = null
            )
        } returns Result.success(202L)

        val result = useCase(
            mealName = "Desayuno",
            category = MealCategory.DESAYUNO,
            timestamp = timestamp,
            items = sampleItems
        )

        assertTrue(result.isSuccess)
        assertEquals(202L, result.getOrThrow().mealId)
        assertFalse(result.getOrThrow().syncedWithHealthConnect)
    }

    @Test
    @DisplayName("Debe retornar fallo si la lista de alimentos está vacía")
    fun testSaveMealEmptyItemsFails() = runBlocking {
        val result = useCase(
            mealName = "Almuerzo vacío",
            category = MealCategory.ALMUERZO,
            timestamp = System.currentTimeMillis(),
            items = emptyList()
        )

        assertTrue(result.isFailure)
        assertEquals("No se pueden registrar comidas sin alimentos.", result.exceptionOrNull()?.message)
    }
}
