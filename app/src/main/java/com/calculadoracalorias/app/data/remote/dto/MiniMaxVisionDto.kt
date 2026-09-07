package com.calculadoracalorias.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MiniMaxChatRequest(
    val model: String = "MiniMax-VL-01",
    val messages: List<MiniMaxMessage>,
    val temperature: Float = 0.2f
)

@Serializable
data class MiniMaxMessage(
    val role: String,
    val content: List<MiniMaxContentPart>
)

@Serializable
sealed interface MiniMaxContentPart

@Serializable
@SerialName("text")
data class MiniMaxTextPart(
    val text: String
) : MiniMaxContentPart

@Serializable
@SerialName("image_url")
data class MiniMaxImageUrlPart(
    @SerialName("image_url")
    val imageUrl: MiniMaxImageUrl
) : MiniMaxContentPart

@Serializable
data class MiniMaxImageUrl(
    val url: String // data:image/jpeg;base64,...
)

@Serializable
data class MiniMaxChatResponse(
    val id: String? = null,
    val choices: List<MiniMaxChoice> = emptyList(),
    val error: MiniMaxError? = null
)

@Serializable
data class MiniMaxChoice(
    val index: Int = 0,
    val message: MiniMaxResponseMessage
)

@Serializable
data class MiniMaxResponseMessage(
    val role: String,
    val content: String
)

@Serializable
data class MiniMaxError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

/**
 * Estructura JSON esperada del modelo de visión
 */
@Serializable
data class MiniMaxDetectedMealDto(
    @SerialName("suggested_meal_category")
    val suggestedMealCategory: String? = null,
    @SerialName("detected_items")
    val detectedItems: List<MiniMaxFoodItemDto> = emptyList()
)

@Serializable
data class MiniMaxFoodItemDto(
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
    val confidence: Float = 0.9f
)
