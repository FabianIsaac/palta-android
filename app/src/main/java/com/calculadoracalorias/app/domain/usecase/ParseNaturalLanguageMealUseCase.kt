package com.calculadoracalorias.app.domain.usecase

import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.repository.NaturalLanguageMealAnalyzer

/**
 * Caso de uso para analizar descripciones en lenguaje natural chileno,
 * validando la entrada y delegando al analizador de IA correspondiente.
 */
class ParseNaturalLanguageMealUseCase(
    private val analyzer: NaturalLanguageMealAnalyzer
) {
    suspend operator fun invoke(description: String): Result<DetectedMealResult> {
        val sanitized = description.trim()
        if (sanitized.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa una descripción de tu comida."))
        }
        return analyzer.analyzeTextDescription(sanitized)
    }
}
