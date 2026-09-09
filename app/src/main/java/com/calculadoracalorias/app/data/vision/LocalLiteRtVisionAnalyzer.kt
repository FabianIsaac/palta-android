package com.calculadoracalorias.app.data.vision

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.FoodCatalogRepository
import com.calculadoracalorias.app.domain.repository.FoodVisionAnalyzer

/**
 * Analizador local en el dispositivo (Edge / LiteRT) para clasificación offline de alimentos.
 * Si la imagen se procesa con éxito, asocia las etiquetas detectadas con el catálogo local de alimentos.
 * Si no se encuentra una coincidencia confiable (>= 0.60), notifica al usuario transparentemente
 * en lugar de fabricar alimentos falsos.
 */
class LocalLiteRtVisionAnalyzer(
    private val foodCatalogRepository: FoodCatalogRepository
) : FoodVisionAnalyzer {

    companion object {
        const val MINIMUM_CONFIDENCE_THRESHOLD = 0.60f
        const val UNCERTAIN_DETECTION_MESSAGE = "No pudimos identificar con certeza tu comida. Puedes buscarla por nombre o describirla."
    }

    override suspend fun analyzeImage(imageBytes: ByteArray): Result<DetectedMealResult> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Los datos de la imagen están vacíos."))
        }

        return try {
            val detectedLabels = classifyImage(imageBytes)

            val matchedItems = mutableListOf<ScannedFoodItem>()
            for (label in detectedLabels) {
                val matched = foodCatalogRepository.findBestMatch(label)
                if (matched != null && matched.confidence >= MINIMUM_CONFIDENCE_THRESHOLD) {
                    matchedItems.add(matched)
                }
            }

            if (matchedItems.isEmpty()) {
                Result.failure(NoSuchElementException(UNCERTAIN_DETECTION_MESSAGE))
            } else {
                Result.success(
                    DetectedMealResult(
                        items = matchedItems,
                        suggestedMealType = MealCategory.ALMUERZO,
                        analysisSource = VisionSource.LOCAL_DEVICE
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extrae etiquetas candidatas a partir del contenido visual.
     * Si no hay un modelo TFLite / LiteRT local inicializado en assets, retorna lista vacía
     * para no inducir a falsos positivos.
     */
    internal fun classifyImage(imageBytes: ByteArray): List<String> {
        val isJpeg = imageBytes.size >= 2 && imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte()
        val isPng = imageBytes.size >= 4 && imageBytes[0] == 0x89.toByte() && imageBytes[1] == 0x50.toByte()

        if (!isJpeg && !isPng && imageBytes.size < 10) {
            return emptyList()
        }

        // Sin modelo TFLite embebido en dispositivo, no se simulan detecciones fijas
        return emptyList()
    }
}
