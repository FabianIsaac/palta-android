package com.calculadoracalorias.app.presentation.scan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

/**
 * Retorna el emoji culturalmente representativo para cada comida en Chile.
 */
fun MealCategory.toEmoji(): String {
    return when (this) {
        MealCategory.DESAYUNO -> "☕"
        MealCategory.ALMUERZO -> "🍲"
        MealCategory.ONCE_CENA -> "🫖"
        MealCategory.COLACIONES -> "🍎"
    }
}

/**
 * Selector compacto y responsivo de categoría de comida chilena.
 * Muestra la categoría actual, accesos rápidos de un toque y sugerencia inteligente según el horario.
 */
@Composable
fun MealCategorySelector(
    selectedCategory: MealCategory,
    onCategorySelected: (MealCategory) -> Unit,
    detectedCategory: MealCategory? = null,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Fila superior: Título de la sección y chip contextual de detección automática
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.category_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (detectedCategory != null && detectedCategory == selectedCategory) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🕒 Sugerido por horario",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector segmentado de 1 toque con scroll horizontal de resguardo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory
                    val isSuggested = category == detectedCategory

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        label = "pill_background"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        label = "pill_content"
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategorySelected(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = backgroundColor,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else if (isSuggested) 1.2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else if (isSuggested) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = category.toEmoji(),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "MealCategorySelector Preview", showBackground = true)
@Composable
private fun MealCategorySelectorPreview() {
    CalculadoraCaloriasTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MealCategorySelector(
                selectedCategory = MealCategory.ALMUERZO,
                onCategorySelected = {},
                detectedCategory = MealCategory.ALMUERZO
            )
            Spacer(modifier = Modifier.height(16.dp))
            MealCategorySelector(
                selectedCategory = MealCategory.DESAYUNO,
                onCategorySelected = {},
                detectedCategory = MealCategory.ALMUERZO
            )
        }
    }
}
