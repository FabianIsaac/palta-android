package com.calculadoracalorias.app.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.domain.model.AiConfiguration
import com.calculadoracalorias.app.domain.model.AiServiceException
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.usecase.backup.ExportBackupUseCase
import com.calculadoracalorias.app.domain.usecase.backup.ImportBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val openOutputStream: (Uri) -> OutputStream? = { null },
    private val openInputStream: (Uri) -> InputStream? = { null },
    private val takePersistableUriPermission: (Uri) -> Unit = {},
    private val releasePersistableUriPermission: (Uri) -> Unit = {},
    private val autoBackupScheduler: com.calculadoracalorias.app.data.worker.AutoBackupScheduler? = null,
    private val getLatestHealthWeightUseCase: com.calculadoracalorias.app.domain.usecase.GetLatestHealthWeightUseCase? = null,
    private val mealAnalyzer: com.calculadoracalorias.app.data.remote.OpenAiCompatibleMealAnalyzer? = null,
    private val debugLogManager: com.calculadoracalorias.app.data.remote.AiDebugLogManager = com.calculadoracalorias.app.data.remote.AiDebugLogManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        aiProvider = prefs.aiProvider,
                        apiKey = prefs.activeApiKey,
                        apiKeyNvidia = prefs.apiKeyNvidia,
                        apiKeyGemini = prefs.apiKeyGemini,
                        apiKeyMiniMax = prefs.effectiveMiniMaxApiKey,
                        apiKeyCustom = prefs.apiKeyCustom,
                        customEndpointUrl = prefs.customEndpointUrl,
                        customTextModel = prefs.customTextModel,
                        customVisionModel = prefs.customVisionModel,
                        visionSource = prefs.preferredVisionSource,
                        healthConnectSyncEnabled = prefs.healthConnectSyncEnabled,
                        healthConnectActivitySyncEnabled = prefs.healthConnectActivitySyncEnabled,
                        includeBurnedCaloriesInBudget = prefs.includeBurnedCaloriesInBudget,
                        healthConnectWeightSyncEnabled = prefs.healthConnectWeightSyncEnabled,
                        latestHealthWeightKg = prefs.lastSyncedWeightKg,
                        latestHealthWeightTimestamp = prefs.lastSyncedWeightTimestamp,
                        targetCalories = prefs.targetCalories,
                        targetProteinGrams = prefs.targetProteinGrams,
                        targetCarbsGrams = prefs.targetCarbsGrams,
                        targetFatGrams = prefs.targetFatGrams,
                        mealTimeWindows = prefs.mealTimeWindows,
                        lastBackupTimestamp = prefs.lastBackupTimestamp,
                        autoBackupEnabled = prefs.autoBackupEnabled,
                        autoBackupFolderUri = prefs.autoBackupFolderUri,
                        autoBackupFolderName = prefs.autoBackupFolderName,
                        lastAutoBackupTimestamp = prefs.lastAutoBackupTimestamp
                    )
                }
            }
        }

        viewModelScope.launch {
            debugLogManager.logs.collect { logs ->
                _uiState.update { it.copy(aiCallLogs = logs) }
            }
        }
    }

    fun updateHealthConnectAvailability(
        isAvailable: Boolean,
        hasPermission: Boolean,
        hasActivityPermissions: Boolean = false,
        hasWeightPermission: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                isHealthConnectAvailable = isAvailable,
                hasHealthConnectPermission = hasPermission,
                hasActivityPermissions = hasActivityPermissions,
                hasWeightPermission = hasWeightPermission
            )
        }
    }

    fun onToggleHealthConnectActivitySync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectActivitySyncEnabled(enabled)
            val msg = if (enabled) "Sincronización de actividad activada" else "Sincronización de actividad desactivada"
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun onToggleIncludeBurnedCalories(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIncludeBurnedCaloriesInBudget(enabled)
            val msg = if (enabled) {
                "Las calorías quemadas se sumarán a tu presupuesto diario"
            } else {
                "Presupuesto enfocado únicamente en la ingesta calórica"
            }
            _uiState.update { it.copy(userMessage = msg) }
        }
    }

    fun onToggleHealthConnectWeightSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectWeightSyncEnabled(enabled)
        }
    }

    fun onSyncWeightFromHealthConnect() {
        val useCase = getLatestHealthWeightUseCase ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingWeight = true, errorMessage = null) }
            val result = useCase()
            result.onSuccess { record ->
                if (record != null) {
                    val epochMilli = record.recordedAt.toEpochMilli()
                    userPreferencesRepository.setLastSyncedWeight(record.weightKg, epochMilli)
                    _uiState.update {
                        it.copy(
                            isSyncingWeight = false,
                            latestHealthWeightKg = record.weightKg,
                            latestHealthWeightTimestamp = epochMilli,
                            userMessage = "Peso actualizado desde Health Connect: ${record.weightKg} kg"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncingWeight = false,
                            userMessage = "No se encontraron pesajes recientes en Health Connect."
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSyncingWeight = false,
                        errorMessage = error.localizedMessage ?: "Error al consultar peso en Health Connect."
                    )
                }
            }
        }
    }

    fun onExportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            try {
                val payload = exportBackupUseCase()
                val jsonString = BackupDataPayload.toJson(payload)
                val outputStream = openOutputStream(uri)
                    ?: throw IllegalStateException("No se pudo abrir el destino para escribir el respaldo")

                outputStream.use { stream ->
                    stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastBackupTimestamp = payload.exportedAt,
                        userMessage = "Copia de seguridad guardada con éxito"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Error al exportar respaldo: ${e.localizedMessage ?: "desconocido"}"
                    )
                }
            }
        }
    }

    fun onSelectBackupFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            try {
                val inputStream = openInputStream(uri)
                    ?: throw IllegalStateException("No se pudo abrir el archivo seleccionado")

                val jsonString = inputStream.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }

                val payload = BackupDataPayload.fromJson(jsonString)
                val preview = BackupPreviewInfo(
                    mealCount = payload.meals.size,
                    supplementCount = payload.supplements.size,
                    exportedAt = payload.exportedAt,
                    payload = payload
                )

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        pendingRestorePreview = preview
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "El archivo seleccionado no es una copia de seguridad válida"
                    )
                }
            }
        }
    }

    fun onConfirmRestore() {
        val preview = _uiState.value.pendingRestorePreview ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, pendingRestorePreview = null, errorMessage = null) }
            try {
                importBackupUseCase(preview.payload)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        userMessage = "Datos restaurados exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Error al restaurar datos: ${e.localizedMessage ?: "desconocido"}"
                    )
                }
            }
        }
    }

    fun onDismissRestoreDialog() {
        _uiState.update { it.copy(pendingRestorePreview = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onSelectAiProvider(provider: com.calculadoracalorias.app.domain.model.AiProvider) {
        viewModelScope.launch {
            userPreferencesRepository.setAiProvider(provider)
        }
    }

    fun onSaveApiKeyForProvider(provider: com.calculadoracalorias.app.domain.model.AiProvider, key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setApiKeyForProvider(provider, key)
        }
    }

    fun onSaveCustomAiParameters(endpointUrl: String, textModel: String, visionModel: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCustomAiParameters(endpointUrl, textModel, visionModel)
        }
    }

    fun onSaveApiKey(key: String) {
        viewModelScope.launch {
            userPreferencesRepository.setApiKeyForProvider(_uiState.value.aiProvider, key)
        }
    }

    fun onSaveVisionSource(source: VisionSource) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredVisionSource(source)
        }
    }

    fun onSaveHealthSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectSyncEnabled(enabled)
        }
    }

    fun onSaveDailyBudget(calories: Double, protein: Double, carbs: Double, fat: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyBudget(calories, protein, carbs, fat)
        }
    }

    fun onSaveMealTimeWindows(windows: MealTimeWindows) {
        viewModelScope.launch {
            userPreferencesRepository.updateMealTimeWindows(windows)
        }
    }

    fun onSelectAutoBackupFolder(uri: Uri, folderName: String? = null) {
        viewModelScope.launch {
            try {
                takePersistableUriPermission(uri)
                val displayName = folderName ?: uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Google Drive / Palta"
                userPreferencesRepository.setAutoBackupFolder(uri.toString(), displayName)
                userPreferencesRepository.setAutoBackupEnabled(true)
                autoBackupScheduler?.scheduleDailyBackup()
                autoBackupScheduler?.triggerImmediateBackup()
                _uiState.update {
                    it.copy(userMessage = "Carpeta vinculada y respaldo sincronizado con Google Drive")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error al vincular carpeta: ${e.localizedMessage ?: "desconocido"}")
                }
            }
        }
    }

    fun onToggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val currentUri = _uiState.value.autoBackupFolderUri
                if (currentUri.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(errorMessage = "Debes vincular una carpeta de Google Drive primero")
                    }
                    return@launch
                }
                userPreferencesRepository.setAutoBackupEnabled(true)
                autoBackupScheduler?.scheduleDailyBackup()
                autoBackupScheduler?.triggerImmediateBackup()
                _uiState.update {
                    it.copy(userMessage = "Respaldo automático activado")
                }
            } else {
                userPreferencesRepository.setAutoBackupEnabled(false)
                autoBackupScheduler?.cancelAutoBackup()
                _uiState.update {
                    it.copy(userMessage = "Respaldo automático desactivado")
                }
            }
        }
    }

    fun onSyncDriveNow() {
        val currentUri = _uiState.value.autoBackupFolderUri
        if (currentUri.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Debes vincular una carpeta de Google Drive primero")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingDrive = true, errorMessage = null) }
            try {
                autoBackupScheduler?.triggerImmediateBackup()
                kotlinx.coroutines.delay(1200)
                _uiState.update {
                    it.copy(
                        isSyncingDrive = false,
                        userMessage = "Respaldo sincronizado con Google Drive exitosamente"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncingDrive = false,
                        errorMessage = "Error al sincronizar con Google Drive: ${e.localizedMessage ?: "desconocido"}"
                    )
                }
            }
        }
    }

    fun onUnlinkAutoBackupFolder() {
        viewModelScope.launch {
            val currentUri = _uiState.value.autoBackupFolderUri
            if (!currentUri.isNullOrBlank()) {
                try {
                    releasePersistableUriPermission(Uri.parse(currentUri))
                } catch (_: Exception) { }
            }
            userPreferencesRepository.setAutoBackupFolder(null, null)
            userPreferencesRepository.setAutoBackupEnabled(false)
            autoBackupScheduler?.cancelAutoBackup()
            _uiState.update {
                it.copy(userMessage = "Carpeta de respaldo desvinculada")
            }
        }
    }

    fun testAiConnectivity(analyzerOverride: com.calculadoracalorias.app.data.remote.OpenAiCompatibleMealAnalyzer? = null) {
        val analyzer = analyzerOverride ?: mealAnalyzer
        if (analyzer == null) {
            _uiState.update {
                it.copy(
                    aiConnectionTestError = "Analizador de IA no configurado.",
                    isTestingAiConnection = false
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isTestingAiConnection = true,
                aiConnectionTestResult = null,
                aiConnectionTestError = null
            )
        }

        viewModelScope.launch {
            val currentConfig = AiConfiguration(
                provider = _uiState.value.aiProvider,
                apiKey = _uiState.value.apiKey,
                customEndpointUrl = _uiState.value.customEndpointUrl,
                customTextModel = _uiState.value.customTextModel,
                customVisionModel = _uiState.value.customVisionModel
            )

            val result = analyzer.testConnectivity(currentConfig)
            result.fold(
                onSuccess = { details ->
                    _uiState.update {
                        it.copy(
                            isTestingAiConnection = false,
                            aiConnectionTestResult = details,
                            aiConnectionTestError = null
                        )
                    }
                },
                onFailure = { error ->
                    val details = (error as? AiServiceException)?.technicalDetails
                    _uiState.update {
                        it.copy(
                            isTestingAiConnection = false,
                            aiConnectionTestResult = details,
                            aiConnectionTestError = error.message ?: "Fallo al conectar con el proveedor."
                        )
                    }
                }
            )
        }
    }

    fun clearAiLogs() {
        debugLogManager.clear()
    }

    fun dismissAiConnectionTestResult() {
        _uiState.update {
            it.copy(
                aiConnectionTestResult = null,
                aiConnectionTestError = null
            )
        }
    }
}
