package com.calculadoracalorias.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.repository.DailyBudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

data class UserPreferences(
    val miniMaxApiKey: String = "",
    val preferredVisionSource: VisionSource = VisionSource.LOCAL_DEVICE,
    val aiProvider: AiProvider = AiProvider.NVIDIA_NIM,
    val apiKeyNvidia: String = "",
    val apiKeyGemini: String = "",
    val apiKeyMiniMax: String = "",
    val apiKeyCustom: String = "",
    val customEndpointUrl: String = "",
    val customTextModel: String = "",
    val customVisionModel: String = "",
    val healthConnectSyncEnabled: Boolean = true,
    val targetCalories: Double = 2000.0,
    val targetProteinGrams: Double = 150.0,
    val targetCarbsGrams: Double = 200.0,
    val targetFatGrams: Double = 65.0,
    val mealTimeWindows: MealTimeWindows = MealTimeWindows(),
    val lastBackupTimestamp: Long? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val autoBackupFolderName: String? = null,
    val lastAutoBackupTimestamp: Long? = null
) {
    val effectiveMiniMaxApiKey: String
        get() = apiKeyMiniMax.ifBlank { miniMaxApiKey }

    val activeApiKey: String
        get() = when (aiProvider) {
            AiProvider.LOCAL -> ""
            AiProvider.NVIDIA_NIM -> apiKeyNvidia
            AiProvider.GOOGLE_GEMINI -> apiKeyGemini
            AiProvider.MINIMAX -> effectiveMiniMaxApiKey
            AiProvider.CUSTOM -> apiKeyCustom
        }

    val aiConfiguration: AiConfiguration
        get() = AiConfiguration(
            provider = aiProvider,
            apiKey = activeApiKey,
            customEndpointUrl = customEndpointUrl,
            customTextModel = customTextModel,
            customVisionModel = customVisionModel
        )
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : DailyBudgetRepository {
    private object PreferencesKeys {
        val MINIMAX_API_KEY = stringPreferencesKey("minimax_api_key")
        val PREFERRED_VISION_SOURCE = stringPreferencesKey("preferred_vision_source")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val API_KEY_NVIDIA = stringPreferencesKey("api_key_nvidia")
        val API_KEY_GEMINI = stringPreferencesKey("api_key_gemini")
        val API_KEY_MINIMAX = stringPreferencesKey("api_key_minimax")
        val API_KEY_CUSTOM = stringPreferencesKey("api_key_custom")
        val CUSTOM_ENDPOINT_URL = stringPreferencesKey("custom_endpoint_url")
        val CUSTOM_TEXT_MODEL = stringPreferencesKey("custom_text_model")
        val CUSTOM_VISION_MODEL = stringPreferencesKey("custom_vision_model")
        val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
        val TARGET_CALORIES = doublePreferencesKey("target_calories")
        val TARGET_PROTEIN = doublePreferencesKey("target_protein_grams")
        val TARGET_CARBS = doublePreferencesKey("target_carbs_grams")
        val TARGET_FAT = doublePreferencesKey("target_fat_grams")
        val BREAKFAST_START_MINUTE = intPreferencesKey("breakfast_start_minute")
        val BREAKFAST_END_MINUTE = intPreferencesKey("breakfast_end_minute")
        val LUNCH_START_MINUTE = intPreferencesKey("lunch_start_minute")
        val LUNCH_END_MINUTE = intPreferencesKey("lunch_end_minute")
        val DINNER_START_MINUTE = intPreferencesKey("dinner_start_minute")
        val DINNER_END_MINUTE = intPreferencesKey("dinner_end_minute")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val AUTO_BACKUP_FOLDER_NAME = stringPreferencesKey("auto_backup_folder_name")
        val LAST_AUTO_BACKUP_TIMESTAMP = longPreferencesKey("last_auto_backup_timestamp")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val legacyMiniMaxKey = preferences[PreferencesKeys.MINIMAX_API_KEY] ?: ""
        val newMiniMaxKey = preferences[PreferencesKeys.API_KEY_MINIMAX]
        val resolvedMiniMaxKey = newMiniMaxKey ?: legacyMiniMaxKey

        val aiProviderStr = preferences[PreferencesKeys.AI_PROVIDER]
        val preferredSourceStr = preferences[PreferencesKeys.PREFERRED_VISION_SOURCE]

        val provider = if (aiProviderStr != null) {
            AiProvider.fromId(aiProviderStr)
        } else if (preferredSourceStr == VisionSource.MINIMAX_CLOUD.name || legacyMiniMaxKey.isNotBlank()) {
            AiProvider.MINIMAX
        } else if (preferredSourceStr == VisionSource.LOCAL_DEVICE.name) {
            AiProvider.LOCAL
        } else {
            AiProvider.NVIDIA_NIM
        }

        val preferredSource = if (preferredSourceStr != null) {
            try {
                VisionSource.valueOf(preferredSourceStr)
            } catch (_: Exception) {
                provider.toVisionSource()
            }
        } else {
            provider.toVisionSource()
        }

        val nvidiaKey = preferences[PreferencesKeys.API_KEY_NVIDIA] ?: ""
        val geminiKey = preferences[PreferencesKeys.API_KEY_GEMINI] ?: ""
        val customKey = preferences[PreferencesKeys.API_KEY_CUSTOM] ?: ""
        val customEndpoint = preferences[PreferencesKeys.CUSTOM_ENDPOINT_URL] ?: ""
        val customText = preferences[PreferencesKeys.CUSTOM_TEXT_MODEL] ?: ""
        val customVision = preferences[PreferencesKeys.CUSTOM_VISION_MODEL] ?: ""

        val syncEnabled = preferences[PreferencesKeys.HEALTH_CONNECT_SYNC_ENABLED] ?: true
        val calories = preferences[PreferencesKeys.TARGET_CALORIES] ?: 2000.0
        val protein = preferences[PreferencesKeys.TARGET_PROTEIN] ?: 150.0
        val carbs = preferences[PreferencesKeys.TARGET_CARBS] ?: 200.0
        val fat = preferences[PreferencesKeys.TARGET_FAT] ?: 65.0
        val lastBackup = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP]
        val autoBackupEnabled = preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] ?: false
        val autoBackupFolderUri = preferences[PreferencesKeys.AUTO_BACKUP_FOLDER_URI]
        val autoBackupFolderName = preferences[PreferencesKeys.AUTO_BACKUP_FOLDER_NAME]
        val lastAutoBackup = preferences[PreferencesKeys.LAST_AUTO_BACKUP_TIMESTAMP]

        val defaultWindows = MealTimeWindows()
        val bStart = preferences[PreferencesKeys.BREAKFAST_START_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.breakfastStart
        val bEnd = preferences[PreferencesKeys.BREAKFAST_END_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.breakfastEnd
        val lStart = preferences[PreferencesKeys.LUNCH_START_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.lunchStart
        val lEnd = preferences[PreferencesKeys.LUNCH_END_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.lunchEnd
        val dStart = preferences[PreferencesKeys.DINNER_START_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.dinnerStart
        val dEnd = preferences[PreferencesKeys.DINNER_END_MINUTE]?.let { minuteToLocalTime(it) } ?: defaultWindows.dinnerEnd

        val mealWindows = MealTimeWindows(
            breakfastStart = bStart,
            breakfastEnd = bEnd,
            lunchStart = lStart,
            lunchEnd = lEnd,
            dinnerStart = dStart,
            dinnerEnd = dEnd
        )

        UserPreferences(
            miniMaxApiKey = resolvedMiniMaxKey,
            preferredVisionSource = preferredSource,
            aiProvider = provider,
            apiKeyNvidia = nvidiaKey,
            apiKeyGemini = geminiKey,
            apiKeyMiniMax = resolvedMiniMaxKey,
            apiKeyCustom = customKey,
            customEndpointUrl = customEndpoint,
            customTextModel = customText,
            customVisionModel = customVision,
            healthConnectSyncEnabled = syncEnabled,
            targetCalories = calories,
            targetProteinGrams = protein,
            targetCarbsGrams = carbs,
            targetFatGrams = fat,
            mealTimeWindows = mealWindows,
            lastBackupTimestamp = lastBackup,
            autoBackupEnabled = autoBackupEnabled,
            autoBackupFolderUri = autoBackupFolderUri,
            autoBackupFolderName = autoBackupFolderName,
            lastAutoBackupTimestamp = lastAutoBackup
        )
    }

    override fun getDailyBudget(): Flow<DailyMacroBudget> {
        return userPreferencesFlow.map {
            DailyMacroBudget(
                targetCalories = it.targetCalories,
                targetProteinGrams = it.targetProteinGrams,
                targetCarbsGrams = it.targetCarbsGrams,
                targetFatGrams = it.targetFatGrams
            )
        }
    }

    suspend fun getMiniMaxApiKey(): String {
        return userPreferencesFlow.first().effectiveMiniMaxApiKey
    }

    suspend fun getAiConfiguration(): AiConfiguration {
        return userPreferencesFlow.first().aiConfiguration
    }

    suspend fun setAiProvider(provider: AiProvider) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER] = provider.id
            preferences[PreferencesKeys.PREFERRED_VISION_SOURCE] = provider.toVisionSource().name
        }
    }

    suspend fun setApiKeyForProvider(provider: AiProvider, apiKey: String) {
        dataStore.edit { preferences ->
            val trimmed = apiKey.trim()
            when (provider) {
                AiProvider.LOCAL -> { /* Sin clave */ }
                AiProvider.NVIDIA_NIM -> preferences[PreferencesKeys.API_KEY_NVIDIA] = trimmed
                AiProvider.GOOGLE_GEMINI -> preferences[PreferencesKeys.API_KEY_GEMINI] = trimmed
                AiProvider.MINIMAX -> {
                    preferences[PreferencesKeys.API_KEY_MINIMAX] = trimmed
                    preferences[PreferencesKeys.MINIMAX_API_KEY] = trimmed
                }
                AiProvider.CUSTOM -> preferences[PreferencesKeys.API_KEY_CUSTOM] = trimmed
            }
        }
    }

    suspend fun setCustomAiParameters(endpointUrl: String, textModel: String, visionModel: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_ENDPOINT_URL] = endpointUrl.trim()
            preferences[PreferencesKeys.CUSTOM_TEXT_MODEL] = textModel.trim()
            preferences[PreferencesKeys.CUSTOM_VISION_MODEL] = visionModel.trim()
        }
    }

    suspend fun setMiniMaxApiKey(apiKey: String) {
        setApiKeyForProvider(AiProvider.MINIMAX, apiKey)
    }

    suspend fun setPreferredVisionSource(source: VisionSource) {
        val mappedProvider = when (source) {
            VisionSource.LOCAL_DEVICE -> AiProvider.LOCAL
            VisionSource.MINIMAX_CLOUD -> AiProvider.MINIMAX
            VisionSource.NVIDIA_NIM -> AiProvider.NVIDIA_NIM
            VisionSource.GOOGLE_GEMINI -> AiProvider.GOOGLE_GEMINI
            VisionSource.CUSTOM_CLOUD -> AiProvider.CUSTOM
        }
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_VISION_SOURCE] = source.name
            preferences[PreferencesKeys.AI_PROVIDER] = mappedProvider.id
        }
    }

    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEALTH_CONNECT_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setDailyBudget(calories: Double, protein: Double, carbs: Double, fat: Double) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_CALORIES] = calories
            preferences[PreferencesKeys.TARGET_PROTEIN] = protein
            preferences[PreferencesKeys.TARGET_CARBS] = carbs
            preferences[PreferencesKeys.TARGET_FAT] = fat
        }
    }

    suspend fun updateMealTimeWindows(windows: MealTimeWindows) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAKFAST_START_MINUTE] = windows.breakfastStart.toMinuteOfDay()
            preferences[PreferencesKeys.BREAKFAST_END_MINUTE] = windows.breakfastEnd.toMinuteOfDay()
            preferences[PreferencesKeys.LUNCH_START_MINUTE] = windows.lunchStart.toMinuteOfDay()
            preferences[PreferencesKeys.LUNCH_END_MINUTE] = windows.lunchEnd.toMinuteOfDay()
            preferences[PreferencesKeys.DINNER_START_MINUTE] = windows.dinnerStart.toMinuteOfDay()
            preferences[PreferencesKeys.DINNER_END_MINUTE] = windows.dinnerEnd.toMinuteOfDay()
        }
    }

    fun getLastBackupTimestamp(): Flow<Long?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP]
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setAutoBackupFolder(uri: String?, folderName: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[PreferencesKeys.AUTO_BACKUP_FOLDER_URI] = uri
            } else {
                preferences.remove(PreferencesKeys.AUTO_BACKUP_FOLDER_URI)
            }
            if (folderName != null) {
                preferences[PreferencesKeys.AUTO_BACKUP_FOLDER_NAME] = folderName
            } else {
                preferences.remove(PreferencesKeys.AUTO_BACKUP_FOLDER_NAME)
            }
        }
    }

    suspend fun setLastAutoBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_AUTO_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun restorePreferences(
        targetCalories: Double,
        targetProtein: Double,
        targetCarbs: Double,
        targetFat: Double,
        windows: MealTimeWindows
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_CALORIES] = targetCalories
            preferences[PreferencesKeys.TARGET_PROTEIN] = targetProtein
            preferences[PreferencesKeys.TARGET_CARBS] = targetCarbs
            preferences[PreferencesKeys.TARGET_FAT] = targetFat
            preferences[PreferencesKeys.BREAKFAST_START_MINUTE] = windows.breakfastStart.toMinuteOfDay()
            preferences[PreferencesKeys.BREAKFAST_END_MINUTE] = windows.breakfastEnd.toMinuteOfDay()
            preferences[PreferencesKeys.LUNCH_START_MINUTE] = windows.lunchStart.toMinuteOfDay()
            preferences[PreferencesKeys.LUNCH_END_MINUTE] = windows.lunchEnd.toMinuteOfDay()
            preferences[PreferencesKeys.DINNER_START_MINUTE] = windows.dinnerStart.toMinuteOfDay()
            preferences[PreferencesKeys.DINNER_END_MINUTE] = windows.dinnerEnd.toMinuteOfDay()
        }
    }
}

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
private fun minuteToLocalTime(minutes: Int): LocalTime =
    LocalTime.of((minutes / 60).coerceIn(0, 23), (minutes % 60).coerceIn(0, 59))
