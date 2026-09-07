package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RecalculatePortionUseCaseTest {

    private lateinit var useCase: RecalculatePortionUseCase

    @BeforeEach
    fun setUp() {
        useCase = RecalculatePortionUseCase()
    }

    @Test
    @DisplayName("Debe recalcular proporcionalmente calorías y macronutrientes al modificar la porción")
    fun testRecalculateSingleItemProportional() {
        // Escenario del spec: Arroz blanco cocido de 100g pasa a 150g
        val item = ScannedFoodItem(
            id = "rice-1",
            name = "Arroz blanco cocido",
            servingGrams = 100.0,
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatPer100g = 0.3
        )

        val updated = useCase(item, 150.0)

        assertEquals(150.0, updated.servingGrams)
        assertEquals(195.0, updated.totalCalories, 0.01)
        assertEquals(4.05, updated.totalProtein, 0.01)
        assertEquals(42.3, updated.totalCarbs, 0.01)
        assertEquals(0.45, updated.totalFat, 0.01)
    }

    @Test
    @DisplayName("Debe sanitizar porciones negativas a 0 gramos")
    fun testNegativePortionSanitization() {
        val item = ScannedFoodItem(
            id = "item-1",
            name = "Palta hass",
            servingGrams = 100.0,
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 9.0,
            fatPer100g = 15.0
        )

        val updated = useCase(item, -50.0)

        assertEquals(0.0, updated.servingGrams)
        assertEquals(0.0, updated.totalCalories)
        assertEquals(0.0, updated.totalProtein)
        assertEquals(0.0, updated.totalCarbs)
        assertEquals(0.0, updated.totalFat)
    }

    @Test
    @DisplayName("Debe actualizar correctamente el ítem correspondiente en una lista de alimentos")
    fun testRecalculateInList() {
        val item1 = ScannedFoodItem(
            id = "item-1",
            name = "Pechuga de pollo",
            servingGrams = 100.0,
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6
        )
        val item2 = ScannedFoodItem(
            id = "item-2",
            name = "Marraqueta",
            servingGrams = 100.0,
            caloriesPer100g = 280.0,
            proteinPer100g = 9.0,
            carbsPer100g = 56.0,
            fatPer100g = 1.0
        )

        val list = listOf(item1, item2)
        val updatedList = useCase(list, "item-1", 150.0)

        val updatedItem1 = updatedList.first { it.id == "item-1" }
        val unmodifiedItem2 = updatedList.first { it.id == "item-2" }

        assertEquals(150.0, updatedItem1.servingGrams)
        assertEquals(247.5, updatedItem1.totalCalories, 0.01)
        assertEquals(46.5, updatedItem1.totalProtein, 0.01)

        assertEquals(100.0, unmodifiedItem2.servingGrams)
        assertEquals(280.0, unmodifiedItem2.totalCalories, 0.01)
    }
}
