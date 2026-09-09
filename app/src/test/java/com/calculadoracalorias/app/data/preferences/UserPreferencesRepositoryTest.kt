package com.calculadoracalorias.app.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @field:TempDir
    lateinit var tempFolder: File

    @Test
    fun `userPreferencesFlow emits default meal time windows and updates reactively`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder, "test_preferences.preferences_pb") }
        )

        val repository = UserPreferencesRepository(testDataStore)

        repository.userPreferencesFlow.test {
            val initial = awaitItem()
            assertEquals(LocalTime.of(6, 0), initial.mealTimeWindows.breakfastStart)
            assertEquals(LocalTime.of(11, 30), initial.mealTimeWindows.breakfastEnd)
            assertEquals(LocalTime.of(11, 30), initial.mealTimeWindows.lunchStart)
            assertEquals(LocalTime.of(16, 0), initial.mealTimeWindows.lunchEnd)
            assertEquals(LocalTime.of(16, 0), initial.mealTimeWindows.dinnerStart)
            assertEquals(LocalTime.of(22, 0), initial.mealTimeWindows.dinnerEnd)

            val customWindows = MealTimeWindows(
                breakfastStart = LocalTime.of(7, 30),
                breakfastEnd = LocalTime.of(10, 0),
                lunchStart = LocalTime.of(12, 30),
                lunchEnd = LocalTime.of(15, 0),
                dinnerStart = LocalTime.of(19, 0),
                dinnerEnd = LocalTime.of(23, 0)
            )

            repository.updateMealTimeWindows(customWindows)

            val updated = awaitItem()
            assertEquals(LocalTime.of(7, 30), updated.mealTimeWindows.breakfastStart)
            assertEquals(LocalTime.of(10, 0), updated.mealTimeWindows.breakfastEnd)
            assertEquals(LocalTime.of(12, 30), updated.mealTimeWindows.lunchStart)
            assertEquals(LocalTime.of(15, 0), updated.mealTimeWindows.lunchEnd)
            assertEquals(LocalTime.of(19, 0), updated.mealTimeWindows.dinnerStart)
            assertEquals(LocalTime.of(23, 0), updated.mealTimeWindows.dinnerEnd)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDailyBudget updates calories and macros reactively in userPreferencesFlow and getDailyBudget`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder, "test_preferences_budget.preferences_pb") }
        )

        val repository = UserPreferencesRepository(testDataStore)

        repository.getDailyBudget().test {
            val initial = awaitItem()
            assertEquals(2000.0, initial.targetCalories)
            assertEquals(150.0, initial.targetProteinGrams)
            assertEquals(200.0, initial.targetCarbsGrams)
            assertEquals(65.0, initial.targetFatGrams)

            repository.setDailyBudget(
                calories = 2300.0,
                protein = 160.0,
                carbs = 220.0,
                fat = 70.0
            )

            val updated = awaitItem()
            assertEquals(2300.0, updated.targetCalories)
            assertEquals(160.0, updated.targetProteinGrams)
            assertEquals(220.0, updated.targetCarbsGrams)
            assertEquals(70.0, updated.targetFatGrams)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `auto backup preferences emit defaults and update reactively`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder, "test_preferences_autobackup.preferences_pb") }
        )

        val repository = UserPreferencesRepository(testDataStore)

        repository.userPreferencesFlow.test {
            val initial = awaitItem()
            assertFalse(initial.autoBackupEnabled)
            assertNull(initial.autoBackupFolderUri)
            assertNull(initial.autoBackupFolderName)
            assertNull(initial.lastAutoBackupTimestamp)

            repository.setAutoBackupEnabled(true)
            val enabledState = awaitItem()
            assertTrue(enabledState.autoBackupEnabled)

            repository.setAutoBackupFolder(
                uri = "content://com.google.android.apps.docs.storage/tree/root%2FPalta",
                folderName = "Palta (Google Drive)"
            )
            val folderState = awaitItem()
            assertEquals("content://com.google.android.apps.docs.storage/tree/root%2FPalta", folderState.autoBackupFolderUri)
            assertEquals("Palta (Google Drive)", folderState.autoBackupFolderName)

            val now = 1725800000000L
            repository.setLastAutoBackupTimestamp(now)
            val timestampState = awaitItem()
            assertEquals(now, timestampState.lastAutoBackupTimestamp)

            repository.setAutoBackupFolder(null, null)
            val clearedFolderState = awaitItem()
            assertNull(clearedFolderState.autoBackupFolderUri)
            assertNull(clearedFolderState.autoBackupFolderName)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ai provider preferences store and retrieve independent credentials per provider`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher + Job())
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder, "test_preferences_ai.preferences_pb") }
        )

        val repository = UserPreferencesRepository(testDataStore)

        repository.userPreferencesFlow.test {
            val initial = awaitItem()
            assertEquals(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM, initial.aiProvider)
            assertEquals("", initial.activeApiKey)

            // Guardar clave NVIDIA
            repository.setApiKeyForProvider(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM, "nvapi-12345")
            val nvidiaState = awaitItem()
            assertEquals("nvapi-12345", nvidiaState.apiKeyNvidia)
            assertEquals("nvapi-12345", nvidiaState.activeApiKey)
            assertEquals("meta/llama-3.3-70b-instruct", nvidiaState.aiConfiguration.effectiveTextModel)

            // Cambiar a Gemini y guardar clave
            repository.setAiProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI)
            val geminiProviderState = awaitItem()
            assertEquals(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI, geminiProviderState.aiProvider)
            assertEquals("", geminiProviderState.activeApiKey)

            repository.setApiKeyForProvider(com.calculadoracalorias.app.domain.model.AiProvider.GOOGLE_GEMINI, "AIzaSy-gemini")
            val geminiState = awaitItem()
            assertEquals("AIzaSy-gemini", geminiState.apiKeyGemini)
            assertEquals("AIzaSy-gemini", geminiState.activeApiKey)

            // Cambiar a Custom y configurar endpoint
            repository.setAiProvider(com.calculadoracalorias.app.domain.model.AiProvider.CUSTOM)
            awaitItem()
            repository.setApiKeyForProvider(com.calculadoracalorias.app.domain.model.AiProvider.CUSTOM, "sk-custom-ollama")
            awaitItem()
            repository.setCustomAiParameters(
                endpointUrl = "http://10.0.2.2:11434/v1/chat/completions",
                textModel = "qwen2.5:latest",
                visionModel = "llava:latest"
            )
            val customState = awaitItem()
            assertEquals("http://10.0.2.2:11434/v1/chat/completions", customState.aiConfiguration.effectiveEndpointUrl)
            assertEquals("qwen2.5:latest", customState.aiConfiguration.effectiveTextModel)
            assertEquals("llava:latest", customState.aiConfiguration.effectiveVisionModel)
            assertEquals("sk-custom-ollama", customState.aiConfiguration.apiKey)

            // Volver a NVIDIA y constatar que su clave se preservó intacta
            repository.setAiProvider(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM)
            val backToNvidia = awaitItem()
            assertEquals(com.calculadoracalorias.app.domain.model.AiProvider.NVIDIA_NIM, backToNvidia.aiProvider)
            assertEquals("nvapi-12345", backToNvidia.activeApiKey)
            assertEquals("nvapi-12345", backToNvidia.apiKeyNvidia)
            assertEquals("AIzaSy-gemini", backToNvidia.apiKeyGemini)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `legacy minimax key is transparently resolved when new key is not set`() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher + Job())
        val file = File(tempFolder, "test_preferences_legacy.preferences_pb")

        // Crear datastore inicial y guardar usando clave tradicional
        val initialDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file }
        )
        val initialRepo = UserPreferencesRepository(initialDataStore)
        initialRepo.setMiniMaxApiKey("legacy-minimax-secret")

        // Reabrir repositorio
        val loadedRepo = UserPreferencesRepository(initialDataStore)
        val prefs = loadedRepo.userPreferencesFlow.test {
            val item = awaitItem()
            assertEquals("legacy-minimax-secret", item.effectiveMiniMaxApiKey)
            assertEquals("legacy-minimax-secret", item.apiKeyMiniMax)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

