package com.calculadoracalorias.app.domain.model.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupDataPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val appVersionName: String,
    val meals: List<BackupMealEntry>,
    val supplements: List<BackupSupplement> = emptyList(),
    val supplementLogs: List<BackupSupplementLog> = emptyList(),
    val preferences: BackupPreferences
) {
    companion object {
        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun fromJson(jsonString: String): BackupDataPayload {
            return json.decodeFromString(serializer(), jsonString)
        }

        fun toJson(payload: BackupDataPayload): String {
            return json.encodeToString(serializer(), payload)
        }
    }
}

@Serializable
data class BackupMealEntry(
    val id: Long,
    val category: String,
    val timestamp: Long,
    val totalCalories: Double,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val notes: String? = null,
    val rawDescription: String? = null,
    val isPendingAiRefinement: Boolean = false,
    val items: List<BackupMealItem>
)

@Serializable
data class BackupMealItem(
    val id: Long = 0,
    val name: String,
    val portionGrams: Double,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val householdMeasure: String? = null,
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0
)

@Serializable
data class BackupSupplement(
    val id: String,
    val name: String,
    val dosageDescription: String,
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val isActive: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = 0L
)

@Serializable
data class BackupSupplementLog(
    val date: String,
    val supplementId: String,
    val takenTimestamp: Long
)

@Serializable
data class BackupPreferences(
    val targetCalories: Double = 2000.0,
    val targetProteinGrams: Double = 150.0,
    val targetCarbsGrams: Double = 200.0,
    val targetFatGrams: Double = 65.0,
    val mealTimeWindows: BackupMealTimeWindows = BackupMealTimeWindows()
)

@Serializable
data class BackupMealTimeWindows(
    val breakfastStartMinute: Int = 420,
    val breakfastEndMinute: Int = 660,
    val lunchStartMinute: Int = 720,
    val lunchEndMinute: Int = 960,
    val dinnerStartMinute: Int = 1140,
    val dinnerEndMinute: Int = 1380
)
