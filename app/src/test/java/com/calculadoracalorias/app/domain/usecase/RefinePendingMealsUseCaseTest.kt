package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.HealthConnectRepository
import com.calculadoracalorias.app.domain.repository.MealRepository
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer
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

class RefinePendingMealsUseCaseTest {

    private val mealRepository: MealRepository = mockk()
    private val analyzer: NaturalLanguageMealAnalyzer = mockk()
    private val healthConnectRepository: HealthConnectRepository = mockk(relaxed = true)
    private lateinit var useCase: RefinePendingMealsUseCase

    private val sampleItem1 = ScannedFoodItem(
        id = "fajita-1",
        name = "Tortillas de fajita",
        servingGrams = 80.0,
        caloriesPer100g = 300.0,
        proteinPer100g = 8.0,
        carbsPer100g = 52.0,
        fatPer100g = 6.0
    )

    private val sampleItem2 = ScannedFoodItem(
        id = "carne-1",
        name = "Carne molida cocida",
        servingGrams = 100.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 26.0,
        carbsPer100g = 0.0,
        fatPer100g = 15.0
    )

    @BeforeEach
    fun setUp() {
        useCase = RefinePendingMealsUseCase(mealRepository, analyzer, healthConnectRepository)
    }

    @Test
    @DisplayName("Debe reprocesar exitosamente comidas pendientes y marcarlas sin refinamiento pendiente")
    fun testSuccessfulRefinement() = runBlocking {
        val pendingMeal = MealEntry(
            id = 1L,
            category = MealCategory.ONCE_CENA,
            timestamp = 1000L,
            items = listOf(sampleItem1),
            summary = NutritionSummary.fromItems(listOf(sampleItem1)),
            healthConnectRecordId = "hc_1",
            rawDescription = "2 fajitas con carne molida",
            isPendingAiRefinement = true
        )

        val refinedItems = listOf(sampleItem1, sampleItem2)
        val detectedResult = DetectedMealResult(
            items = refinedItems,
            suggestedMealType = MealCategory.ONCE_CENA,
            analysisSource = VisionSource.MINIMAX_CLOUD
        )

        coEvery { mealRepository.getPendingRefinementMeals() } returns Result.success(listOf(pendingMeal))
        coEvery { analyzer.analyzeTextDescription("2 fajitas con carne molida") } returns Result.success(detectedResult)
        coEvery { healthConnectRepository.isHealthConnectAvailable() } returns true
        coEvery { healthConnectRepository.hasWriteNutritionPermission() } returns true
        coEvery { mealRepository.updateMeal(any()) } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        val refinedList = result.getOrThrow()
        assertEquals(1, refinedList.size)
        val updated = refinedList.first()
        assertEquals(2, updated.items.size)
        assertFalse(updated.isPendingAiRefinement)
        assertEquals("2 fajitas con carne molida", updated.rawDescription)

        coVerify(exactly = 1) {
            analyzer.analyzeTextDescription("2 fajitas con carne molida")
            mealRepository.updateMeal(match {
                it.id == 1L && !it.isPendingAiRefinement && it.items.size == 2
            })
            healthConnectRepository.updateNutritionRecord("hc_1", any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    @DisplayName("Debe manejar fallos parciales sin abortar el resto de comidas pendientes")
    fun testPartialFailureHandling() = runBlocking {
        val meal1 = MealEntry(
            id = 1L,
            category = MealCategory.ALMUERZO,
            timestamp = 1000L,
            items = listOf(sampleItem1),
            summary = NutritionSummary.fromItems(listOf(sampleItem1)),
            rawDescription = "comida que falla",
            isPendingAiRefinement = true
        )
        val meal2 = MealEntry(
            id = 2L,
            category = MealCategory.DESAYUNO,
            timestamp = 2000L,
            items = listOf(sampleItem1),
            summary = NutritionSummary.fromItems(listOf(sampleItem1)),
            rawDescription = "café con leche",
            isPendingAiRefinement = true
        )

        coEvery { mealRepository.getPendingRefinementMeals() } returns Result.success(listOf(meal1, meal2))
        coEvery { analyzer.analyzeTextDescription("comida que falla") } returns Result.failure(RuntimeException("Network error"))
        coEvery { analyzer.analyzeTextDescription("café con leche") } returns Result.success(
            DetectedMealResult(
                items = listOf(sampleItem2),
                suggestedMealType = MealCategory.DESAYUNO,
                analysisSource = VisionSource.MINIMAX_CLOUD
            )
        )
        coEvery { mealRepository.updateMeal(any()) } returns Result.success(Unit)

        val result = useCase()

        assertTrue(result.isSuccess)
        val refinedList = result.getOrThrow()
        assertEquals(1, refinedList.size)
        assertEquals(2L, refinedList.first().id)

        coVerify(exactly = 1) {
            mealRepository.updateMeal(match { it.id == 2L && !it.isPendingAiRefinement })
        }
    }

    @Test
    @DisplayName("Debe fallar si getPendingRefinementMeals retorna fallo")
    fun testRepositoryFailure() = runBlocking {
        val error = RuntimeException("Database error")
        coEvery { mealRepository.getPendingRefinementMeals() } returns Result.failure(error)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
