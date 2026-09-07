package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.ScannedFoodItem

/**
 * Contrato de repositorio para consultar el catálogo local de alimentos y tablas nutricionales.
 */
interface FoodCatalogRepository {
    suspend fun searchFood(query: String): List<ScannedFoodItem>
    suspend fun findBestMatch(label: String): ScannedFoodItem?
    suspend fun getPopularFoods(): List<ScannedFoodItem>
}
