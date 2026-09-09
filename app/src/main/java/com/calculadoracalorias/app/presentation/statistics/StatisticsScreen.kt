package com.calculadoracalorias.app.presentation.statistics

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.DayCalorieMetric
import com.calculadoracalorias.app.domain.model.PeriodStatistics
import com.calculadoracalorias.app.domain.model.StatisticsRange
import com.calculadoracalorias.app.presentation.calendar.MonthlyHabitsCalendarDialog
import com.calculadoracalorias.app.presentation.statistics.components.CalorieEvolutionBarChart
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import com.calculadoracalorias.app.presentation.theme.MacroCarbs
import com.calculadoracalorias.app.presentation.theme.MacroFat
import com.calculadoracalorias.app.presentation.theme.MacroProtein
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onEvent: (StatisticsEvent) -> Unit,
    onNavigateToDiaryDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_statistics),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onEvent(StatisticsEvent.OnOpenCalendarClicked()) }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Abrir calendario mensual",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector de Rango de Análisis (7, 14, 30 días)
                RangeSelectorRow(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = { range -> onEvent(StatisticsEvent.OnRangeSelected(range)) }
                )

                if (uiState.isLoading && uiState.statistics == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.statistics != null) {
                    val stats = uiState.statistics

                    // Tarjeta de Resumen Global del Periodo
                    PeriodSummaryCard(stats = stats)

                    // Gráfico de Evolución Calórica
                    CalorieEvolutionBarChart(
                        metrics = stats.dailyCalorieMetrics,
                        targetCalories = stats.targetDailyCalories,
                        selectedDate = uiState.selectedCalendarDate,
                        onDaySelected = { date -> onEvent(StatisticsEvent.OnCalendarDateSelected(date)) }
                    )

                    // Desglose de Macronutrientes Promedio
                    MacroAveragesCard(stats = stats)

                    // Consistencia de Suplementos
                    SupplementConsistencyCard(stats = stats)
                }
            }

            // Diálogo Modal de Calendario Mensual de Hábitos
            MonthlyHabitsCalendarDialog(
                isOpen = uiState.isCalendarOpen,
                currentYearMonth = uiState.calendarYearMonth,
                calendarDays = uiState.calendarDays,
                selectedDate = uiState.selectedCalendarDate,
                onDateSelected = { date -> onEvent(StatisticsEvent.OnCalendarDateSelected(date)) },
                onPreviousMonth = { onEvent(StatisticsEvent.OnPreviousMonthClicked) },
                onNextMonth = { onEvent(StatisticsEvent.OnNextMonthClicked) },
                onNavigateToDiaryDate = onNavigateToDiaryDate,
                onDismiss = { onEvent(StatisticsEvent.OnCloseCalendarClicked) }
            )
        }
    }
}

@Composable
private fun RangeSelectorRow(
    selectedRange: StatisticsRange,
    onRangeSelected: (StatisticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatisticsRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            FilterChip(
                selected = isSelected,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        text = range.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PeriodSummaryCard(
    stats: PeriodStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Promedio Diario de Calorías
            MetricSummaryColumn(
                title = "Promedio Diario",
                value = "%.0f".format(Locale.getDefault(), stats.averageDailyCalories),
                unit = "kcal",
                subtitle = "Meta: %.0f".format(Locale.getDefault(), stats.targetDailyCalories),
                highlightColor = MaterialTheme.colorScheme.primary
            )

            // Días con Hábito (3 comidas principales)
            MetricSummaryColumn(
                title = "Hábito Cumplido",
                value = "${stats.habitCompletedDaysCount}",
                unit = "/ ${stats.totalPeriodDays} d",
                subtitle = "3 comidas diarias",
                highlightColor = MaterialTheme.colorScheme.secondary
            )

            // Racha actual
            MetricSummaryColumn(
                title = "Racha Actual",
                value = "${stats.currentStreakDays}",
                unit = "días",
                subtitle = "Consecutivos",
                highlightColor = Color(0xFFE76F51) // Tono fuego
            )
        }
    }
}

@Composable
private fun MetricSummaryColumn(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MacroAveragesCard(
    stats: PeriodStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Promedio de Macronutrientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Consumo diario estimado en el periodo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjetas de macros individuales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroStatCard(
                    name = "Proteína",
                    grams = stats.averageProteinGrams,
                    color = MacroProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroStatCard(
                    name = "Carbos",
                    grams = stats.averageCarbsGrams,
                    color = MacroCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroStatCard(
                    name = "Grasas",
                    grams = stats.averageFatGrams,
                    color = MacroFat,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra proporcional de macronutrientes
            val proteinKcal = stats.averageProteinGrams * 4.0
            val carbsKcal = stats.averageCarbsGrams * 4.0
            val fatKcal = stats.averageFatGrams * 9.0
            val totalMacroKcal = (proteinKcal + carbsKcal + fatKcal).coerceAtLeast(1.0)

            val pWeight = (proteinKcal / totalMacroKcal).toFloat().coerceAtLeast(0.01f)
            val cWeight = (carbsKcal / totalMacroKcal).toFloat().coerceAtLeast(0.01f)
            val fWeight = (fatKcal / totalMacroKcal).toFloat().coerceAtLeast(0.01f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(modifier = Modifier.weight(pWeight).fillMaxSize().background(MacroProtein))
                Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.weight(cWeight).fillMaxSize().background(MacroCarbs))
                Spacer(modifier = Modifier.width(2.dp))
                Box(modifier = Modifier.weight(fWeight).fillMaxSize().background(MacroFat))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Porcentajes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "P: %.0f%%".format(Locale.getDefault(), (pWeight * 100)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroProtein
                )
                Text(
                    text = "C: %.0f%%".format(Locale.getDefault(), (cWeight * 100)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroCarbs
                )
                Text(
                    text = "G: %.0f%%".format(Locale.getDefault(), (fWeight * 100)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MacroFat
                )
            }
        }
    }
}

@Composable
private fun MacroStatCard(
    name: String,
    grams: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "%.0fg".format(Locale.getDefault(), grams),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SupplementConsistencyCard(
    stats: PeriodStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Consistencia de Suplementos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adherencia a la rutina activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "%.0f%%".format(Locale.getDefault(), stats.supplementAdherencePercentage),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (stats.supplementAdherencePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

// ==========================================
// Compose Previews
// ==========================================

private fun getMockPeriodStatistics(range: StatisticsRange = StatisticsRange.LAST_7_DAYS): PeriodStatistics {
    val today = LocalDate.of(2026, 9, 8)
    val metrics = (range.days - 1 downTo 0).map { offset ->
        val d = today.minusDays(offset.toLong())
        DayCalorieMetric(
            date = d,
            calories = 1850.0 + (offset * 35 % 300),
            targetCalories = 2000.0,
            isToday = offset == 0
        )
    }

    return PeriodStatistics(
        range = range,
        startDate = today.minusDays((range.days - 1).toLong()),
        endDate = today,
        averageDailyCalories = 1960.0,
        targetDailyCalories = 2000.0,
        averageProteinGrams = 148.0,
        averageCarbsGrams = 205.0,
        averageFatGrams = 62.0,
        habitCompletedDaysCount = (range.days * 0.75).toInt(),
        totalPeriodDays = range.days,
        currentStreakDays = 5,
        supplementAdherencePercentage = 85.0,
        dailyCalorieMetrics = metrics
    )
}

@Preview(name = "Estadísticas 7 días - Modo Claro", showBackground = true)
@Composable
private fun StatisticsScreenLightPreview() {
    CalculadoraCaloriasTheme(darkTheme = false) {
        StatisticsScreen(
            uiState = StatisticsUiState(
                isLoading = false,
                selectedRange = StatisticsRange.LAST_7_DAYS,
                statistics = getMockPeriodStatistics(StatisticsRange.LAST_7_DAYS)
            ),
            onEvent = {},
            onNavigateToDiaryDate = {}
        )
    }
}

@Preview(name = "Estadísticas 14 días - Modo Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatisticsScreenDarkPreview() {
    CalculadoraCaloriasTheme(darkTheme = true) {
        StatisticsScreen(
            uiState = StatisticsUiState(
                isLoading = false,
                selectedRange = StatisticsRange.LAST_14_DAYS,
                statistics = getMockPeriodStatistics(StatisticsRange.LAST_14_DAYS)
            ),
            onEvent = {},
            onNavigateToDiaryDate = {}
        )
    }
}
