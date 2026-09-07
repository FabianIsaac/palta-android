package com.calculadoracalorias.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import com.calculadoracalorias.app.data.local.AppDatabase
import com.calculadoracalorias.app.data.local.LocalFoodCatalogRepository
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.data.repository.AndroidHealthConnectRepository
import com.calculadoracalorias.app.data.repository.RoomMealRepository
import com.calculadoracalorias.app.data.vision.FoodVisionAnalyzerFactory
import com.calculadoracalorias.app.data.vision.LocalLiteRtVisionAnalyzer
import com.calculadoracalorias.app.data.vision.MiniMaxVisionAnalyzer
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.usecase.AnalyzeFoodImageUseCase
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import com.calculadoracalorias.app.presentation.scan.FoodCameraScreen
import com.calculadoracalorias.app.presentation.scan.FoodScanReviewScreen
import com.calculadoracalorias.app.presentation.scan.FoodScanReviewViewModel
import com.calculadoracalorias.app.presentation.settings.SettingsScreen
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val ComponentActivity.dataStore by preferencesDataStore(name = "user_settings")

enum class AppDestination {
    CAMERA,
    REVIEW,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val mealRepository = RoomMealRepository(database.mealDao())
        val healthConnectRepository = AndroidHealthConnectRepository(this)
        val userPreferencesRepository = UserPreferencesRepository(dataStore)

        val foodCatalog = LocalFoodCatalogRepository()
        val localAnalyzer = LocalLiteRtVisionAnalyzer(foodCatalog)

        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        val miniMaxAnalyzer = MiniMaxVisionAnalyzer(
            apiKeyProvider = { "" },
            httpClient = httpClient
        )

        val analyzerFactory = FoodVisionAnalyzerFactory(localAnalyzer, miniMaxAnalyzer)
        val recalculatePortionUseCase = RecalculatePortionUseCase()
        val saveMealWithHealthSyncUseCase = SaveMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)

        val reviewViewModel = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase
        )

        setContent {
            CalculadoraCaloriasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        reviewViewModel = reviewViewModel,
                        analyzerFactory = analyzerFactory,
                        userPreferencesRepository = userPreferencesRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    reviewViewModel: FoodScanReviewViewModel,
    analyzerFactory: FoodVisionAnalyzerFactory,
    userPreferencesRepository: UserPreferencesRepository
) {
    var currentScreen by remember { mutableStateOf(AppDestination.CAMERA) }
    val uiState by reviewViewModel.uiState.collectAsState()
    val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.calculadoracalorias.app.data.preferences.UserPreferences()
    )
    val coroutineScope = rememberCoroutineScope()

    when (currentScreen) {
        AppDestination.CAMERA -> {
            FoodCameraScreen(
                onImageCaptured = { imageBytes ->
                    coroutineScope.launch {
                        val analyzer = analyzerFactory.getAnalyzer(
                            preferredSource = userPreferences.preferredVisionSource,
                            isOnline = true,
                            hasApiKey = userPreferences.miniMaxApiKey.isNotBlank()
                        )
                        val analyzeUseCase = AnalyzeFoodImageUseCase(analyzer)
                        val result = analyzeUseCase(imageBytes)

                        result.onSuccess { mealResult ->
                            reviewViewModel.initializeWithResult(mealResult)
                            currentScreen = AppDestination.REVIEW
                        }.onFailure {
                            // En caso de error, inicializa review con lista vacía y error
                            reviewViewModel.initializeWithResult(
                                DetectedMealResult(
                                    items = emptyList(),
                                    suggestedMealType = com.calculadoracalorias.app.domain.model.MealCategory.ALMUERZO,
                                    analysisSource = userPreferences.preferredVisionSource
                                )
                            )
                            currentScreen = AppDestination.REVIEW
                        }
                    }
                }
            )
        }

        AppDestination.REVIEW -> {
            FoodScanReviewScreen(
                uiState = uiState,
                onEvent = reviewViewModel::onEvent,
                onNavigateBack = { currentScreen = AppDestination.CAMERA },
                onSavedSuccessfully = { currentScreen = AppDestination.CAMERA }
            )
        }

        AppDestination.SETTINGS -> {
            SettingsScreen(
                currentApiKey = userPreferences.miniMaxApiKey,
                currentVisionSource = userPreferences.preferredVisionSource,
                currentHealthSyncEnabled = userPreferences.healthConnectSyncEnabled,
                onSaveSettings = { apiKey, source, sync ->
                    coroutineScope.launch {
                        userPreferencesRepository.setMiniMaxApiKey(apiKey)
                        userPreferencesRepository.setPreferredVisionSource(source)
                        userPreferencesRepository.setHealthConnectSyncEnabled(sync)
                    }
                },
                onNavigateBack = { currentScreen = AppDestination.CAMERA }
            )
        }
    }
}
