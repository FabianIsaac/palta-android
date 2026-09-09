package com.calculadoracalorias.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.calculadoracalorias.app.presentation.navigation.AppBottomNavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.preferencesDataStore
import androidx.health.connect.client.PermissionController
import com.calculadoracalorias.app.data.local.AppDatabase
import com.calculadoracalorias.app.data.local.LocalFoodCatalogRepository
import com.calculadoracalorias.app.data.preferences.UserPreferencesRepository
import com.calculadoracalorias.app.data.preferences.userDataStore
import com.calculadoracalorias.app.data.worker.AutoBackupScheduler
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import com.calculadoracalorias.app.data.repository.AndroidHealthConnectRepository
import com.calculadoracalorias.app.data.repository.RoomMealRepository
import com.calculadoracalorias.app.data.vision.FoodVisionAnalyzerFactory
import com.calculadoracalorias.app.data.vision.LocalLiteRtVisionAnalyzer
import com.calculadoracalorias.app.data.vision.MiniMaxVisionAnalyzer
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.VisionSource
import java.time.LocalDate
import java.time.LocalTime
import com.calculadoracalorias.app.domain.usecase.AnalyzeFoodImageUseCase
import com.calculadoracalorias.app.domain.usecase.DeleteMealWithHealthSyncUseCase
import com.calculadoracalorias.app.domain.usecase.GetDailyMealSummaryUseCase
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import com.calculadoracalorias.app.domain.usecase.UpdateMealWithHealthSyncUseCase
import com.calculadoracalorias.app.data.remote.RemoteSupplementNutritionAnalyzer
import com.calculadoracalorias.app.domain.usecase.EstimateSupplementNutritionUseCase
import com.calculadoracalorias.app.presentation.edit.EditMealScreen
import com.calculadoracalorias.app.presentation.edit.EditMealViewModel
import com.calculadoracalorias.app.presentation.scan.FoodCameraScreen
import com.calculadoracalorias.app.presentation.scan.FoodScanReviewScreen
import com.calculadoracalorias.app.presentation.scan.FoodScanReviewViewModel
import com.calculadoracalorias.app.domain.usecase.GetMonthlyCalendarHabitsUseCase
import com.calculadoracalorias.app.domain.usecase.GetPeriodStatisticsUseCase
import com.calculadoracalorias.app.presentation.calendar.MonthlyHabitsCalendarDialog
import com.calculadoracalorias.app.presentation.settings.SettingsScreen
import com.calculadoracalorias.app.presentation.statistics.StatisticsEvent
import com.calculadoracalorias.app.presentation.statistics.StatisticsScreen
import com.calculadoracalorias.app.presentation.statistics.StatisticsViewModel
import com.calculadoracalorias.app.presentation.summary.DailySummaryEvent
import com.calculadoracalorias.app.presentation.summary.DailySummaryScreen
import com.calculadoracalorias.app.presentation.summary.DailySummaryViewModel
import com.calculadoracalorias.app.presentation.supplements.SupplementsScreen
import com.calculadoracalorias.app.presentation.supplements.SupplementsViewModel
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import com.calculadoracalorias.app.data.repository.RoomBackupRepository
import com.calculadoracalorias.app.domain.usecase.backup.ExportBackupUseCase
import com.calculadoracalorias.app.domain.usecase.backup.ImportBackupUseCase
import com.calculadoracalorias.app.presentation.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class AppDestination {
    SUMMARY,
    STATISTICS,
    CAMERA,
    SUPPLEMENTS,
    SETTINGS,
    REVIEW,
    EDIT_MEAL
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        val mealRepository = RoomMealRepository(database.mealDao())
        val healthConnectRepository = AndroidHealthConnectRepository(this)
        val userPreferencesRepository = UserPreferencesRepository(userDataStore)
        val autoBackupScheduler = AutoBackupScheduler(this)

        lifecycleScope.launch {
            if (userPreferencesRepository.userPreferencesFlow.first().autoBackupEnabled) {
                autoBackupScheduler.scheduleDailyBackup()
            }
        }

        val foodCatalog = LocalFoodCatalogRepository()
        val localAnalyzer = LocalLiteRtVisionAnalyzer(foodCatalog)

        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
        }

        val cloudVisionAnalyzer = com.calculadoracalorias.app.data.vision.OpenAiCompatibleVisionAnalyzer(
            configProvider = {
                kotlinx.coroutines.runBlocking { userPreferencesRepository.getAiConfiguration() }
            },
            httpClient = httpClient
        )

        val naturalLanguageMealAnalyzer = com.calculadoracalorias.app.data.remote.OpenAiCompatibleMealAnalyzer(
            configProvider = {
                kotlinx.coroutines.runBlocking { userPreferencesRepository.getAiConfiguration() }
            },
            httpClient = httpClient,
            catalogRepository = foodCatalog
        )
        val parseNaturalLanguageMealUseCase = com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase(naturalLanguageMealAnalyzer)

        val analyzerFactory = FoodVisionAnalyzerFactory(localAnalyzer, cloudVisionAnalyzer)
        val recalculatePortionUseCase = RecalculatePortionUseCase()
        val saveMealWithHealthSyncUseCase = SaveMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
        val getDailyMealSummaryUseCase = GetDailyMealSummaryUseCase(mealRepository, userPreferencesRepository)
        val updateMealWithHealthSyncUseCase = UpdateMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
        val deleteMealWithHealthSyncUseCase = DeleteMealWithHealthSyncUseCase(mealRepository, healthConnectRepository)
        val refinePendingMealsUseCase = com.calculadoracalorias.app.domain.usecase.RefinePendingMealsUseCase(
            mealRepository = mealRepository,
            analyzer = naturalLanguageMealAnalyzer,
            healthConnectRepository = healthConnectRepository
        )

        val supplementRepository = com.calculadoracalorias.app.data.repository.LocalSupplementRepository(
            supplementDao = database.supplementDao(),
            supplementLogDao = database.supplementLogDao()
        )
        val calculateDailyStreakUseCase = com.calculadoracalorias.app.domain.usecase.CalculateDailyStreakUseCase(
            mealRepository = mealRepository
        )

        val dailySummaryViewModel = DailySummaryViewModel(
            getDailyMealSummaryUseCase = getDailyMealSummaryUseCase,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase,
            supplementRepository = supplementRepository,
            refinePendingMealsUseCase = refinePendingMealsUseCase
        )

        val getPeriodStatisticsUseCase = GetPeriodStatisticsUseCase(
            mealRepository = mealRepository,
            dailyBudgetRepository = userPreferencesRepository,
            supplementRepository = supplementRepository,
            calculateDailyStreakUseCase = calculateDailyStreakUseCase
        )
        val getMonthlyCalendarHabitsUseCase = GetMonthlyCalendarHabitsUseCase(
            mealRepository = mealRepository,
            dailyBudgetRepository = userPreferencesRepository,
            supplementRepository = supplementRepository
        )
        val statisticsViewModel = StatisticsViewModel(
            getPeriodStatisticsUseCase = getPeriodStatisticsUseCase,
            getMonthlyCalendarHabitsUseCase = getMonthlyCalendarHabitsUseCase
        )

        // Observador de conectividad para refinamiento automático en segundo plano
        try {
            val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            if (connectivityManager != null) {
                val networkRequest = android.net.NetworkRequest.Builder()
                    .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

                connectivityManager.registerNetworkCallback(
                    networkRequest,
                    object : android.net.ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: android.net.Network) {
                            dailySummaryViewModel.triggerPendingMealsRefinement()
                        }
                    }
                )
            }
        } catch (_: Exception) {
            // Ignorar excepciones en entornos sin permisos o pruebas
        }

        val editMealViewModel = EditMealViewModel(
            mealRepository = mealRepository,
            recalculatePortionUseCase = recalculatePortionUseCase,
            updateMealWithHealthSyncUseCase = updateMealWithHealthSyncUseCase,
            deleteMealWithHealthSyncUseCase = deleteMealWithHealthSyncUseCase,
            foodCatalogRepository = foodCatalog
        )

        val reviewViewModel = FoodScanReviewViewModel(
            recalculatePortionUseCase = recalculatePortionUseCase,
            saveMealWithHealthSyncUseCase = saveMealWithHealthSyncUseCase,
            parseNaturalLanguageMealUseCase = parseNaturalLanguageMealUseCase,
            mealRepository = mealRepository
        )

        val remoteSupplementAnalyzer = com.calculadoracalorias.app.data.remote.OpenAiCompatibleSupplementAnalyzer(
            configProvider = {
                kotlinx.coroutines.runBlocking { userPreferencesRepository.getAiConfiguration() }
            },
            httpClient = httpClient
        )
        val estimateSupplementNutritionUseCase = EstimateSupplementNutritionUseCase(
            remoteAnalyzer = remoteSupplementAnalyzer
        )
        val supplementsViewModel = SupplementsViewModel(
            supplementRepository = supplementRepository,
            estimateSupplementNutritionUseCase = estimateSupplementNutritionUseCase
        )

        val backupRepository = RoomBackupRepository(
            appDatabase = database,
            mealDao = database.mealDao(),
            supplementDao = database.supplementDao(),
            supplementLogDao = database.supplementLogDao(),
            userPreferencesRepository = userPreferencesRepository
        )
        val exportBackupUseCase = ExportBackupUseCase(backupRepository)
        val importBackupUseCase = ImportBackupUseCase(backupRepository)
        val settingsViewModel = SettingsViewModel(
            userPreferencesRepository = userPreferencesRepository,
            exportBackupUseCase = exportBackupUseCase,
            importBackupUseCase = importBackupUseCase,
            openOutputStream = { uri -> contentResolver.openOutputStream(uri) },
            openInputStream = { uri -> contentResolver.openInputStream(uri) },
            takePersistableUriPermission = { uri ->
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)
            },
            releasePersistableUriPermission = { uri ->
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.releasePersistableUriPermission(uri, flags)
            },
            autoBackupScheduler = autoBackupScheduler
        )

        setContent {
            CalculadoraCaloriasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        dailySummaryViewModel = dailySummaryViewModel,
                        editMealViewModel = editMealViewModel,
                        reviewViewModel = reviewViewModel,
                        supplementsViewModel = supplementsViewModel,
                        settingsViewModel = settingsViewModel,
                        statisticsViewModel = statisticsViewModel,
                        analyzerFactory = analyzerFactory,
                        userPreferencesRepository = userPreferencesRepository,
                        healthConnectRepository = healthConnectRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    dailySummaryViewModel: DailySummaryViewModel,
    editMealViewModel: EditMealViewModel,
    reviewViewModel: FoodScanReviewViewModel,
    supplementsViewModel: SupplementsViewModel,
    settingsViewModel: SettingsViewModel,
    statisticsViewModel: StatisticsViewModel,
    analyzerFactory: FoodVisionAnalyzerFactory,
    userPreferencesRepository: UserPreferencesRepository,
    healthConnectRepository: AndroidHealthConnectRepository
) {
    var currentScreen by remember { mutableStateOf(AppDestination.SUMMARY) }
    var pendingMealCategory by remember { mutableStateOf<MealCategory?>(null) }
    var pendingMealDate by remember { mutableStateOf<LocalDate?>(null) }

    val summaryUiState by dailySummaryViewModel.uiState.collectAsState()
    val editMealUiState by editMealViewModel.uiState.collectAsState()
    val reviewUiState by reviewViewModel.uiState.collectAsState()
    val supplementsUiState by supplementsViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val statisticsUiState by statisticsViewModel.uiState.collectAsState()
    val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.calculadoracalorias.app.data.preferences.UserPreferences()
    )
    val coroutineScope = rememberCoroutineScope()

    var isHealthAvailable by remember { mutableStateOf(false) }
    var hasHealthPermission by remember { mutableStateOf(false) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        hasHealthPermission = granted.containsAll(healthConnectRepository.requiredPermissions)
    }

    LaunchedEffect(Unit) {
        isHealthAvailable = healthConnectRepository.isHealthConnectAvailable()
        if (isHealthAvailable) {
            hasHealthPermission = healthConnectRepository.hasWriteNutritionPermission()
        }
    }

    val isBottomBarVisible = currentScreen == AppDestination.SUMMARY ||
            currentScreen == AppDestination.STATISTICS ||
            currentScreen == AppDestination.SUPPLEMENTS ||
            currentScreen == AppDestination.SETTINGS

    Box(modifier = Modifier.fillMaxSize()) {
        // Capa 1 (Fondo): Pantalla activa ocupando toda la superficie de la ventana
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppDestination.SUMMARY -> {
                    DailySummaryScreen(
                        uiState = summaryUiState,
                        onEvent = dailySummaryViewModel::onEvent,
                        onNavigateToCamera = { category ->
                            pendingMealCategory = category
                            pendingMealDate = summaryUiState.selectedDate
                            currentScreen = AppDestination.CAMERA
                        },
                        onNavigateToEditMeal = { mealId ->
                            editMealViewModel.loadMeal(mealId)
                            currentScreen = AppDestination.EDIT_MEAL
                        },
                        onNavigateToSettings = {
                            currentScreen = AppDestination.SETTINGS
                        },
                        onOpenCalendar = {
                            statisticsViewModel.onEvent(StatisticsEvent.OnOpenCalendarClicked(summaryUiState.selectedDate))
                        }
                    )
                }

                AppDestination.STATISTICS -> {
                    StatisticsScreen(
                        uiState = statisticsUiState,
                        onEvent = statisticsViewModel::onEvent,
                        onNavigateToDiaryDate = { date ->
                            dailySummaryViewModel.onEvent(DailySummaryEvent.OnDateSelected(date))
                            statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked)
                            currentScreen = AppDestination.SUMMARY
                        }
                    )
                }

                AppDestination.SUPPLEMENTS -> {
                    SupplementsScreen(
                        uiState = supplementsUiState,
                        onEvent = supplementsViewModel::onEvent,
                        onNavigateBack = { currentScreen = AppDestination.SUMMARY }
                    )
                }

                AppDestination.CAMERA -> {
                    val smartCategory = pendingMealCategory ?: userPreferences.mealTimeWindows.detectCategory(LocalTime.now())
                    val smartDate = pendingMealDate ?: summaryUiState.selectedDate
                    FoodCameraScreen(
                        onImageCaptured = { imageBytes ->
                            val analyzer = analyzerFactory.getAnalyzer(
                                preferredSource = userPreferences.preferredVisionSource,
                                isOnline = true,
                                hasApiKey = userPreferences.miniMaxApiKey.isNotBlank()
                            )
                            val analyzeUseCase = AnalyzeFoodImageUseCase(analyzer)
                            reviewViewModel.startImageAnalysis(
                                imageBytes = imageBytes,
                                category = smartCategory,
                                targetDate = smartDate
                            ) { bytes ->
                                analyzeUseCase(bytes)
                            }
                            currentScreen = AppDestination.REVIEW
                        },
                        isCloudAiActive = userPreferences.preferredVisionSource == VisionSource.MINIMAX_CLOUD && userPreferences.miniMaxApiKey.isNotBlank(),
                        onAnalyzeTextDescription = { description ->
                            reviewViewModel.onEvent(
                                com.calculadoracalorias.app.presentation.scan.FoodScanReviewEvent.OnAnalyzeNaturalLanguage(
                                    description = description,
                                    category = smartCategory,
                                    targetDate = smartDate
                                )
                            )
                            currentScreen = AppDestination.REVIEW
                        },
                        isAnalyzingText = reviewUiState.isAnalyzingText,
                        onNavigateToSettings = { currentScreen = AppDestination.SETTINGS },
                        onNavigateToManualEntry = {
                            reviewViewModel.initializeWithResult(
                                DetectedMealResult(
                                    items = emptyList(),
                                    suggestedMealType = smartCategory,
                                    analysisSource = userPreferences.preferredVisionSource
                                ),
                                targetDate = smartDate
                            )
                            currentScreen = AppDestination.REVIEW
                        },
                        onNavigateBack = { currentScreen = AppDestination.SUMMARY }
                    )
                }

                AppDestination.REVIEW -> {
                    val smartCategory = pendingMealCategory ?: userPreferences.mealTimeWindows.detectCategory(LocalTime.now())
                    FoodScanReviewScreen(
                        uiState = reviewUiState,
                        onEvent = reviewViewModel::onEvent,
                        suggestedCategory = smartCategory,
                        onNavigateBack = {
                            reviewViewModel.resetState()
                            pendingMealCategory = null
                            pendingMealDate = null
                            currentScreen = AppDestination.SUMMARY
                        },
                        onSavedSuccessfully = {
                            reviewViewModel.resetState()
                            pendingMealCategory = null
                            pendingMealDate = null
                            currentScreen = AppDestination.SUMMARY
                        }
                    )
                }

                AppDestination.EDIT_MEAL -> {
                    EditMealScreen(
                        uiState = editMealUiState,
                        onEvent = editMealViewModel::onEvent,
                        onNavigateBack = { currentScreen = AppDestination.SUMMARY },
                        onSearchCatalog = editMealViewModel::searchCatalog,
                        onGetPopularFoods = editMealViewModel::getPopularFoods
                    )
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        currentAiProvider = settingsUiState.aiProvider,
                        currentApiKey = settingsUiState.apiKey,
                        currentApiKeyNvidia = settingsUiState.apiKeyNvidia,
                        currentApiKeyGemini = settingsUiState.apiKeyGemini,
                        currentApiKeyMiniMax = settingsUiState.apiKeyMiniMax,
                        currentApiKeyCustom = settingsUiState.apiKeyCustom,
                        currentCustomEndpointUrl = settingsUiState.customEndpointUrl,
                        currentCustomTextModel = settingsUiState.customTextModel,
                        currentCustomVisionModel = settingsUiState.customVisionModel,
                        currentVisionSource = settingsUiState.visionSource,
                        currentHealthSyncEnabled = settingsUiState.healthConnectSyncEnabled,
                        currentMealTimeWindows = settingsUiState.mealTimeWindows,
                        currentTargetCalories = settingsUiState.targetCalories,
                        currentTargetProteinGrams = settingsUiState.targetProteinGrams,
                        currentTargetCarbsGrams = settingsUiState.targetCarbsGrams,
                        currentTargetFatGrams = settingsUiState.targetFatGrams,
                        isHealthConnectAvailable = isHealthAvailable,
                        hasHealthConnectPermission = hasHealthPermission,
                        onRequestHealthConnectPermission = {
                            healthPermissionLauncher.launch(healthConnectRepository.requiredPermissions)
                        },
                        onSelectAiProvider = settingsViewModel::onSelectAiProvider,
                        onSaveApiKeyForProvider = settingsViewModel::onSaveApiKeyForProvider,
                        onSaveCustomAiParameters = settingsViewModel::onSaveCustomAiParameters,
                        onSaveSettings = { apiKey, source, sync ->
                            settingsViewModel.onSaveApiKey(apiKey)
                            settingsViewModel.onSaveVisionSource(source)
                            settingsViewModel.onSaveHealthSyncEnabled(sync)
                        },
                        onSaveMealTimeWindows = settingsViewModel::onSaveMealTimeWindows,
                        onSaveDailyBudget = settingsViewModel::onSaveDailyBudget,
                        onNavigateBack = { currentScreen = AppDestination.SUMMARY },
                        lastBackupTimestamp = settingsUiState.lastBackupTimestamp,
                        autoBackupEnabled = settingsUiState.autoBackupEnabled,
                        autoBackupFolderUri = settingsUiState.autoBackupFolderUri,
                        autoBackupFolderName = settingsUiState.autoBackupFolderName,
                        lastAutoBackupTimestamp = settingsUiState.lastAutoBackupTimestamp,
                        isExporting = settingsUiState.isExporting,
                        isImporting = settingsUiState.isImporting,
                        pendingRestorePreview = settingsUiState.pendingRestorePreview,
                        onToggleAutoBackup = settingsViewModel::onToggleAutoBackup,
                        onSelectAutoBackupFolder = settingsViewModel::onSelectAutoBackupFolder,
                        onUnlinkAutoBackupFolder = settingsViewModel::onUnlinkAutoBackupFolder,
                        onExportBackup = settingsViewModel::onExportBackup,
                        onSelectBackupFile = settingsViewModel::onSelectBackupFile,
                        onConfirmRestore = settingsViewModel::onConfirmRestore,
                        onDismissRestoreDialog = settingsViewModel::onDismissRestoreDialog,
                        userMessage = settingsUiState.userMessage,
                        errorMessage = settingsUiState.errorMessage,
                        onClearUserMessage = settingsViewModel::clearUserMessage,
                        onClearErrorMessage = settingsViewModel::clearErrorMessage
                    )
                }
            }
        }

        // Capa 2 (Superpuesta): Barra de navegación con cuna flotando en la base
        if (isBottomBarVisible) {
            AppBottomNavigationBar(
                currentDestination = currentScreen,
                onNavigateToDestination = { destination ->
                    if (destination == AppDestination.CAMERA) {
                        pendingMealCategory = null
                        pendingMealDate = summaryUiState.selectedDate
                    }
                    currentScreen = destination
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Diálogo de calendario mensual si se abre desde el resumen diario
        if (statisticsUiState.isCalendarOpen && currentScreen == AppDestination.SUMMARY) {
            MonthlyHabitsCalendarDialog(
                isOpen = true,
                currentYearMonth = statisticsUiState.calendarYearMonth,
                calendarDays = statisticsUiState.calendarDays,
                selectedDate = statisticsUiState.selectedCalendarDate,
                onDateSelected = { date ->
                    statisticsViewModel.onEvent(StatisticsEvent.OnCalendarDateSelected(date))
                },
                onPreviousMonth = { statisticsViewModel.onEvent(StatisticsEvent.OnPreviousMonthClicked) },
                onNextMonth = { statisticsViewModel.onEvent(StatisticsEvent.OnNextMonthClicked) },
                onNavigateToDiaryDate = { date ->
                    dailySummaryViewModel.onEvent(DailySummaryEvent.OnDateSelected(date))
                    statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked)
                    currentScreen = AppDestination.SUMMARY
                },
                onDismiss = { statisticsViewModel.onEvent(StatisticsEvent.OnCloseCalendarClicked) }
            )
        }
    }
}
