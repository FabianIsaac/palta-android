package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteMealWithHealthSyncUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val healthConnectRepository: HealthConnectRepository = mockk()
    private lateinit var useCase: DeleteMealWithHealthSyncUseCase

    @BeforeEach
    fun setUp() {
        useCase = DeleteMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
    }

    @Test
    @DisplayName("Debe eliminar de Health Connect y de Room cuando la comida posee un healthConnectRecordId")
    fun testDeleteMealWithHealthConnectSync() = runBlocking {
        val mealId = 20L
        val recordId = "hc_rec_to_delete"
        val existingMeal = MealEntry(
            id = mealId,
            category = MealCategory.ALMUERZO,
            timestamp = 1788775200000L,
            items = emptyList(),
            summary = NutritionSummary(),
            healthConnectRecordId = recordId
        )

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(existingMeal)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery { healthConnectRepository.deleteNutritionRecord(recordId) } returns Result.success(Unit)
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.success(Unit)

        val result = useCase(mealId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            healthConnectRepository.deleteNutritionRecord(recordId)
            mealRepository.deleteMeal(mealId)
        }
    }

    @Test
    @DisplayName("Debe eliminar solo de Room si la comida no tenía healthConnectRecordId")
    fun testDeleteMealWithoutHealthConnectRecord() = runBlocking {
        val mealId = 25L
        val existingMeal = MealEntry(
            id = mealId,
            category = MealCategory.COLACIONES,
            timestamp = 1788775200000L,
            items = emptyList(),
            summary = NutritionSummary(),
            healthConnectRecordId = null
        )

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(existingMeal)
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.success(Unit)

        val result = useCase(mealId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) {
            healthConnectRepository.deleteNutritionRecord(any())
        }
        coVerify(exactly = 1) {
            mealRepository.deleteMeal(mealId)
        }
    }

    @Test
    @DisplayName("Debe continuar con la eliminación en Room incluso si Health Connect falla")
    fun testDeleteMealWhenHealthConnectFails() = runBlocking {
        val mealId = 30L
        val recordId = "hc_rec_error"
        val existingMeal = MealEntry(
            id = mealId,
            category = MealCategory.DESAYUNO,
            timestamp = 1788775200000L,
            items = emptyList(),
            summary = NutritionSummary(),
            healthConnectRecordId = recordId
        )

        coEvery { mealRepository.getMealById(mealId) } returns Result.success(existingMeal)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery { healthConnectRepository.deleteNutritionRecord(recordId) } throws RuntimeException("Connection error")
        coEvery { mealRepository.deleteMeal(mealId) } returns Result.success(Unit)

        val result = useCase(mealId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mealRepository.deleteMeal(mealId)
        }
    }
}
