package com.calculadoracalorias.app.presentation.settings

import android.net.Uri
import com.calculadoracalorias.app.data.preferences.UserPreferences
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.data.worker.AutoBackupScheduler
import com.calculadoracalorias.app.domain.model.backup.BackupDataPayload
import com.calculadoracalorias.app.domain.model.backup.BackupMealEntry
import com.calculadoracalorias.app.domain.model.backup.BackupPreferences
import com.calculadoracalorias.app.domain.model.backup.BackupSupplement
import com.calculadoracalorias.app.domain.usecase.backup.ExportBackupUseCase
import com.calculadoracalorias.app.domain.usecase.backup.ImportBackupUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val exportBackupUseCase: ExportBackupUseCase = mockk(relaxed = true)
    private val importBackupUseCase: ImportBackupUseCase = mockk(relaxed = true)
    private val autoBackupScheduler: AutoBackupScheduler = mockk(relaxed = true)
    private val mockUri: Uri = mockk(relaxed = true)

    private val samplePayload = BackupDataPayload(
        version = 1,
        exportedAt = 1788775200000L,
        appVersionName = "1.1",
        meals = listOf(
            BackupMealEntry(
                id = 1L,
                category = "DESAYUNO",
                timestamp = 1788775200000L,
                totalCalories = 300.0,
                totalProteinGrams = 10.0,
                totalCarbsGrams = 40.0,
                totalFatGrams = 8.0,
                items = emptyList()
            )
        ),
        supplements = listOf(
            BackupSupplement(
                id = "omega3",
                name = "Omega 3",
                dosageDescription = "1 cápsula",
                calories = 10.0,
                proteinGrams = 0.0,
                carbsGrams = 0.0,
                fatGrams = 1.0
            )
        ),
        supplementLogs = emptyList(),
        preferences = BackupPreferences(
            targetCalories = 2000.0,
            targetProteinGrams = 150.0,
            targetCarbsGrams = 200.0,
            targetFatGrams = 65.0
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                miniMaxApiKey = "test_key",
                targetCalories = 2000.0,
                lastBackupTimestamp = 1788770000000L
            )
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("onExportBackup debe escribir el JSON en el stream y mostrar mensaje de éxito")
    fun testExportBackupSuccess() = runTest(testDispatcher) {
        val outputStream = ByteArrayOutputStream()
        coEvery { exportBackupUseCase() } returns samplePayload

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            openOutputStream = { outputStream },
            openInputStream = { null }
        )
        advanceUntilIdle()

        viewModel.onExportBackup(mockUri)
        advanceUntilIdle()

        val writtenJson = outputStream.toString(Charsets.UTF_8.name())
        assertNotNull(writtenJson)
        val parsed = BackupDataPayload.fromJson(writtenJson)
        assertEquals(samplePayload.version, parsed.version)
        assertEquals(1, parsed.meals.size)
        assertEquals(1, parsed.supplements.size)
        assertEquals("Copia de seguridad guardada con éxito", viewModel.uiState.value.userMessage)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    @DisplayName("onSelectBackupFile debe parsear el payload y desplegar previsualización")
    fun testSelectBackupFileSuccess() = runTest(testDispatcher) {
        val json = BackupDataPayload.toJson(samplePayload)
        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            openOutputStream = { null },
            openInputStream = { inputStream }
        )
        advanceUntilIdle()

        viewModel.onSelectBackupFile(mockUri)
        advanceUntilIdle()

        val preview = viewModel.uiState.value.pendingRestorePreview
        assertNotNull(preview)
        assertEquals(1, preview?.mealCount)
        assertEquals(1, preview?.supplementCount)
        assertEquals(samplePayload.exportedAt, preview?.exportedAt)
    }

    @Test
    @DisplayName("onSelectBackupFile con JSON inválido debe mostrar mensaje de error")
    fun testSelectBackupFileCorrupt() = runTest(testDispatcher) {
        val corruptedJson = "{ not a valid json"
        val inputStream = ByteArrayInputStream(corruptedJson.toByteArray(Charsets.UTF_8))

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            openOutputStream = { null },
            openInputStream = { inputStream }
        )
        advanceUntilIdle()

        viewModel.onSelectBackupFile(mockUri)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRestorePreview)
        assertEquals("El archivo seleccionado no es una copia de seguridad válida", viewModel.uiState.value.errorMessage)
    }

    @Test
    @DisplayName("onConfirmRestore debe ejecutar ImportBackupUseCase y notificar al usuario")
    fun testConfirmRestoreSuccess() = runTest(testDispatcher) {
        val json = BackupDataPayload.toJson(samplePayload)
        val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            openOutputStream = { null },
            openInputStream = { inputStream }
        )
        advanceUntilIdle()

        viewModel.onSelectBackupFile(mockUri)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingRestorePreview)

        viewModel.onConfirmRestore()
        advanceUntilIdle()

        coVerify(exactly = 1) { importBackupUseCase(samplePayload) }
        assertNull(viewModel.uiState.value.pendingRestorePreview)
        assertEquals("Datos restaurados exitosamente", viewModel.uiState.value.userMessage)
    }

    @Test
    @DisplayName("onSelectAutoBackupFolder persiste permiso, guarda carpeta, programa respaldo y notifica")
    fun testSelectAutoBackupFolderSuccess() = runTest(testDispatcher) {
        var permissionTakenUri: Uri? = null
        val folderUri = mockUri
        val folderName = "Palta en Drive"

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            takePersistableUriPermission = { permissionTakenUri = it },
            autoBackupScheduler = autoBackupScheduler
        )
        advanceUntilIdle()

        viewModel.onSelectAutoBackupFolder(folderUri, folderName)
        advanceUntilIdle()

        assertEquals(folderUri, permissionTakenUri)
        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupFolder(folderUri.toString(), folderName) }
        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupEnabled(true) }
        verify(exactly = 1) { autoBackupScheduler.scheduleDailyBackup() }
        verify(exactly = 1) { autoBackupScheduler.triggerImmediateBackup() }
        assertEquals("Carpeta vinculada para respaldo automático", viewModel.uiState.value.userMessage)
    }

    @Test
    @DisplayName("onToggleAutoBackup sin carpeta configurada muestra error")
    fun testToggleAutoBackupWithoutFolderShowsError() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            autoBackupScheduler = autoBackupScheduler
        )
        advanceUntilIdle()

        viewModel.onToggleAutoBackup(true)
        advanceUntilIdle()

        assertEquals("Debes vincular una carpeta de Google Drive primero", viewModel.uiState.value.errorMessage)
        coVerify(exactly = 0) { userPreferencesRepository.setAutoBackupEnabled(true) }
    }

    @Test
    @DisplayName("onToggleAutoBackup con carpeta configurada activa y cancela correctamente")
    fun testToggleAutoBackupEnableAndDisable() = runTest(testDispatcher) {
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                autoBackupEnabled = false,
                autoBackupFolderUri = "content://mock/folder",
                autoBackupFolderName = "Carpeta Mock"
            )
        )

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            autoBackupScheduler = autoBackupScheduler
        )
        advanceUntilIdle()

        viewModel.onToggleAutoBackup(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupEnabled(true) }
        verify(exactly = 1) { autoBackupScheduler.scheduleDailyBackup() }
        assertEquals("Respaldo automático activado", viewModel.uiState.value.userMessage)

        viewModel.onToggleAutoBackup(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupEnabled(false) }
        verify(exactly = 1) { autoBackupScheduler.cancelAutoBackup() }
        assertEquals("Respaldo automático desactivado", viewModel.uiState.value.userMessage)
    }

    @Test
    @DisplayName("onUnlinkAutoBackupFolder libera permisos y limpia preferencias")
    fun testUnlinkAutoBackupFolder() = runTest(testDispatcher) {
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                autoBackupEnabled = true,
                autoBackupFolderUri = "content://mock/folder",
                autoBackupFolderName = "Carpeta Mock"
            )
        )
        var releasedUri: Uri? = null

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            releasePersistableUriPermission = { releasedUri = it },
            autoBackupScheduler = autoBackupScheduler
        )
        advanceUntilIdle()

        viewModel.onUnlinkAutoBackupFolder()
        advanceUntilIdle()

        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupFolder(null, null) }
        coVerify(exactly = 1) { userPreferencesRepository.setAutoBackupEnabled(false) }
        verify(exactly = 1) { autoBackupScheduler.cancelAutoBackup() }
        assertEquals("Carpeta de respaldo desvinculada", viewModel.uiState.value.userMessage)
    }

    @Test
    @DisplayName("Debe reflejar reactivamente el proveedor de IA y credenciales independientes en uiState")
    fun testAiProviderStateReaction() = runTest(testDispatcher) {
        every { userPreferencesRepository.userPreferencesFlow } returns flowOf(
            UserPreferences(
                aiProvider = com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM,
                apiKeyNvidia = "nvapi-val",
                apiKeyGemini = "gemini-val",
                apiKeyMiniMax = "mm-val"
            )
        )

        val viewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase
        )
        advanceUntilIdle()

        assertEquals(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM, viewModel.uiState.value.aiProvider)
        assertEquals("nvapi-val", viewModel.uiState.value.apiKey)
        assertEquals("nvapi-val", viewModel.uiState.value.apiKeyNvidia)
        assertEquals("gemini-val", viewModel.uiState.value.apiKeyGemini)
        assertEquals("mm-val", viewModel.uiState.value.apiKeyMiniMax)

        viewModel.onSelectAiProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI)
        advanceUntilIdle()
        coVerify(exactly = 1) { userPreferencesRepository.setAiProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI) }

        viewModel.onSaveApiKeyForProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI, "new-gemini-key")
        advanceUntilIdle()
        coVerify(exactly = 1) { userPreferencesRepository.setApiKeyForProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI, "new-gemini-key") }

        viewModel.onSaveCustomAiParameters("http://localhost:11434/v1/chat/completions", "model-text", "model-vision")
        advanceUntilIdle()
        coVerify(exactly = 1) {
            userPreferencesRepository.setCustomAiParameters(
                "http://localhost:11434/v1/chat/completions",
                "model-text",
                "model-vision"
            )
        }
    }
}

