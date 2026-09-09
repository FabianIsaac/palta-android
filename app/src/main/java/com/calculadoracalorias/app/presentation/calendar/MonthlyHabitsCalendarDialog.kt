package com.calculadoracalorias.app.presentation.calendar

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calculadoracalorias.app.domain.model.DayHabitSummary
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import com.calculadoracalorias.app.presentation.theme.MacroCarbs
import com.calculadoracalorias.app.presentation.theme.MacroFat
import com.calculadoracalorias.app.presentation.theme.MacroProtein
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyHabitsCalendarDialog(
    isOpen: Boolean,
    currentYearMonth: YearMonth,
    calendarDays: List<DayHabitSummary>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNavigateToDiaryDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val localeSpanish = Locale("es", "CL")
    val monthTitle = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", localeSpanish))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeSpanish) else it.toString() }

    val selectedDayDetail = calendarDays.firstOrNull { it.date == selectedDate }
        ?: DayHabitSummary(
            date = selectedDate,
            isCurrentMonth = selectedDate.year == currentYearMonth.year && selectedDate.month == currentYearMonth.month,
            isToday = selectedDate == LocalDate.now(),
            hasBreakfast = false,
            hasLunch = false,
            hasDinner = false,
            totalCalories = 0.0,
            targetCalories = 2000.0,
            totalProteinGrams = 0.0,
            totalCarbsGrams = 0.0,
            totalFatGrams = 0.0,
            supplementsTakenCount = 0,
            totalSupplementsCount = 0
        )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Barra superior del diálogo con título y botón de cierre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calendario de Hábitos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar calendario",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de mes con flechas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Mes anterior",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Mes siguiente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Días de la semana (Lunes a Domingo)
                val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayNames.forEach { name ->
                        Text(
                            text = name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cuadrícula de días
                val weeks = calendarDays.chunked(7)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { daySummary ->
                                val isSelected = daySummary.date == selectedDate
                                val isToday = daySummary.isToday

                                val cellBackground = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                }

                                val cellBorder = when {
                                    isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    else -> null
                                }

                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                    !daySummary.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellBackground)
                                        .then(if (cellBorder != null) Modifier.background(cellBackground, RoundedCornerShape(10.dp)) else Modifier)
                                        .clickable { onDateSelected(daySummary.date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${daySummary.date.dayOfMonth}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )

                                        // Badges: Hábito (🔥) y suplementos (punto)
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.height(10.dp)
                                        ) {
                                            if (daySummary.isHabitCompleted) {
                                                Text(
                                                    text = "🔥",
                                                    fontSize = 8.sp,
                                                    lineHeight = 8.sp
                                                )
                                            }
                                            if (daySummary.hasSupplementsTaken) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.tertiary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Ficha de detalle del día seleccionado
                SelectedDayDetailCard(
                    dayDetail = selectedDayDetail,
                    onNavigateToDiary = {
                        onNavigateToDiaryDate(selectedDate)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    dayDetail: DayHabitSummary,
    onNavigateToDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localeSpanish = Locale("es", "CL")
    val formattedDate = dayDetail.date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", localeSpanish))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeSpanish) else it.toString() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fecha encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dayDetail.isToday) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Hoy",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Estado del hábito y suplementos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dayDetail.isHabitCompleted) {
                    HabitStatusChip(
                        text = "🔥 Hábito cumplido",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    val mealsStatus = buildString {
                        append("Comidas: ")
                        append(if (dayDetail.hasBreakfast) "Desayuno ✓ " else "Desayuno — ")
                        append(if (dayDetail.hasLunch) "Almuerzo ✓ " else "Almuerzo — ")
                        append(if (dayDetail.hasDinner) "Once/Cena ✓" else "Once/Cena —")
                    }
                    Text(
                        text = mealsStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dayDetail.totalSupplementsCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val suppText = if (dayDetail.hasSupplementsTaken) {
                    "💊 Suplementos: ${dayDetail.supplementsTakenCount}/${dayDetail.totalSupplementsCount} tomados"
                } else {
                    "💊 Suplementos: pendientes (${dayDetail.totalSupplementsCount})"
                }
                Text(
                    text = suppText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Consumo calórico y macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calorías consumidas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f / %.0f kcal".format(Locale.getDefault(), dayDetail.totalCalories, dayDetail.targetCalories),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroPill(label = "P", grams = dayDetail.totalProteinGrams, color = MacroProtein)
                    MacroPill(label = "C", grams = dayDetail.totalCarbsGrams, color = MacroCarbs)
                    MacroPill(label = "G", grams = dayDetail.totalFatGrams, color = MacroFat)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de salto al diario
            Button(
                onClick = onNavigateToDiary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ir al Diario de este día",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HabitStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun MacroPill(
    label: String,
    grams: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "%.0fg".format(Locale.getDefault(), grams),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==========================================
// Previews
// ==========================================

private fun getSampleCalendarDays(): List<DayHabitSummary> {
    val base = LocalDate.of(2026, 9, 8)
    return (-7..27).map { offset ->
        val date = base.plusDays(offset.toLong())
        val isCurrent = date.monthValue == 9
        val isHabit = offset in listOf(0, -1, -3, 2, 5)
        val hasSupps = offset in listOf(0, -1, 1, 2)
        DayHabitSummary(
            date = date,
            isCurrentMonth = isCurrent,
            isToday = offset == 0,
            hasBreakfast = isHabit || offset % 2 == 0,
            hasLunch = isHabit || offset % 3 == 0,
            hasDinner = isHabit,
            totalCalories = if (isHabit) 1950.0 else 1250.0,
            targetCalories = 2000.0,
            totalProteinGrams = if (isHabit) 145.0 else 85.0,
            totalCarbsGrams = if (isHabit) 210.0 else 140.0,
            totalFatGrams = if (isHabit) 62.0 else 45.0,
            supplementsTakenCount = if (hasSupps) 2 else 0,
            totalSupplementsCount = 2
        )
    }
}

@Preview(name = "Modo Claro", showBackground = true)
@Composable
private fun MonthlyHabitsCalendarDialogLightPreview() {
    CalculadoraCaloriasTheme(darkTheme = false) {
        MonthlyHabitsCalendarDialog(
            isOpen = true,
            currentYearMonth = YearMonth.of(2026, 9),
            calendarDays = getSampleCalendarDays(),
            selectedDate = LocalDate.of(2026, 9, 8),
            onDateSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onNavigateToDiaryDate = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Modo Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MonthlyHabitsCalendarDialogDarkPreview() {
    CalculadoraCaloriasTheme(darkTheme = true) {
        MonthlyHabitsCalendarDialog(
            isOpen = true,
            currentYearMonth = YearMonth.of(2026, 9),
            calendarDays = getSampleCalendarDays(),
            selectedDate = LocalDate.of(2026, 9, 8),
            onDateSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onNavigateToDiaryDate = {},
            onDismiss = {}
        )
    }
}
