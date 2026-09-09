package com.calculadoracalorias.app.data.local

import com.calculadoracalorias.app.domain.model.HouseholdUnit
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LocalFoodCatalogRepositoryTest {

    private lateinit var repository: LocalFoodCatalogRepository

    @BeforeEach
    fun setUp() {
        repository = LocalFoodCatalogRepository()
    }

    @Test
    @DisplayName("Debe encontrar café negro y café con leche al buscar café")
    fun testSearchCoffee() = runTest {
        val results = repository.searchFood("café")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Café negro" })
        assertTrue(results.any { it.name == "Café con leche" })

        val cafeNegro = results.first { it.name == "Café negro" }
        assertEquals(2.0, cafeNegro.caloriesPer100g)
        assertEquals(200.0, cafeNegro.servingGrams)
        assertNotNull(cafeNegro.householdPortion)
        assertEquals(HouseholdUnit.CUP, cafeNegro.householdPortion?.unit)
    }

    @Test
    @DisplayName("Debe encontrar té y azúcar en el catálogo")
    fun testSearchTeaAndSugar() = runTest {
        val teaResults = repository.searchFood("té")
        assertTrue(teaResults.any { it.name == "Té" })

        val sugarResults = repository.searchFood("azúcar")
        assertTrue(sugarResults.any { it.name == "Azúcar blanca" })
        val sugar = sugarResults.first { it.name == "Azúcar blanca" }
        assertEquals(5.0, sugar.servingGrams)
        assertEquals(HouseholdUnit.TEASPOON, sugar.householdPortion?.unit)
        assertEquals(1.0, sugar.householdPortion?.quantity)
    }

    @Test
    @DisplayName("Debe encontrar mejor coincidencia para alimentos cotidianos chilenos")
    fun testFindBestMatchChileanFoods() = runTest {
        val marraqueta = repository.findBestMatch("marraqueta")
        assertNotNull(marraqueta)
        assertEquals("Marraqueta", marraqueta?.name)
        assertEquals(HouseholdUnit.UNIT, marraqueta?.householdPortion?.unit)

        val palta = repository.findBestMatch("palta")
        assertNotNull(palta)
        assertEquals("Palta hass", palta?.name)

        val cafe = repository.findBestMatch("café solo")
        assertNotNull(cafe)
        assertEquals("Café negro", cafe?.name)
    }
}
