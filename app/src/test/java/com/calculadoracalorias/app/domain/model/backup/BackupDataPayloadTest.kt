package com.calculadoracalorias.app.domain.model.backup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BackupDataPayloadTest {

    @Test
    @DisplayName("Debe serializar y deserializar correctamente un payload completo de respaldo")
    fun testSerializationRoundTrip() {
        val payload = BackupDataPayload(
            version = 1,
            exportedAt = 1788775200000L,
            appVersionName = "1.1",
            meals = listOf(
                BackupMealEntry(
                    id = 1L,
                    category = "ALMUERZO",
                    timestamp = 1788775200000L,
                    totalCalories = 550.0,
                    totalProteinGrams = 35.0,
                    totalCarbsGrams = 60.0,
                    totalFatGrams = 15.0,
                    notes = "Almuerzo casero",
                    rawDescription = "Pollo con arroz",
                    isPendingAiRefinement = false,
                    items = listOf(
                        BackupMealItem(
                            id = 10L,
                            name = "Pechuga de pollo",
                            portionGrams = 150.0,
                            calories = 247.5,
                            proteinGrams = 46.5,
                            carbsGrams = 0.0,
                            fatGrams = 5.4,
                            householdMeasure = "1 filete",
                            caloriesPer100g = 165.0,
                            proteinPer100g = 31.0,
                            carbsPer100g = 0.0,
                            fatPer100g = 3.6
                        )
                    )
                )
            ),
            supplements = listOf(
                BackupSupplement(
                    id = "creatina_monohidrato",
                    name = "Creatina Monohidrato",
                    dosageDescription = "5g",
                    calories = 0.0,
                    proteinGrams = 0.0,
                    carbsGrams = 0.0,
                    fatGrams = 0.0,
                    isActive = true,
                    isCustom = false,
                    createdAt = 1788770000000L
                )
            ),
            supplementLogs = listOf(
                BackupSupplementLog(
                    date = "2026-09-08",
                    supplementId = "creatina_monohidrato",
                    takenTimestamp = 1788771000000L
                )
            ),
            preferences = BackupPreferences(
                targetCalories = 2200.0,
                targetProteinGrams = 160.0,
                targetCarbsGrams = 220.0,
                targetFatGrams = 70.0,
                mealTimeWindows = BackupMealTimeWindows(
                    breakfastStartMinute = 420,
                    breakfastEndMinute = 660,
                    lunchStartMinute = 720,
                    lunchEndMinute = 960,
                    dinnerStartMinute = 1140,
                    dinnerEndMinute = 1380
                )
            )
        )

        val json = BackupDataPayload.toJson(payload)
        assertNotNull(json)

        val parsed = BackupDataPayload.fromJson(json)
        assertEquals(payload.version, parsed.version)
        assertEquals(payload.exportedAt, parsed.exportedAt)
        assertEquals(payload.appVersionName, parsed.appVersionName)
        assertEquals(1, parsed.meals.size)
        assertEquals("ALMUERZO", parsed.meals[0].category)
        assertEquals(1, parsed.meals[0].items.size)
        assertEquals("Pechuga de pollo", parsed.meals[0].items[0].name)
        assertEquals(1, parsed.supplements.size)
        assertEquals("creatina_monohidrato", parsed.supplements[0].id)
        assertEquals(1, parsed.supplementLogs.size)
        assertEquals("2026-09-08", parsed.supplementLogs[0].date)
        assertEquals(2200.0, parsed.preferences.targetCalories)
        assertEquals(420, parsed.preferences.mealTimeWindows.breakfastStartMinute)
    }

    @Test
    @DisplayName("Debe lanzar excepción si el formato JSON es corrupto o inválido")
    fun testCorruptedJsonThrowsException() {
        val corruptedJson = "{ \"version\": 1, \"exportedAt\": \"invalid_timestamp\" }"
        assertThrows(Exception::class.java) {
            BackupDataPayload.fromJson(corruptedJson)
        }
    }
}
