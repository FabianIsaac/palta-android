package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateMealWithHealthSyncUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val healthConnectRepository: HealthConnectRepository = mockk()
    private lateinit var useCase: UpdateMealWithHealthSyncUseCase

    private val sampleItem = ScannedFoodItem(
        id = "palta-1",
        name = "Palta hass",
        servingGrams = 50.0,
        caloriesPer100g = 160.0,
        proteinPer100g = 2.0,
        carbsPer100g = 9.0,
        fatPer100g = 15.0
    )

    @BeforeEach
    fun setUp() {
        useCase = UpdateMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
    }

    @Test
    @DisplayName("Debe actualizar en Room y actualizar en Health Connect cuando existe healthConnectRecordId")
    fun testUpdateMealWithHealthConnectExistingRecord() = runBlocking {
        val mealId = 10L
        val timestamp = 1788775200000L
        val existingRecordId = "hc_rec_123"

        val existingMeal = MealEntry(
            id = mealId,
            category = MealCategory.DESAYUNO,
            timestamp = timestamp,
            items = listOf(sampleItem),
            summary = NutritionSummary.fromItems(listOf(sampleItem)),
            healthConnectRecordId = existingRecordId
        )

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(existingMeal)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery {
            healthConnectRepository.updateNutritionRecord(
                recordId = existingRecordId,
                mealName = "Desayuno",
                mealCategory = MealCategory.DESAYUNO,
                timestamp = timestamp,
                calories = any(),
                proteinGrams = any(),
                carbsGrams = any(),
                fatGrams = any()
            )
        } returns Result.success(Unit)

        coEvery {
            mealRepository.updateMeal(
                mealId = mealId,
                category = MealCategory.DESAYUNO,
                timestamp = timestamp,
                items = listOf(sampleItem),
                healthConnectRecordId = existingRecordId
            )
        } returns Result.success(Unit)

        val result = useCase(
            mealId = mealId,
            category = MealCategory.DESAYUNO,
            timestamp = timestamp,
            items = listOf(sampleItem)
        )

        assertTrue(result.isSuccess)
        val updateResult = result.getOrThrow()
        assertEquals(mealId, updateResult.mealId)
        assertEquals(existingRecordId, updateResult.healthConnectRecordId)
        assertTrue(updateResult.syncedWithHealthConnect)

        coVerify(exactly = 1) {
            healthConnectRepository.updateNutritionRecord(existingRecordId, any(), any(), any(), any(), any(), any(), any())
            mealRepository.updateMeal(mealId, MealCategory.DESAYUNO, timestamp, listOf(sampleItem), existingRecordId)
        }
    }

    @Test
    @DisplayName("Debe crear registro en Health Connect si la comida no tenía pero ahora hay permisos")
    fun testUpdateMealCreatingHealthConnectRecordIfNoneExisted() = runBlocking {
        val mealId = 15L
        val timestamp = 1788775200000L
        val newRecordId = "hc_new_456"

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(null)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery {
            healthConnectRepository.writeNutritionRecord(
                mealName = "Once / Cena",
                mealCategory = MealCategory.ONCE_CENA,
                timestamp = timestamp,
                calories = any(),
                proteinGrams = any(),
                carbsGrams = any(),
                fatGrams = any()
            )
        } returns Result.success(newRecordId)

        coEvery {
            mealRepository.updateMeal(
                mealId = mealId,
                category = MealCategory.ONCE_CENA,
                timestamp = timestamp,
                items = listOf(sampleItem),
                healthConnectRecordId = newRecordId
            )
        } returns Result.success(Unit)

        val result = useCase(
            mealId = mealId,
            category = MealCategory.ONCE_CENA,
            timestamp = timestamp,
            items = listOf(sampleItem)
        )

        assertTrue(result.isSuccess)
        val updateResult = result.getOrThrow()
        assertEquals(newRecordId, updateResult.healthConnectRecordId)
        assertTrue(updateResult.syncedWithHealthConnect)
    }

    @Test
    @DisplayName("Debe degradar de forma agraciada y actualizar en Room si Health Connect falla")
    fun testUpdateMealWhenHealthConnectFails() = runBlocking {
        val mealId = 10L
        val timestamp = 1788775200000L
        val recordId = "hc_rec_999"

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(null)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery {
            healthConnectRepository.updateNutritionRecord(any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Health Connect disconnected")

        coEvery {
            mealRepository.updateMeal(
                mealId = mealId,
                category = MealCategory.DESAYUNO,
                timestamp = timestamp,
                items = listOf(sampleItem),
                healthConnectRecordId = recordId
            )
        } returns Result.success(Unit)

        val result = useCase(
            mealId = mealId,
            category = MealCategory.DESAYUNO,
            timestamp = timestamp,
            items = listOf(sampleItem),
            existingHealthConnectRecordId = recordId
        )

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().syncedWithHealthConnect)
    }

    @Test
    @DisplayName("Debe fallar si la lista de alimentos está vacía")
    fun testUpdateMealEmptyItemsFails() = runBlocking {
        val result = useCase(
            mealId = 1L,
            category = MealCategory.DESAYUNO,
            timestamp = 1000L,
            items = emptyList()
        )
        assertTrue(result.isFailure)
        assertEquals("No se pueden guardar comidas sin alimentos.", result.exceptionOrNull()?.message)
    }

    @Test
    @DisplayName("Debe fallar si alguna porción es menor o igual a cero gramos")
    fun testUpdateMealInvalidPortionFails() = runBlocking {
        val invalidItem = sampleItem.copy(servingGrams = 0.0)
        val result = useCase(
            mealId = 1L,
            category = MealCategory.DESAYUNO,
            timestamp = 1000L,
            items = listOf(invalidItem)
        )
        assertTrue(result.isFailure)
        assertEquals("Las porciones de los alimentos deben ser mayores a 0 gramos.", result.exceptionOrNull()?.message)
    }
}
