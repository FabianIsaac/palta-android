package com.calculadoracalorias.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HouseholdPortionTest {

    @Test
    @DisplayName("Debe parsear nombres de unidades caseras en singular y plural")
    fun testParseHouseholdUnits() {
        assertEquals(HouseholdUnit.CUP, HouseholdUnit.fromDisplayName("taza"))
        assertEquals(HouseholdUnit.CUP, HouseholdUnit.fromDisplayName("tazas"))
        assertEquals(HouseholdUnit.MUG, HouseholdUnit.fromDisplayName("tazón"))
        assertEquals(HouseholdUnit.MUG, HouseholdUnit.fromDisplayName("tazones"))
        assertEquals(HouseholdUnit.TABLESPOON, HouseholdUnit.fromDisplayName("cda"))
        assertEquals(HouseholdUnit.TABLESPOON, HouseholdUnit.fromDisplayName("cucharada"))
        assertEquals(HouseholdUnit.TABLESPOON, HouseholdUnit.fromDisplayName("cucharadas"))
        assertEquals(HouseholdUnit.TEASPOON, HouseholdUnit.fromDisplayName("cdta"))
        assertEquals(HouseholdUnit.TEASPOON, HouseholdUnit.fromDisplayName("cucharadita"))
        assertEquals(HouseholdUnit.TEASPOON, HouseholdUnit.fromDisplayName("cucharaditas"))
        assertEquals(HouseholdUnit.UNIT, HouseholdUnit.fromDisplayName("unidad"))
        assertEquals(HouseholdUnit.UNIT, HouseholdUnit.fromDisplayName("unidades"))
        assertEquals(HouseholdUnit.PORTION, HouseholdUnit.fromDisplayName("porción"))
        assertEquals(HouseholdUnit.PORTION, HouseholdUnit.fromDisplayName("porciones"))
        assertEquals(HouseholdUnit.GRAMS, HouseholdUnit.fromDisplayName("g"))
        assertEquals(HouseholdUnit.MILLILITERS, HouseholdUnit.fromDisplayName("ml"))

        assertNull(HouseholdUnit.fromDisplayName("desconocido"))
    }

    @Test
    @DisplayName("Debe calcular la equivalencia en gramos/ml correctamente")
    fun testCalculateEquivalentGrams() {
        // 1 taza estándar = 200 ml/g
        val cupGrams = HouseholdPortion.calculateEquivalentGrams(HouseholdUnit.CUP, 1.0)
        assertEquals(200.0, cupGrams)

        // 1 tazón estándar = 350 ml/g
        val mugGrams = HouseholdPortion.calculateEquivalentGrams(HouseholdUnit.MUG, 1.0)
        assertEquals(350.0, mugGrams)

        // 1 cucharadita de azúcar = 5 g
        val tspGrams = HouseholdPortion.calculateEquivalentGrams(HouseholdUnit.TEASPOON, 1.0)
        assertEquals(5.0, tspGrams)

        // 2 cucharadas = 30 g
        val tbspGrams = HouseholdPortion.calculateEquivalentGrams(HouseholdUnit.TABLESPOON, 2.0)
        assertEquals(30.0, tbspGrams)

        // Sobrescribir gramos por unidad personalizada (ej. 1 marraqueta = 100g)
        val marraquetaGrams = HouseholdPortion.calculateEquivalentGrams(HouseholdUnit.UNIT, 1.5, gramPerUnit = 100.0)
        assertEquals(150.0, marraquetaGrams)
    }

    @Test
    @DisplayName("Debe generar el texto amigable de presentación en español de Chile")
    fun testToDisplayText() {
        val portionCup = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        assertEquals("1 taza (~200 ml)", portionCup.toDisplayText())

        val portionMug = HouseholdPortion(HouseholdUnit.MUG, 1.0, 350.0)
        assertEquals("1 tazón (~350 ml)", portionMug.toDisplayText())

        val portionMultipleCups = HouseholdPortion(HouseholdUnit.CUP, 2.0, 400.0)
        assertEquals("2 tazas (~400 ml)", portionMultipleCups.toDisplayText())

        val portionSugar = HouseholdPortion(HouseholdUnit.TEASPOON, 1.0, 5.0)
        assertEquals("1 cdta (~5 g)", portionSugar.toDisplayText())

        val portionSugarPlural = HouseholdPortion(HouseholdUnit.TEASPOON, 2.0, 10.0)
        assertEquals("2 cdtas (~10 g)", portionSugarPlural.toDisplayText())

        val portionGrams = HouseholdPortion(HouseholdUnit.GRAMS, 150.0, 150.0)
        assertEquals("150 g", portionGrams.toDisplayText())
    }
}
