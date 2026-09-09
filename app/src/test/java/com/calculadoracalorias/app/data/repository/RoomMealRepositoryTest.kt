package com.calculadoracalorias.app.data.repository

import com.calculadoracalorias.app.data.local.dao.MealDao
import com.calculadoracalorias.app.data.local.entity.MealEntryEntity
import com.calculadoracalorias.app.data.local.entity.MealFoodItemEntity
import com.calculadoracalorias.app.data.local.relation.MealWithItems
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RoomMealRepositoryTest {

    private val mealDao: MealDao = mockk()
    private lateinit var repository: RoomMealRepository

    private val sampleFoodItem = ScannedFoodItem(
        id = "1",
        name = "Marraqueta con palta",
        servingGrams = 100.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 8.0,
        carbsPer100g = 45.0,
        fatPer100g = 5.0
    )

    @BeforeEach
    fun setUp() {
        repository = RoomMealRepository(mealDao)
    }

    @Test
    @DisplayName("Debe consultar comidas por rango de tiempo y mapear a entidades de dominio")
    fun testGetMealsForDay() = runBlocking {
        val mealEntity = MealEntryEntity(
            id = 1L,
            category = "DESAYUNO",
            timestamp = 1788775200000L,
            healthConnectRecordId = "hc_123",
            totalCalories = 250.0,
            totalProtein = 8.0,
            totalCarbs = 45.0,
            totalFat = 5.0
        )
        val itemEntity = MealFoodItemEntity(
            id = 10L,
            mealEntryId = 1L,
            name = "Marraqueta con palta",
            servingGrams = 100.0,
            caloriesPer100g = 250.0,
            proteinPer100g = 8.0,
            carbsPer100g = 45.0,
            fatPer100g = 5.0
        )
        val relation = MealWithItems(meal = mealEntity, items = listOf(itemEntity))

        every { mealDao.getMealsWithItemsBetween(any(), any()) } returns flowOf(listOf(relation))

        val result = repository.getMealsForDay(100L, 200L).first()

        assertEquals(1, result.size)
        val domainMeal = result.first()
        assertEquals(1L, domainMeal.id)
        assertEquals(MealCategory.DESAYUNO, domainMeal.category)
        assertEquals("hc_123", domainMeal.healthConnectRecordId)
        assertEquals(1, domainMeal.items.size)
        assertEquals("Marraqueta con palta", domainMeal.items.first().name)
    }

    @Test
    @DisplayName("Debe consultar comida por id y mapear a dominio")
    fun testGetMealById() = runBlocking {
        val mealEntity = MealEntryEntity(
            id = 5L,
            category = "ALMUERZO",
            timestamp = 1788775200000L,
            healthConnectRecordId = null,
            totalCalories = 500.0,
            totalProtein = 20.0,
            totalCarbs = 60.0,
            totalFat = 10.0
        )
        val relation = MealWithItems(meal = mealEntity, items = emptyList())

        coEvery { mealDao.getMealWithItemsById(5L) } returns relation

        val result = repository.getMealById(5L)

        assertTrue(result.isSuccess)
        val meal = result.getOrThrow()
        assertNotNull(meal)
        assertEquals(5L, meal?.id)
        assertEquals(MealCategory.ALMUERZO, meal?.category)
    }

    @Test
    @DisplayName("Debe actualizar comida en MealDao con transacción atómica")
    fun testUpdateMeal() = runBlocking {
        coEvery { mealDao.updateMealWithItems(any(), any()) } returns Unit

        val result = repository.updateMeal(
            mealId = 2L,
            category = MealCategory.ONCE_CENA,
            timestamp = 1788775200000L,
            items = listOf(sampleFoodItem),
            healthConnectRecordId = "hc_updated"
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mealDao.updateMealWithItems(
                match { it.id == 2L && it.category == "ONCE_CENA" && it.healthConnectRecordId == "hc_updated" },
                match { it.size == 1 && it.first().mealEntryId == 2L }
            )
        }
    }

    @Test
    @DisplayName("Debe eliminar comida en MealDao")
    fun testDeleteMeal() = runBlocking {
        coEvery { mealDao.deleteMeal(7L) } returns Unit

        val result = repository.deleteMeal(7L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) {
            mealDao.deleteMeal(7L)
        }
    }

    @Test
    @DisplayName("Debe consultar comidas pendientes de refinamiento")
    fun testGetPendingRefinementMeals() = runBlocking {
        val pendingEntity = MealEntryEntity(
            id = 8L,
            category = "ONCE_CENA",
            timestamp = 1788775200000L,
            healthConnectRecordId = null,
            totalCalories = 250.0,
            totalProtein = 8.0,
            totalCarbs = 45.0,
            totalFat = 5.0,
            rawDescription = "2 fajitas con pollo",
            isPendingAiRefinement = true
        )
        val relation = MealWithItems(meal = pendingEntity, items = emptyList())

        coEvery { mealDao.getPendingRefinementMealsWithItems() } returns listOf(relation)

        val result = repository.getPendingRefinementMeals()

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        val pending = list.first()
        assertEquals(8L, pending.id)
        assertEquals("2 fajitas con pollo", pending.rawDescription)
        assertTrue(pending.isPendingAiRefinement)
    }
}
