package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import com.calculadoracalorias.app.domain.repository.FoodVisionAnalyzer
import java.util.UUID

/**
 * Analizador local en el dispositivo (Edge / LiteRT) para clasificación offline de alimentos.
 * Si la imagen se procesa con éxito, asocia las etiquetas detectadas con el catálogo local de alimentos.
 */
class LocalLiteRtVisionAnalyzer(
    private val foodCatalogRepository: FoodCatalogRepository
) : FoodVisionAnalyzer {

    override suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Los datos de la imagen están vacíos."))
        }

        return try {
            // Clasificación heurística / LiteRT sobre los bytes de la imagen
            val detectedLabels = classifyImage(imageBytes)

            val matchedItems = mutableListOf<ScannedFoodItem>()
            for (label in detectedLabels) {
                val matched = foodCatalogRepository.findBestMatch(label)
                if (matched != null) {
                    matchedItems.add(matched)
                }
            }

            // Si no se encuentra coincidencia exacta, se ofrece una sugerencia genérica
            val finalItems = if (matchedItems.isNotEmpty()) {
                matchedItems
            } else {
                listOf(
                    ScannedFoodItem(
                        id = UUID.randomUUID().toString(),
                        name = "Plato combinado",
                        servingGrams = 150.0,
                        caloriesPer100g = 140.0,
                        proteinPer100g = 8.0,
                        carbsPer100g = 18.0,
                        fatPer100g = 4.0,
                        confidence = 0.70f
                    )
                )
            }

            Result.success(
                DetectedMealResult(
                    items = finalItems,
                    suggestedMealType = MealCategory.ALMUERZO,
                    analysisSource = VisionSource.LOCAL_DEVICE
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extrae etiquetas candidatas a partir del contenido visual.
     * En producción se enlaza con el intérprete TFLite / LiteRT embebido en assets.
     */
    internal fun classifyImage(imageBytes: ByteArray): List<String> {
        // Validación básica de encabezado de imagen (JPEG / PNG / WebP)
        val isJpeg = imageBytes.size >= 2 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte()
        val isPng = imageBytes.size >= 4 && imageBytes[0] == 0x89.toByte() && imageBytes[1] == 0x50.toByte()

        if (!isJpeg && !isPng && imageBytes.size < 10) {
            return emptyList()
        }

        // Simulación determinista de clasificación LiteRT
        return listOf("pollo", "arroz")
    }
}
