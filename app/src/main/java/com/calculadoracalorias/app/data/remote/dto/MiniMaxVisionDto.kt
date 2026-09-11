package com.calculadoracalorias.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float = 0.1f
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>
)

@Serializable
sealed interface OpenAiContentPart

@Serializable
@SerialName("text")
data class OpenAiTextPart(
    val text: String
) : OpenAiContentPart

@Serializable
@SerialName("image_url")
data class OpenAiImageUrlPart(
    @SerialName("image_url")
    val imageUrl: OpenAiImageUrl
) : OpenAiContentPart

@Serializable
data class OpenAiImageUrl(
    val url: String // data:image/jpeg;base64,...
)

@Serializable
data class OpenAiChatResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice> = emptyList(),
    val error: OpenAiError? = null
)

@Serializable
data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiResponseMessage
)

@Serializable
data class OpenAiResponseMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

/**
 * Estructura JSON esperada del modelo de visión y texto
 */
@Serializable
data class OpenAiDetectedMealDto(
    @SerialName("suggested_meal_category")
    val suggestedMealCategory: String? = null,
    @SerialName("detected_items")
    val detectedItems: List<OpenAiFoodItemDto> = emptyList()
)

@Serializable
data class OpenAiFoodItemDto(
    val name: String,
    @SerialName("serving_grams")
    val servingGrams: Double = 100.0,
    @SerialName("calories_per_100g")
    val caloriesPer100g: Double,
    @SerialName("protein_per_100g")
    val proteinPer100g: Double,
    @SerialName("carbs_per_100g")
    val carbsPer100g: Double,
    @SerialName("fat_per_100g")
    val fatPer100g: Double,
    val confidence: Float = 0.9f,
    @SerialName("household_unit")
    val householdUnit: String? = null,
    @SerialName("household_quantity")
    val householdQuantity: Double? = null
)

@Serializable
data class OpenAiSupplementEstimateDto(
    val name: String? = null,
    @SerialName("dosage_description")
    val dosageDescription: String? = null,
    val calories: Double = 0.0,
    @SerialName("protein_grams")
    val proteinGrams: Double = 0.0,
    @SerialName("carbs_grams")
    val carbsGrams: Double = 0.0,
    @SerialName("fat_grams")
    val fatGrams: Double = 0.0
)

@Serializable
data class OpenAiNutritionLabelDto(
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("serving_description")
    val servingDescription: String? = null,
    @SerialName("serving_grams")
    val servingGrams: Double? = null,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

// Alias retrocompatibles con código preexistente
typealias MiniMaxChatRequest = OpenAiChatRequest
typealias MiniMaxMessage = OpenAiMessage
typealias MiniMaxContentPart = OpenAiContentPart
typealias MiniMaxTextPart = OpenAiTextPart
typealias MiniMaxImageUrlPart = OpenAiImageUrlPart
typealias MiniMaxImageUrl = OpenAiImageUrl
typealias MiniMaxChatResponse = OpenAiChatResponse
typealias MiniMaxChoice = OpenAiChoice
typealias MiniMaxResponseMessage = OpenAiResponseMessage
typealias MiniMaxError = OpenAiError
typealias MiniMaxDetectedMealDto = OpenAiDetectedMealDto
typealias MiniMaxFoodItemDto = OpenAiFoodItemDto
typealias MiniMaxSupplementEstimateDto = OpenAiSupplementEstimateDto
typealias MiniMaxNutritionLabelDto = OpenAiNutritionLabelDto
