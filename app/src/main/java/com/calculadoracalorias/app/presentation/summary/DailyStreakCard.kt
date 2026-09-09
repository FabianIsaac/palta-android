package com.calculadoracalorias.app.presentation.summary

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.DailyStreak
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

/**
 * Widget compacto de racha de hábitos y días buenos.
 * Diseñado en una sola fila minimalista con superficie uniforme y sin botones toscos.
 */
@Composable
fun DailyStreakCard(
    streak: DailyStreak,
    modifier: Modifier = Modifier
) {
    val streakTitle = when {
        streak.currentStreakDays == 0 -> stringResource(R.string.streak_title_zero)
        streak.currentStreakDays == 1 -> stringResource(R.string.streak_title_single)
        else -> stringResource(R.string.streak_title, streak.currentStreakDays)
    }

    val missingMeals = streak.missingMealsToday
    val subtitle = when {
        streak.isTodayCompleted -> stringResource(R.string.streak_completed_today)
        missingMeals.size == 1 -> stringResource(R.string.streak_prompt_missing, missingMeals.first().displayName)
        missingMeals.size in 2..3 && streak.currentStreakDays > 0 -> {
            val names = missingMeals.joinToString(" y ") { it.displayName }
            stringResource(R.string.streak_prompt_missing, names)
        }
        else -> stringResource(R.string.streak_start_prompt)
    }

    val completedCount = listOf(streak.hasBreakfastToday, streak.hasLunchToday, streak.hasDinnerToday).count { it }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Icono sutil
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (streak.currentStreakDays > 0) {
                                Color(0xFFFF9800).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (streak.currentStreakDays > 0) "🔥" else "⚡",
                        fontSize = 18.sp
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = streakTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (streak.isTodayCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badge lateral de estado / progreso
            if (streak.isTodayCompleted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "¡Al 100%!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$completedCount/3",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "DailyStreakCard - Light (Inactivo)", showBackground = true)
@Preview(name = "DailyStreakCard - Dark (Inactivo)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DailyStreakCardInactivePreview() {
    CalculadoraCaloriasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DailyStreakCard(
                streak = DailyStreak(
                    currentStreakDays = 0,
                    hasBreakfastToday = false,
                    hasLunchToday = false,
                    hasDinnerToday = false
                )
            )
        }
    }
}

@Preview(name = "DailyStreakCard - Dark (Activo)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DailyStreakCardActivePreview() {
    CalculadoraCaloriasTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            DailyStreakCard(
                streak = DailyStreak(
                    currentStreakDays = 5,
                    hasBreakfastToday = true,
                    hasLunchToday = true,
                    hasDinnerToday = false
                )
            )
        }
    }
}
