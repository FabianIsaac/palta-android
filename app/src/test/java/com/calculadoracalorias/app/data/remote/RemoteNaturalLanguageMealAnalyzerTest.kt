package com.calculadoracalorias.app.data.remote

import com.calculadoracalorias.app.data.local.LocalFoodCatalogRepository
import com.calculadoracalorias.app.domain.model.HouseholdUnit
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.VisionSource
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RemoteNaturalLanguageMealAnalyzerTest {

    private val httpClient: HttpClient = mockk(relaxed = true)
    private val catalogRepository = LocalFoodCatalogRepository()
    private lateinit var analyzer: RemoteNaturalLanguageMealAnalyzer

    @BeforeEach
    fun setUp() {
        analyzer = RemoteNaturalLanguageMealAnalyzer(
            apiKeyProvider = { "test-api-key" },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )
    }

    @Test
    @DisplayName("Debe deserializar JSON estructurado con porciones caseras correctamente")
    fun testParseMealDtoWithHouseholdUnits() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Desayuno",
              "detected_items": [
                {
                  "name": "Café negro",
                  "serving_grams": 200.0,
                  "calories_per_100g": 2.0,
                  "protein_per_100g": 0.1,
                  "carbs_per_100g": 0.2,
                  "fat_per_100g": 0.0,
                  "confidence": 0.98,
                  "household_unit": "taza",
                  "household_quantity": 1.0
                },
                {
                  "name": "Azúcar blanca",
                  "serving_grams": 5.0,
                  "calories_per_100g": 387.0,
                  "protein_per_100g": 0.0,
                  "carbs_per_100g": 100.0,
                  "fat_per_100g": 0.0,
                  "confidence": 0.95,
                  "household_unit": "cdta",
                  "household_quantity": 1.0
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)

        assertEquals("Desayuno", dto.suggestedMealCategory)
        assertEquals(2, dto.detectedItems.size)
        assertEquals("Café negro", dto.detectedItems[0].name)
        assertEquals(200.0, dto.detectedItems[0].servingGrams)
        assertEquals("taza", dto.detectedItems[0].householdUnit)
        assertEquals(1.0, dto.detectedItems[0].householdQuantity)
    }

    @Test
    @DisplayName("Debe parsear JSON envuelto en bloques markdown")
    fun testParseMealDtoWithMarkdownFence() {
        val markdownPayload = """
            ```json
            {
              "suggested_meal_category": "Once / Cena",
              "detected_items": [
                {
                  "name": "Marraqueta con palta",
                  "serving_grams": 150.0,
                  "calories_per_100g": 220.0,
                  "protein_per_100g": 6.0,
                  "carbs_per_100g": 32.0,
                  "fat_per_100g": 8.0,
                  "confidence": 0.95,
                  "household_unit": "unidad",
                  "household_quantity": 1.0
                }
              ]
            }
            ```
        """.trimIndent()

        val dto = analyzer.parseMealDto(markdownPayload)

        assertEquals("Once / Cena", dto.suggestedMealCategory)
        assertEquals(1, dto.detectedItems.size)
        assertEquals("Marraqueta con palta", dto.detectedItems[0].name)
    }

    @Test
    @DisplayName("Debe mapear DTO a DetectedMealResult con objetos HouseholdPortion")
    fun testMapDtoToResult() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Desayuno",
              "detected_items": [
                {
                  "name": "Café cortado",
                  "serving_grams": 150.0,
                  "calories_per_100g": 25.0,
                  "protein_per_100g": 1.8,
                  "carbs_per_100g": 2.5,
                  "fat_per_100g": 1.0,
                  "confidence": 0.95,
                  "household_unit": "taza",
                  "household_quantity": 1.0
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)
        val result = analyzer.mapDtoToResult(dto)

        assertEquals(MealCategory.DESAYUNO, result.suggestedMealType)
        assertEquals(VisionSource.MINIMAX_CLOUD, result.analysisSource)
        assertEquals(1, result.items.size)

        val item = result.items[0]
        assertEquals("Café cortado", item.name)
        assertNotNull(item.householdPortion)
        assertEquals(HouseholdUnit.CUP, item.householdPortion?.unit)
        assertEquals(1.0, item.householdPortion?.quantity)
        assertEquals(150.0, item.householdPortion?.equivalentGrams)
    }

    @Test
    @DisplayName("Debe operar con fallback inteligente offline cuando no hay API Key")
    fun testOfflineFallbackWithoutApiKey() = runTest {
        val offlineAnalyzer = RemoteNaturalLanguageMealAnalyzer(
            apiKeyProvider = { "" },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )

        val result = offlineAnalyzer.analyzeTextDescription("Un café con leche y una marraqueta con palta")

        assertTrue(result.isSuccess)
        val meal = result.getOrNull()
        assertNotNull(meal)
        assertEquals(MealCategory.DESAYUNO, meal!!.suggestedMealType)
        assertEquals(VisionSource.LOCAL_DEVICE, meal.analysisSource)
        assertTrue(meal.items.any { it.name.contains("Café", ignoreCase = true) })
        assertTrue(meal.items.any { it.name.contains("Marraqueta", ignoreCase = true) })
        assertTrue(meal.items.any { it.name.contains("Palta", ignoreCase = true) })
    }

    @Test
    @DisplayName("Debe manejar multiplicadores en el fallback offline (ej. dos huevos revueltos)")
    fun testOfflineMultiplierHandling() = runTest {
        val offlineAnalyzer = RemoteNaturalLanguageMealAnalyzer(
            apiKeyProvider = { "" },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )

        val result = offlineAnalyzer.analyzeTextDescription("Dos huevos revueltos")

        assertTrue(result.isSuccess)
        val meal = result.getOrNull()
        assertNotNull(meal)
        val eggItem = meal!!.items.first { it.name.contains("Huevo", ignoreCase = true) }
        // 100g base * 2 = 200g
        assertEquals(200.0, eggItem.servingGrams)
    }

    @Test
    @DisplayName("Debe descomponer preparaciones compuestas (fajitas) en 7 ítems individuales en fallback offline")
    fun testOfflineCompoundDishDecomposition() = runTest {
        val offlineAnalyzer = RemoteNaturalLanguageMealAnalyzer(
            apiKeyProvider = { "" },
            httpClient = httpClient,
            catalogRepository = catalogRepository
        )

        val input = "me comi 2 fajitas con choclo, carne molida, lechuga, tomate aderezado con yogurt griego y tajin"
        val result = offlineAnalyzer.analyzeTextDescription(input)

        assertTrue(result.isSuccess)
        val meal = result.getOrNull()
        assertNotNull(meal)
        assertEquals(7, meal!!.items.size)

        val fajitaItem = meal.items.first { it.name == "Tortillas de fajita" }
        assertEquals(80.0, fajitaItem.servingGrams)
        assertEquals(2.0, fajitaItem.householdPortion?.quantity)

        assertTrue(meal.items.any { it.name == "Carne molida cocida" })
        assertTrue(meal.items.any { it.name == "Choclo desgranado" })
        assertTrue(meal.items.any { it.name == "Lechuga picada" })
        assertTrue(meal.items.any { it.name == "Tomate picado" })
        assertTrue(meal.items.any { it.name == "Yogurt griego" })
        assertTrue(meal.items.any { it.name == "Tajín" })
    }

    @Test
    @DisplayName("Debe parsear JSON con descomposición atómica de 7 ingredientes para fajitas")
    fun testParseCompoundDishAtomicDecomposition() {
        val jsonPayload = """
            {
              "suggested_meal_category": "Once / Cena",
              "detected_items": [
                {
                  "name": "Tortillas de fajita",
                  "serving_grams": 80.0,
                  "calories_per_100g": 300.0,
                  "protein_per_100g": 8.0,
                  "carbs_per_100g": 52.0,
                  "fat_per_100g": 6.0,
                  "confidence": 0.95,
                  "household_unit": "unidad",
                  "household_quantity": 2.0
                },
                {
                  "name": "Carne molida",
                  "serving_grams": 100.0,
                  "calories_per_100g": 250.0,
                  "protein_per_100g": 26.0,
                  "carbs_per_100g": 0.0,
                  "fat_per_100g": 15.0,
                  "confidence": 0.95,
                  "household_unit": "porción",
                  "household_quantity": 1.0
                },
                {
                  "name": "Choclo",
                  "serving_grams": 50.0,
                  "calories_per_100g": 96.0,
                  "protein_per_100g": 3.4,
                  "carbs_per_100g": 21.0,
                  "fat_per_100g": 1.5,
                  "confidence": 0.95,
                  "household_unit": "taza",
                  "household_quantity": 0.5
                },
                {
                  "name": "Lechuga",
                  "serving_grams": 30.0,
                  "calories_per_100g": 15.0,
                  "protein_per_100g": 1.4,
                  "carbs_per_100g": 2.9,
                  "fat_per_100g": 0.2,
                  "confidence": 0.95,
                  "household_unit": "taza",
                  "household_quantity": 0.5
                },
                {
                  "name": "Tomate picado",
                  "serving_grams": 50.0,
                  "calories_per_100g": 18.0,
                  "protein_per_100g": 0.9,
                  "carbs_per_100g": 3.9,
                  "fat_per_100g": 0.2,
                  "confidence": 0.95,
                  "household_unit": "porción",
                  "household_quantity": 1.0
                },
                {
                  "name": "Yogurt griego",
                  "serving_grams": 40.0,
                  "calories_per_100g": 97.0,
                  "protein_per_100g": 9.0,
                  "carbs_per_100g": 4.0,
                  "fat_per_100g": 5.0,
                  "confidence": 0.95,
                  "household_unit": "cda",
                  "household_quantity": 2.0
                },
                {
                  "name": "Tajín",
                  "serving_grams": 3.0,
                  "calories_per_100g": 0.0,
                  "protein_per_100g": 0.0,
                  "carbs_per_100g": 0.0,
                  "fat_per_100g": 0.0,
                  "confidence": 0.95,
                  "household_unit": "cdta",
                  "household_quantity": 1.0
                }
              ]
            }
        """.trimIndent()

        val dto = analyzer.parseMealDto(jsonPayload)
        val result = analyzer.mapDtoToResult(dto)

        assertEquals(MealCategory.ONCE_CENA, result.suggestedMealType)
        assertEquals(7, result.items.size)
        val fajita = result.items[0]
        assertEquals("Tortillas de fajita", fajita.name)
        assertEquals(80.0, fajita.servingGrams)
        assertEquals(2.0, fajita.householdPortion?.quantity)
    }

    @Test
    @DisplayName("Debe retornar error al enviar descripción vacía")
    fun testEmptyDescriptionFails() = runTest {
        val result = analyzer.analyzeTextDescription("   ")
        assertTrue(result.isFailure)
        assertEquals("Ingresa una descripción de tu comida.", result.exceptionOrNull()?.message)
    }
}
