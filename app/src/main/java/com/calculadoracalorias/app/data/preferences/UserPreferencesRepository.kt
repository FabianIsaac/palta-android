package com.calculadoracalorias.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calculadoracalorias.app.domain.model.VisionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserPreferences(
    val miniMaxApiKey: String = "",
    val preferredVisionSource: VisionSource = VisionSource.LOCAL_DEVICE,
    val healthConnectSyncEnabled: Boolean = true
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val MINIMAX_API_KEY = stringPreferencesKey("minimax_api_key")
        val PREFERRED_VISION_SOURCE = stringPreferencesKey("preferred_vision_source")
        val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        val apiKey = preferences[PreferencesKeys.MINIMAX_API_KEY] ?: ""
        val sourceStr = preferences[PreferencesKeys.PREFERRED_VISION_SOURCE] ?: VisionSource.LOCAL_DEVICE.name
        val preferredSource = try {
            VisionSource.valueOf(sourceStr)
        } catch (_: Exception) {
            VisionSource.LOCAL_DEVICE
        }
        val syncEnabled = preferences[PreferencesKeys.HEALTH_CONNECT_SYNC_ENABLED] ?: true

        UserPreferences(
            miniMaxApiKey = apiKey,
            preferredVisionSource = preferredSource,
            healthConnectSyncEnabled = syncEnabled
        )
    }

    suspend fun getMiniMaxApiKey(): String {
        return userPreferencesFlow.first().miniMaxApiKey
    }

    suspend fun setMiniMaxApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINIMAX_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setPreferredVisionSource(source: VisionSource) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_VISION_SOURCE] = source.name
        }
    }

    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEALTH_CONNECT_SYNC_ENABLED] = enabled
        }
    }
}
