package com.calculadoracalorias.app.domain.repository

import com.calculadoracalorias.app.domain.model.DetectedMealResult

/**
 * Contrato de repositorio para analizar descripciones en lenguaje cotidiano / natural
 * y traducirlas a alimentos detectados con estimación de porciones y macronutrientes.
 */
interface NaturalLanguageMealAnalyzer {
    suspend fun analyzeTextDescription(description: String): Result<DetectedMealResult>
}
