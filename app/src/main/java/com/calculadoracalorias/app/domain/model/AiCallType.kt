package com.calculadoracalorias.app.domain.model

/**
 * Tipo de interacción realizada con el modelo de Inteligencia Artificial.
 */
enum class AiCallType(val displayName: String) {
    MEAL_TEXT("Texto de comida"),
    MEAL_IMAGE("Imagen de comida"),
    SUPPLEMENT_TEXT("Suplemento"),
    NUTRITION_LABEL("Tabla nutricional"),
    CONNECTIVITY_TEST("Prueba de conexión")
}
