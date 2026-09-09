package com.calculadoracalorias.app.presentation.statistics.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.domain.model.DayCalorieMetric
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

/**
 * Gráfico de barras fluido y nativo en Compose Canvas para visualizar la evolución del consumo
 * calórico diario comparado contra la meta establecida.
 */
@Composable
fun CalorieEvolutionBarChart(
    metrics: List<DayCalorieMetric>,
    targetCalories: Double,
    selectedDate: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val activeSelectedMetric = metrics.firstOrNull { it.date == selectedDate }
        ?: metrics.firstOrNull { it.isToday }
        ?: metrics.lastOrNull()

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
            // Encabezado del gráfico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Evolución Calórica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                    Text(
                        text = "Línea punteada: Meta diaria (%.0f kcal)".format(Locale.getDefault(), targetCalories),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant
                    )
                }

                if (activeSelectedMetric != null) {
                    val localeSpanish = Locale("es", "CL")
                    val dayLabel = activeSelectedMetric.date.format(DateTimeFormatter.ofPattern("d MMM", localeSpanish))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dayLabel: %.0f kcal".format(Locale.getDefault(), activeSelectedMetric.calories),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (metrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin datos de comidas para este periodo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .pointerInput(metrics) {
                            detectTapGestures { offset ->
                                val totalBars = metrics.size
                                if (totalBars > 0) {
                                    val barSlotWidth = size.width / totalBars
                                    val clickedIndex = (offset.x / barSlotWidth).toInt().coerceIn(0, totalBars - 1)
                                    onDaySelected(metrics[clickedIndex].date)
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val totalHeight = size.height
                    val bottomLabelSpace = 28.dp.toPx()
                    val chartHeight = totalHeight - bottomLabelSpace

                    val maxConsumed = metrics.maxOfOrNull { it.calories } ?: 2000.0
                    val maxVal = max(targetCalories, maxConsumed) * 1.18f

                    // Línea horizontal de meta calórica
                    val targetY = chartHeight * (1f - (targetCalories / maxVal).toFloat())
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

                    drawLine(
                        color = primaryColor.copy(alpha = 0.55f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // Línea base del eje X
                    drawLine(
                        color = outlineVariant.copy(alpha = 0.4f),
                        start = Offset(0f, chartHeight),
                        end = Offset(width, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    val barCount = metrics.size
                    val slotWidth = width / barCount
                    val barWidth = (slotWidth * 0.56f).coerceIn(4.dp.toPx(), 28.dp.toPx())

                    val labelStyle = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceVariant
                    )
                    val selectedLabelStyle = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    metrics.forEachIndexed { index, metric ->
                        val isSelected = metric.date == selectedDate
                        val slotCenter = index * slotWidth + slotWidth / 2f
                        val barLeft = slotCenter - barWidth / 2f

                        val barProportion = (metric.calories / maxVal).toFloat().coerceIn(0.01f, 1f)
                        val barHeight = (chartHeight * barProportion).coerceAtLeast(4.dp.toPx())
                        val barTop = chartHeight - barHeight

                        // Fondo de selección
                        if (isSelected) {
                            drawRoundRect(
                                color = primaryColor.copy(alpha = 0.15f),
                                topLeft = Offset(slotCenter - (barWidth * 0.9f), 0f),
                                size = Size(barWidth * 1.8f, chartHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }

                        // Barra del día
                        val barColor = when {
                            isSelected -> primaryColor
                            metric.isToday -> primaryColor.copy(alpha = 0.85f)
                            else -> secondaryColor.copy(alpha = 0.65f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Etiqueta del eje X
                        val shouldShowLabel = when {
                            barCount <= 7 -> true
                            barCount <= 14 -> index % 2 == 0 || index == barCount - 1
                            else -> index % 5 == 0 || index == barCount - 1
                        }

                        if (shouldShowLabel) {
                            val labelText = if (barCount <= 7) {
                                val dayLetter = when (metric.date.dayOfWeek.value) {
                                    1 -> "L"
                                    2 -> "M"
                                    3 -> "M"
                                    4 -> "J"
                                    5 -> "V"
                                    6 -> "S"
                                    else -> "D"
                                }
                                "$dayLetter ${metric.date.dayOfMonth}"
                            } else {
                                "${metric.date.dayOfMonth}"
                            }

                            val measuredText = textMeasurer.measure(
                                text = labelText,
                                style = if (isSelected) selectedLabelStyle else labelStyle
                            )

                            drawText(
                                textLayoutResult = measuredText,
                                topLeft = Offset(
                                    x = slotCenter - measuredText.size.width / 2f,
                                    y = chartHeight + 6.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Leyenda de referencia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(primaryColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Consumo diario",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(primaryColor.copy(alpha = 0.55f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Meta calórica",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

private fun getSampleMetrics(count: Int = 7): List<DayCalorieMetric> {
    val today = LocalDate.of(2026, 9, 8)
    val cals = listOf(1950.0, 2120.0, 1840.0, 2250.0, 1980.0, 2040.0, 1920.0)
    return (count - 1 downTo 0).mapIndexed { i, offset ->
        val date = today.minusDays(offset.toLong())
        val cal = cals[i % cals.size]
        DayCalorieMetric(
            date = date,
            calories = cal,
            targetCalories = 2000.0,
            isToday = offset == 0
        )
    }
}

@Preview(name = "Gráfico 7 días - Modo Claro", showBackground = true)
@Composable
private fun CalorieChart7DaysLightPreview() {
    CalculadoraCaloriasTheme(darkTheme = false) {
        CalorieEvolutionBarChart(
            metrics = getSampleMetrics(7),
            targetCalories = 2000.0,
            selectedDate = LocalDate.of(2026, 9, 8),
            onDaySelected = {}
        )
    }
}

@Preview(name = "Gráfico 14 días - Modo Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalorieChart14DaysDarkPreview() {
    CalculadoraCaloriasTheme(darkTheme = true) {
        CalorieEvolutionBarChart(
            metrics = getSampleMetrics(14),
            targetCalories = 2000.0,
            selectedDate = LocalDate.of(2026, 9, 6),
            onDaySelected = {}
        )
    }
}
