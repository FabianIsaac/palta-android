package com.calculadoracalorias.app.data.local

import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import java.util.UUID

class LocalFoodCatalogRepository : FoodCatalogRepository {

    private val defaultCatalog = listOf(
        CatalogItem("Marraqueta", 280.0, 9.0, 56.0, 1.0),
        CatalogItem("Hallulla", 310.0, 8.5, 58.0, 4.5),
        CatalogItem("Palta hass", 160.0, 2.0, 9.0, 15.0),
        CatalogItem("Pechuga de pollo a la plancha", 165.0, 31.0, 0.0, 3.6),
        CatalogItem("Arroz blanco cocido", 130.0, 2.7, 28.2, 0.3),
        CatalogItem("Cazuela de vacuno", 95.0, 8.0, 6.0, 4.0),
        CatalogItem("Porotos granados", 125.0, 6.5, 20.0, 1.8),
        CatalogItem("Pastel de choclo", 185.0, 8.5, 22.0, 7.0),
        CatalogItem("Huevo revuelto", 150.0, 10.0, 1.5, 11.0),
        CatalogItem("Manzana", 52.0, 0.3, 13.8, 0.2),
        CatalogItem("Plátano", 89.0, 1.1, 22.8, 0.3),
        CatalogItem("Ensalada a la chilena", 35.0, 1.0, 4.0, 1.8),
        CatalogItem("Tomate picado", 18.0, 0.9, 3.9, 0.2),
        CatalogItem("Avena en hojuelas", 389.0, 16.9, 66.3, 6.9)
    )

    override suspend fun searchFood(query: String): List<ScannedFoodItem> {
        val sanitized = query.trim().lowercase()
        return defaultCatalog
            .filter { it.name.lowercase().contains(sanitized) }
            .map { it.toScannedFoodItem() }
    }

    override suspend fun findBestMatch(label: String): ScannedFoodItem? {
        val sanitized = label.trim().lowercase()
        val directMatch = defaultCatalog.firstOrNull { it.name.lowercase().contains(sanitized) }
        if (directMatch != null) return directMatch.toScannedFoodItem()

        // Búsqueda por palabras clave aproximadas
        val keywords = sanitized.split(" ", "-", "_")
        val fuzzyMatch = defaultCatalog.firstOrNull { catalogItem ->
            keywords.any { catalogItem.name.lowercase().contains(it) }
        }
        return fuzzyMatch?.toScannedFoodItem()
    }

    override suspend fun getPopularFoods(): List<ScannedFoodItem> {
        return defaultCatalog.take(5).map { it.toScannedFoodItem() }
    }

    private data class CatalogItem(
        val name: String,
        val caloriesPer100g: Double,
        val proteinPer100g: Double,
        val carbsPer100g: Double,
        val fatPer100g: Double
    ) {
        fun toScannedFoodItem(servingGrams: Double = 100.0, confidence: Float = 0.95f): ScannedFoodItem {
            return ScannedFoodItem(
                id = UUID.randomUUID().toString(),
                name = name,
                servingGrams = servingGrams,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                confidence = confidence
            )
        }
    }
}
