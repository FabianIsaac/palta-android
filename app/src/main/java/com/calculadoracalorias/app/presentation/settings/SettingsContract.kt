package com.calculadoracalorias.app.presentation.settings

import com.calculadoracalorias.app.domain.model.AiCallLogEntry
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.AiTechnicalDetails
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload

data class BackupPreviewInfo(
    val mealCount: Int,
    val supplementCount: Int,
    val exportedAt: Long,
    val payload: BackupDataPayload
)

data class SettingsUiState(
    val aiProvider: AiProvider = AiProvider.NVIDIA_NIM,
    val apiKey: String = "",
    val apiKeyNvidia: String = "",
    val apiKeyGemini: String = "",
    val apiKeyMiniMax: String = "",
    val apiKeyCustom: String = "",
    val customEndpointUrl: String = "",
    val customTextModel: String = "",
    val customVisionModel: String = "",
    val visionSource: VisionSource = VisionSource.LOCAL_DEVICE,
    val isTestingAiConnection: Boolean = false,
    val aiConnectionTestResult: AiTechnicalDetails? = null,
    val aiConnectionTestError: String? = null,
    val aiCallLogs: List<AiCallLogEntry> = emptyList(),
    val healthConnectSyncEnabled: Boolean = true,
    val healthConnectActivitySyncEnabled: Boolean = true,
    val includeBurnedCaloriesInBudget: Boolean = false,
    val healthConnectWeightSyncEnabled: Boolean = true,
    val latestHealthWeightKg: Double? = null,
    val latestHealthWeightTimestamp: Long? = null,
    val isSyncingWeight: Boolean = false,
    val targetCalories: Double = 2000.0,
    val targetProteinGrams: Double = 150.0,
    val targetCarbsGrams: Double = 200.0,
    val targetFatGrams: Double = 65.0,
    val mealTimeWindows: MealTimeWindows = MealTimeWindows(),
    val isHealthConnectAvailable: Boolean = true,
    val hasHealthConnectPermission: Boolean = false,
    val hasActivityPermissions: Boolean = false,
    val hasWeightPermission: Boolean = false,
    val lastBackupTimestamp: Long? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val autoBackupFolderName: String? = null,
    val lastAutoBackupTimestamp: Long? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSyncingDrive: Boolean = false,
    val pendingRestorePreview: BackupPreviewInfo? = null,
    val userMessage: String? = null,
    val errorMessage: String? = null
)
