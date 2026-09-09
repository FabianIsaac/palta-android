package com.calculadoracalorias.app.data.local

import com.calculadoracalorias.app.domain.model.HouseholdPortion
import com.calculadoracalorias.app.domain.model.HouseholdUnit
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import java.util.UUID

class LocalFoodCatalogRepository : FoodCatalogRepository {

    private val defaultCatalog = listOf(
        CatalogItem(
            name = "Café negro",
            caloriesPer100g = 2.0,
            proteinPer100g = 0.1,
            carbsPer100g = 0.2,
            fatPer100g = 0.0,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Café con leche",
            caloriesPer100g = 35.0,
            proteinPer100g = 3.4,
            carbsPer100g = 4.8,
            fatPer100g = 0.2,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Café con leche entera",
            caloriesPer100g = 62.0,
            proteinPer100g = 3.2,
            carbsPer100g = 4.7,
            fatPer100g = 3.5,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Café cortado",
            caloriesPer100g = 25.0,
            proteinPer100g = 1.8,
            carbsPer100g = 2.5,
            fatPer100g = 1.0,
            defaultServingGrams = 150.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 0.75, 150.0)
        ),
        CatalogItem(
            name = "Té",
            caloriesPer100g = 1.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.2,
            fatPer100g = 0.0,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Azúcar blanca",
            caloriesPer100g = 387.0,
            proteinPer100g = 0.0,
            carbsPer100g = 100.0,
            fatPer100g = 0.0,
            defaultServingGrams = 5.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.TEASPOON, 1.0, 5.0)
        ),
        CatalogItem(
            name = "Endulzante",
            caloriesPer100g = 0.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 0.0,
            defaultServingGrams = 1.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 1.0)
        ),
        CatalogItem(
            name = "Leche descremada",
            caloriesPer100g = 34.0,
            proteinPer100g = 3.4,
            carbsPer100g = 4.9,
            fatPer100g = 0.1,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Leche entera",
            caloriesPer100g = 61.0,
            proteinPer100g = 3.1,
            carbsPer100g = 4.7,
            fatPer100g = 3.3,
            defaultServingGrams = 200.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 200.0)
        ),
        CatalogItem(
            name = "Yogur natural",
            caloriesPer100g = 59.0,
            proteinPer100g = 3.5,
            carbsPer100g = 4.7,
            fatPer100g = 3.3,
            defaultServingGrams = 125.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 125.0)
        ),
        CatalogItem(
            name = "Marraqueta",
            caloriesPer100g = 280.0,
            proteinPer100g = 9.0,
            carbsPer100g = 56.0,
            fatPer100g = 1.0,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.UNIT, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Hallulla",
            caloriesPer100g = 310.0,
            proteinPer100g = 8.5,
            carbsPer100g = 58.0,
            fatPer100g = 4.5,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.UNIT, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Palta hass",
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 9.0,
            fatPer100g = 15.0,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Pechuga de pollo a la plancha",
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            defaultServingGrams = 150.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 150.0)
        ),
        CatalogItem(
            name = "Arroz blanco cocido",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatPer100g = 0.3,
            defaultServingGrams = 150.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 150.0)
        ),
        CatalogItem(
            name = "Cazuela de vacuno",
            caloriesPer100g = 95.0,
            proteinPer100g = 8.0,
            carbsPer100g = 6.0,
            fatPer100g = 4.0,
            defaultServingGrams = 350.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 350.0)
        ),
        CatalogItem(
            name = "Porotos granados",
            caloriesPer100g = 125.0,
            proteinPer100g = 6.5,
            carbsPer100g = 20.0,
            fatPer100g = 1.8,
            defaultServingGrams = 300.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 300.0)
        ),
        CatalogItem(
            name = "Pastel de choclo",
            caloriesPer100g = 185.0,
            proteinPer100g = 8.5,
            carbsPer100g = 22.0,
            fatPer100g = 7.0,
            defaultServingGrams = 300.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 300.0)
        ),
        CatalogItem(
            name = "Huevo revuelto",
            caloriesPer100g = 150.0,
            proteinPer100g = 10.0,
            carbsPer100g = 1.5,
            fatPer100g = 11.0,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Manzana",
            caloriesPer100g = 52.0,
            proteinPer100g = 0.3,
            carbsPer100g = 13.8,
            fatPer100g = 0.2,
            defaultServingGrams = 150.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.UNIT, 1.0, 150.0)
        ),
        CatalogItem(
            name = "Plátano",
            caloriesPer100g = 89.0,
            proteinPer100g = 1.1,
            carbsPer100g = 22.8,
            fatPer100g = 0.3,
            defaultServingGrams = 120.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.UNIT, 1.0, 120.0)
        ),
        CatalogItem(
            name = "Ensalada a la chilena",
            caloriesPer100g = 35.0,
            proteinPer100g = 1.0,
            carbsPer100g = 4.0,
            fatPer100g = 1.8,
            defaultServingGrams = 150.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 150.0)
        ),
        CatalogItem(
            name = "Tomate picado",
            caloriesPer100g = 18.0,
            proteinPer100g = 0.9,
            carbsPer100g = 3.9,
            fatPer100g = 0.2,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Avena en hojuelas",
            caloriesPer100g = 389.0,
            proteinPer100g = 16.9,
            carbsPer100g = 66.3,
            fatPer100g = 6.9,
            defaultServingGrams = 50.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.TABLESPOON, 4.0, 50.0)
        ),
        CatalogItem(
            name = "Tortillas de fajita",
            caloriesPer100g = 300.0,
            proteinPer100g = 8.0,
            carbsPer100g = 52.0,
            fatPer100g = 6.0,
            defaultServingGrams = 40.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.UNIT, 1.0, 40.0)
        ),
        CatalogItem(
            name = "Carne molida cocida",
            caloriesPer100g = 250.0,
            proteinPer100g = 26.0,
            carbsPer100g = 0.0,
            fatPer100g = 15.0,
            defaultServingGrams = 100.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.PORTION, 1.0, 100.0)
        ),
        CatalogItem(
            name = "Choclo desgranado",
            caloriesPer100g = 96.0,
            proteinPer100g = 3.4,
            carbsPer100g = 21.0,
            fatPer100g = 1.5,
            defaultServingGrams = 80.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 0.5, 80.0)
        ),
        CatalogItem(
            name = "Lechuga picada",
            caloriesPer100g = 15.0,
            proteinPer100g = 1.4,
            carbsPer100g = 2.9,
            fatPer100g = 0.2,
            defaultServingGrams = 50.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.CUP, 1.0, 50.0)
        ),
        CatalogItem(
            name = "Yogurt griego",
            caloriesPer100g = 97.0,
            proteinPer100g = 9.0,
            carbsPer100g = 4.0,
            fatPer100g = 5.0,
            defaultServingGrams = 60.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.TABLESPOON, 3.0, 60.0)
        ),
        CatalogItem(
            name = "Tajín",
            caloriesPer100g = 0.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 0.0,
            defaultServingGrams = 2.0,
            defaultHouseholdPortion = HouseholdPortion(HouseholdUnit.TEASPOON, 0.5, 2.0)
        )
    )

    override suspend fun searchFood(query: String): List<ScannedFoodItem> {
        val sanitized = query.trim().lowercase()
        if (sanitized.isBlank()) return emptyList()
        return defaultCatalog
            .filter { it.name.lowercase().contains(sanitized) }
            .map { it.toScannedFoodItem() }
    }

    override suspend fun findBestMatch(label: String): ScannedFoodItem? {
        val sanitized = label.trim().lowercase()
        if (sanitized.isBlank()) return null

        // Coincidencia exacta o contenida directa
        val directMatch = defaultCatalog.firstOrNull { it.name.lowercase() == sanitized }
            ?: defaultCatalog.firstOrNull { it.name.lowercase().contains(sanitized) }
        if (directMatch != null) return directMatch.toScannedFoodItem()

        // Búsqueda por palabras clave aproximadas
        val keywords = sanitized.split(" ", "-", "_").filter { it.length > 2 }
        val fuzzyMatch = defaultCatalog.firstOrNull { catalogItem ->
            val itemNameLower = catalogItem.name.lowercase()
            keywords.any { itemNameLower.contains(it) }
        }
        return fuzzyMatch?.toScannedFoodItem()
    }

    override suspend fun getPopularFoods(): List<ScannedFoodItem> {
        return defaultCatalog.take(6).map { it.toScannedFoodItem() }
    }

    private data class CatalogItem(
        val name: String,
        val caloriesPer100g: Double,
        val proteinPer100g: Double,
        val carbsPer100g: Double,
        val fatPer100g: Double,
        val defaultServingGrams: Double = 100.0,
        val defaultHouseholdPortion: HouseholdPortion? = null
    ) {
        fun toScannedFoodItem(
            servingGrams: Double = defaultServingGrams,
            confidence: Float = 0.95f
        ): ScannedFoodItem {
            val portion = if (servingGrams == defaultServingGrams) {
                defaultHouseholdPortion
            } else {
                defaultHouseholdPortion?.let {
                    val ratio = if (defaultServingGrams > 0.0) servingGrams / defaultServingGrams else 1.0
                    it.copy(
                        quantity = it.quantity * ratio,
                        equivalentGrams = servingGrams
                    )
                }
            }

            return ScannedFoodItem(
                id = UUID.randomUUID().toString(),
                name = name,
                servingGrams = servingGrams,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                confidence = confidence,
                householdPortion = portion
            )
        }
    }
}
