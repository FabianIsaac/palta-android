package com.calculadoracalorias.app.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculadoracalorias.app.domain.model.DetectedMealResult
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.usecase.RecalculatePortionUseCase
import com.calculadoracalorias.app.domain.usecase.SaveMealWithHealthSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FoodScanReviewViewModel(
    private val recalculatePortionUseCase: RecalculatePortionUseCase = RecalculatePortionUseCase(),
    private val saveMealWithHealthSyncUseCase: SaveMealWithHealthSyncUseCase,
    private val parseNaturalLanguageMealUseCase: com.calculadoracalorias.app.domain.usecase.ParseNaturalLanguageMealUseCase? = null,
    private val mealRepository: com.calculadoracalorias.app.domain.repository.MealRepository? = null,
    private val analyzeFoodImageUseCase: com.calculadoracalorias.app.domain.usecase.AnalyzeFoodImageUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodScanReviewUiState())
    val uiState: StateFlow<FoodScanReviewUiState> = _uiState.asStateFlow()

    fun resetState(targetDate: LocalDate = LocalDate.now()) {
        _uiState.value = FoodScanReviewUiState(targetDate = targetDate)
    }

    fun startImageAnalysis(
        imageBytes: ByteArray,
        category: MealCategory? = null,
        targetDate: LocalDate = _uiState.value.targetDate,
        analyzeCall: (suspend (ByteArray) -> Result<DetectedMealResult>)? = null
    ) {
        val targetCategory = category ?: _uiState.value.selectedCategory
        _uiState.update {
            it.copy(
                isLoading = true,
                isAnalyzingText = false,
                imagePath = null,
                items = emptyList(),
                selectedCategory = targetCategory,
                targetDate = targetDate,
                totalCalories = 0.0,
                totalProtein = 0.0,
                totalCarbs = 0.0,
                totalFat = 0.0,
                errorMessage = null,
                saveSuccess = false,
                consolidateTargetMealMinutesAgo = null
            )
        }

        checkRecentMealConsolidation(targetCategory, targetDate = targetDate)

        viewModelScope.launch {
            val result = if (analyzeCall != null) {
                analyzeCall(imageBytes)
            } else if (analyzeFoodImageUseCase != null) {
                analyzeFoodImageUseCase(imageBytes)
            } else {
                Result.failure(IllegalStateException("No hay analizador de imagen disponible."))
            }

            result.fold(
                onSuccess = { detectedResult ->
                    val finalResult = if (category != null) {
                        detectedResult.copy(suggestedMealType = category)
                    } else {
                        detectedResult
                    }
                    initializeWithResult(finalResult, targetDate = targetDate)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = emptyList(),
                            selectedCategory = targetCategory,
                            targetDate = targetDate,
                            errorMessage = error.message ?: "No pudimos identificar con certeza tu comida. Puedes buscarla por nombre o describirla."
                        )
                    }
                }
            )
        }
    }

    fun initializeWithResult(
        result: DetectedMealResult,
        imagePath: String? = null,
        initialErrorMessage: String? = null,
        targetDate: LocalDate = _uiState.value.targetDate
    ) {
        val summary = NutritionSummary.fromItems(result.items)
        _uiState.update {
            it.copy(
                isLoading = false,
                isAnalyzingText = false,
                imagePath = imagePath,
                items = result.items,
                selectedCategory = result.suggestedMealType,
                targetDate = targetDate,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat,
                errorMessage = initialErrorMessage
            )
        }
        checkRecentMealConsolidation(result.suggestedMealType, targetDate = targetDate)
    }

    fun checkRecentMealConsolidation(
        category: MealCategory,
        targetDate: LocalDate = _uiState.value.targetDate,
        currentTimestamp: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val repo = mealRepository ?: return
        viewModelScope.launch {
            try {
                val effectiveDate = if (currentTimestamp != null && targetDate == _uiState.value.targetDate) {
                    Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
                } else {
                    targetDate
                }
                val startOfDay = effectiveDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfDay = effectiveDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

                val recentMeal = repo.getMostRecentMealForCategory(category, startOfDay, endOfDay).getOrNull()
                if (recentMeal != null) {
                    val nowMs = currentTimestamp ?: effectiveDate.atTime(LocalTime.now()).atZone(zoneId).toInstant().toEpochMilli()
                    val diffMs = nowMs - recentMeal.timestamp
                    val thirtyMinutesMs = 30 * 60 * 1000L
                    if (diffMs in 0..thirtyMinutesMs) {
                        val minutesAgo = (diffMs / (60 * 1000L)).toInt().coerceAtLeast(1)
                        _uiState.update { it.copy(consolidateTargetMealMinutesAgo = minutesAgo) }
                        return@launch
                    }
                }
                _uiState.update { it.copy(consolidateTargetMealMinutesAgo = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(consolidateTargetMealMinutesAgo = null) }
            }
        }
    }

    fun onEvent(event: FoodScanReviewEvent) {
        when (event) {
            is FoodScanReviewEvent.OnPortionChanged -> handlePortionChanged(event.itemId, event.newGrams)
            is FoodScanReviewEvent.OnItemRemoved -> handleItemRemoved(event.itemId)
            is FoodScanReviewEvent.OnItemAdded -> handleItemAdded(event.item)
            is FoodScanReviewEvent.OnItemNameChanged -> handleItemNameChanged(event.itemId, event.newName)
            is FoodScanReviewEvent.OnMealCategoryChanged -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
                checkRecentMealConsolidation(event.category, targetDate = _uiState.value.targetDate)
            }
            is FoodScanReviewEvent.OnTargetDateChanged -> {
                _uiState.update { it.copy(targetDate = event.date) }
                checkRecentMealConsolidation(_uiState.value.selectedCategory, targetDate = event.date)
            }
            is FoodScanReviewEvent.OnAnalyzeNaturalLanguage -> handleAnalyzeNaturalLanguage(event.description, event.category, event.targetDate)
            FoodScanReviewEvent.OnRetryNaturalLanguage -> {
                val lastDesc = _uiState.value.lastRawDescription
                if (!lastDesc.isNullOrBlank()) {
                    handleAnalyzeNaturalLanguage(lastDesc, _uiState.value.selectedCategory, _uiState.value.targetDate)
                }
            }
            FoodScanReviewEvent.OnSavePendingAiRefinement -> {
                _uiState.update { it.copy(isPendingAiRefinement = true) }
                handleConfirmAndSave()
            }
            FoodScanReviewEvent.OnConfirmAndSave -> handleConfirmAndSave()
            FoodScanReviewEvent.OnDismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            FoodScanReviewEvent.OnDismissSuccess -> {
                resetState(_uiState.value.targetDate)
            }
            FoodScanReviewEvent.OnResetState -> resetState(_uiState.value.targetDate)
        }
    }

    private fun handleAnalyzeNaturalLanguage(
        description: String,
        category: MealCategory? = null,
        targetDate: LocalDate? = null
    ) {
        val useCase = parseNaturalLanguageMealUseCase
        if (useCase == null) {
            _uiState.update { it.copy(errorMessage = "El analizador de lenguaje natural no está configurado.") }
            return
        }

        val targetCategory = category ?: _uiState.value.selectedCategory
        val effectiveDate = targetDate ?: _uiState.value.targetDate
        _uiState.update {
            it.copy(
                isLoading = true,
                isAnalyzingText = true,
                errorMessage = null,
                selectedCategory = targetCategory,
                targetDate = effectiveDate,
                lastRawDescription = description,
                canRetryTextAnalysis = false
            )
        }

        viewModelScope.launch {
            val result = useCase(description)
            result.fold(
                onSuccess = { detectedResult ->
                    val isLocalFallback = detectedResult.analysisSource == com.calculadoracalorias.app.domain.model.VisionSource.LOCAL_DEVICE
                    val combinedItems = if (_uiState.value.items.isEmpty()) {
                        detectedResult.items
                    } else {
                        _uiState.value.items + detectedResult.items
                    }
                    val summary = NutritionSummary.fromItems(combinedItems)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAnalyzingText = false,
                            items = combinedItems,
                            selectedCategory = category ?: (if (_uiState.value.items.isEmpty()) detectedResult.suggestedMealType else it.selectedCategory),
                            targetDate = effectiveDate,
                            totalCalories = summary.totalCalories,
                            totalProtein = summary.totalProtein,
                            totalCarbs = summary.totalCarbs,
                            totalFat = summary.totalFat,
                            lastRawDescription = description,
                            isPendingAiRefinement = isLocalFallback,
                            canRetryTextAnalysis = isLocalFallback
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAnalyzingText = false,
                            errorMessage = error.message ?: "No se pudo interpretar la comida ingresada.",
                            lastRawDescription = description,
                            canRetryTextAnalysis = true
                        )
                    }
                }
            )
        }
    }

    private fun handlePortionChanged(itemId: String, newGrams: Double) {
        val currentItems = _uiState.value.items
        val updatedItems = recalculatePortionUseCase(currentItems, itemId, newGrams)
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleItemRemoved(itemId: String) {
        val updatedItems = _uiState.value.items.filterNot { it.id == itemId }
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleItemAdded(item: ScannedFoodItem) {
        val updatedItems = _uiState.value.items + item
        val summary = NutritionSummary.fromItems(updatedItems)

        _uiState.update {
            it.copy(
                items = updatedItems,
                totalCalories = summary.totalCalories,
                totalProtein = summary.totalProtein,
                totalCarbs = summary.totalCarbs,
                totalFat = summary.totalFat
            )
        }
    }

    private fun handleItemNameChanged(itemId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updatedItems = _uiState.value.items.map { item ->
            if (item.id == itemId) item.copy(name = trimmed) else item
        }
        _uiState.update { it.copy(items = updatedItems) }
    }

    private fun handleConfirmAndSave() {
        val currentState = _uiState.value
        if (currentState.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Agrega al menos un alimento para poder guardar tu comida.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        val targetTime = LocalTime.now()
        val mealTimestamp = currentState.targetDate
            .atTime(targetTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        viewModelScope.launch {
            val result = saveMealWithHealthSyncUseCase(
                mealName = currentState.selectedCategory.displayName,
                category = currentState.selectedCategory,
                timestamp = mealTimestamp,
                items = currentState.items,
                rawDescription = currentState.lastRawDescription,
                isPendingAiRefinement = currentState.isPendingAiRefinement
            )

            result.fold(
                onSuccess = { saveResult ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            syncedWithHealthConnect = saveResult.syncedWithHealthConnect
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Ocurrió un error al guardar la comida."
                        )
                    }
                }
            )
        }
    }
}
