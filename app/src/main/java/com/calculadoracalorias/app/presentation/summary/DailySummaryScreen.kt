package com.calculadoracalorias.app.presentation.summary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.DailyMacroBudget
import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.MealEntry
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import com.calculadoracalorias.app.presentation.theme.MacroCarbs
import com.calculadoracalorias.app.presentation.theme.MacroFat
import com.calculadoracalorias.app.presentation.theme.MacroProtein
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(
    uiState: DailySummaryUiState,
    onEvent: (DailySummaryEvent) -> Unit,
    onNavigateToCamera: (MealCategory?) -> Unit,
    onNavigateToEditMeal: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_daily_summary),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenCalendar) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Abrir calendario mensual",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Selector de fecha
            item {
                DateSelectorHeader(
                    selectedDate = uiState.selectedDate,
                    onPreviousDay = { onEvent(DailySummaryEvent.OnPreviousDayClicked) },
                    onNextDay = { onEvent(DailySummaryEvent.OnNextDayClicked) },
                    onOpenCalendar = onOpenCalendar
                )
            }

            // 2. Widget motivador de racha (Días Buenos)
            item {
                DailyStreakCard(streak = uiState.streak)
            }

            // 3. Anillo de progreso calórico y desglose de macros
            item {
                CalorieMacroOverviewCard(uiState = uiState)
            }

            // 3.1 Actividad física y pasos (Health Connect)
            if (uiState.isActivitySyncEnabled) {
                item {
                    DailyActivityCard(
                        burnedCalories = uiState.burnedCalories,
                        stepsCount = uiState.stepsCount,
                        includeBurnedInBudget = uiState.includeBurnedInBudget,
                        onRefresh = { onEvent(DailySummaryEvent.OnRefreshActivity) }
                    )
                }
            }

            // 4. Tarjetas de categorías chilenas
            items(MealCategory.entries.toList(), key = { it.name }) { category ->
                val meals = uiState.mealsByCategory[category].orEmpty()
                ChileanMealCategoryCard(
                    category = category,
                    meals = meals,
                    onAddMeal = { onNavigateToCamera(category) },
                    onMealClicked = onNavigateToEditMeal
                )
            }

            // 5. Widget de suplementos diarios (inmediatamente después de comidas y colaciones)
            if (uiState.supplements.isNotEmpty()) {
                item {
                    DailySupplementsCard(
                        supplements = uiState.supplements,
                        onToggleSupplement = { supplementId, isTaken ->
                            onEvent(DailySummaryEvent.OnToggleSupplement(supplementId, isTaken))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelectorHeader(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isToday = selectedDate == LocalDate.now()
    val localeSpanish = Locale("es", "CL")
    val formattedDate = if (isToday) {
        stringResource(R.string.label_today) + " (${selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", localeSpanish))})"
    } else {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeSpanish))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeSpanish) else it.toString() }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_previous_day)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenCalendar() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.btn_next_day)
                )
            }
        }
    }
}

@Composable
fun CalorieMacroOverviewCard(
    uiState: DailySummaryUiState,
    modifier: Modifier = Modifier
) {
    val calorieProgress = if (uiState.targetCalories > 0) {
        (uiState.consumedCalories / uiState.targetCalories).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = calorieProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "CalorieProgress"
    )

    val isExceeded = uiState.remainingCalories < 0
    val remainingKcal = abs(uiState.remainingCalories)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Anillo circular con texto dentro
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 14.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    strokeWidth = 14.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%.0f".format(Locale.getDefault(), uiState.consumedCalories),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.unit_kcal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isExceeded) {
                            stringResource(R.string.label_exceeded_calories, remainingKcal)
                        } else {
                            stringResource(R.string.label_remaining_calories, remainingKcal)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_target_calories, uiState.targetCalories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Barras de macronutrientes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroProgressBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.label_protein),
                    consumedGrams = uiState.consumedProteinGrams,
                    targetGrams = uiState.targetProteinGrams,
                    color = MacroProtein
                )
                MacroProgressBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.label_carbs),
                    consumedGrams = uiState.consumedCarbsGrams,
                    targetGrams = uiState.targetCarbsGrams,
                    color = MacroCarbs
                )
                MacroProgressBar(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.label_fat),
                    consumedGrams = uiState.consumedFatGrams,
                    targetGrams = uiState.targetFatGrams,
                    color = MacroFat
                )
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    consumedGrams: Double,
    targetGrams: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (targetGrams > 0) {
        (consumedGrams / targetGrams).toFloat().coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "%.0fg / %.0fg".format(Locale.getDefault(), consumedGrams, targetGrams),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChileanMealCategoryCard(
    category: MealCategory,
    meals: List<MealEntry>,
    onAddMeal: () -> Unit,
    onMealClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCategoryCalories = meals.sumOf { it.summary.totalCalories }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera de la categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (meals.isNotEmpty()) {
                            Text(
                                text = "%.0f kcal".format(Locale.getDefault(), totalCategoryCalories),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onAddMeal,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_add_to_category),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de comidas o estado vacío
            if (meals.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_category_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    meals.forEach { meal ->
                        MealEntryRow(
                            meal = meal,
                            onClick = { onMealClicked(meal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MealEntryRow(
    meal: MealEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val foodNames = meal.items.joinToString(", ") { "${it.name} (%.0fg)".format(Locale.getDefault(), it.servingGrams) }
                Text(
                    text = foodNames.ifBlank { "Comida registrada" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                if (meal.isPendingAiRefinement) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⏳ " + stringResource(R.string.badge_pending_ai_refinement),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "P: %.1fg  •  C: %.1fg  •  G: %.1fg".format(
                        Locale.getDefault(),
                        meal.summary.totalProtein,
                        meal.summary.totalCarbs,
                        meal.summary.totalFat
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.0f kcal".format(Locale.getDefault(), meal.summary.totalCalories),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "DailySummaryScreen Preview")
@Composable
fun DailySummaryScreenPreview() {
    val sampleItem = ScannedFoodItem(
        id = "1",
        name = "Marraqueta con palta",
        servingGrams = 120.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 8.0,
        carbsPer100g = 45.0,
        fatPer100g = 5.0
    )
    val sampleMeal = MealEntry(
        id = 1L,
        category = MealCategory.DESAYUNO,
        timestamp = System.currentTimeMillis(),
        items = listOf(sampleItem),
        summary = NutritionSummary(300.0, 9.6, 54.0, 6.0)
    )

    val previewState = DailySummaryUiState(
        selectedDate = LocalDate.now(),
        isLoading = false,
        targetCalories = 2000.0,
        consumedCalories = 650.0,
        remainingCalories = 1350.0,
        targetProteinGrams = 150.0,
        consumedProteinGrams = 45.0,
        targetCarbsGrams = 200.0,
        consumedCarbsGrams = 70.0,
        targetFatGrams = 65.0,
        consumedFatGrams = 22.0,
        mealsByCategory = mapOf(
            MealCategory.DESAYUNO to listOf(sampleMeal),
            MealCategory.ALMUERZO to emptyList(),
            MealCategory.ONCE_CENA to emptyList(),
            MealCategory.COLACIONES to emptyList()
        ),
        streak = DailyStreak(
            currentStreakDays = 4,
            hasBreakfastToday = true,
            hasLunchToday = false,
            hasDinnerToday = false
        ),
        supplements = listOf(
            Supplement.DEFAULT_OMEGA_3.copy(isTakenToday = true)
        )
    )

    CalculadoraCaloriasTheme {
        Surface {
            DailySummaryScreen(
                uiState = previewState,
                onEvent = {},
                onNavigateToCamera = {},
                onNavigateToEditMeal = {},
                onNavigateToSettings = {}
            )
        }
    }
}
