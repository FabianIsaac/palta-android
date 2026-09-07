package com.calculadoracalorias.app.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.MealCategory
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodScanReviewScreen(
    uiState: FoodScanReviewUiState,
    onEvent: (FoodScanReviewEvent) -> Unit,
    onNavigateBack: () -> Unit = {},
    onSavedSuccessfully: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onEvent(FoodScanReviewEvent.OnDismissError)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSavedSuccessfully()
            onEvent(FoodScanReviewEvent.OnDismissSuccess)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_review_meal)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { onEvent(FoodScanReviewEvent.OnConfirmAndSave) },
                        enabled = !uiState.isSaving && !uiState.isLoading && uiState.items.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.btn_confirm_save),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.loading_analyzing_image),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subtítulo explicativo
            item {
                Text(
                    text = stringResource(id = R.string.review_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Selector de Categoría Chilena
            item {
                Text(
                    text = stringResource(id = R.string.category_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MealCategory.entries.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { onEvent(FoodScanReviewEvent.OnMealCategoryChanged(category)) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }

            // Resumen Nutricional
            item {
                NutritionalSummaryCard(
                    totalCalories = uiState.totalCalories,
                    totalProtein = uiState.totalProtein,
                    totalCarbs = uiState.totalCarbs,
                    totalFat = uiState.totalFat
                )
            }

            // Encabezado de la lista de alimentos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.detected_foods_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            val newItem = ScannedFoodItem(
                                id = UUID.randomUUID().toString(),
                                name = "Nuevo alimento",
                                servingGrams = 100.0,
                                caloriesPer100g = 100.0,
                                proteinPer100g = 5.0,
                                carbsPer100g = 15.0,
                                fatPer100g = 2.0
                            )
                            onEvent(FoodScanReviewEvent.OnItemAdded(newItem))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(id = R.string.btn_add_food))
                    }
                }
            }

            // Lista de ítems detectados
            items(uiState.items, key = { it.id }) { item ->
                ScannedFoodItemCard(
                    item = item,
                    onPortionChanged = { newGrams ->
                        onEvent(FoodScanReviewEvent.OnPortionChanged(item.id, newGrams))
                    },
                    onRemove = {
                        onEvent(FoodScanReviewEvent.OnItemRemoved(item.id))
                    }
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_items_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun NutritionalSummaryCard(
    totalCalories: Double,
    totalProtein: Double,
    totalCarbs: Double,
    totalFat: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.label_total_calories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$totalCalories kcal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroColumn(
                    name = stringResource(id = R.string.label_protein),
                    amount = "$totalProtein g",
                    color = MaterialTheme.colorScheme.primary
                )
                MacroColumn(
                    name = stringResource(id = R.string.label_carbs),
                    amount = "$totalCarbs g",
                    color = MaterialTheme.colorScheme.tertiary
                )
                MacroColumn(
                    name = stringResource(id = R.string.label_fat),
                    amount = "$totalFat g",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MacroColumn(name: String, amount: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ScannedFoodItemCard(
    item: ScannedFoodItem,
    onPortionChanged: (Double) -> Unit,
    onRemove: () -> Unit
) {
    var textValue by remember(item.servingGrams) {
        mutableStateOf(item.servingGrams.toString())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.totalCalories} kcal  •  P: ${item.totalProtein}g  C: ${item.totalCarbs}g  G: ${item.totalFat}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Campo para porción en gramos
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        onPortionChanged(parsed)
                    }
                },
                modifier = Modifier.width(95.dp),
                suffix = { Text(stringResource(id = R.string.unit_grams)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.btn_delete_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Compose Previews (Requisito 4.8)
// -------------------------------------------------------------

@Preview(name = "Pantalla de Revisión - Lista Editable", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreen() {
    CalculadoraCaloriasTheme {
        val sampleItems = listOf(
            ScannedFoodItem(
                id = "1",
                name = "Pechuga de pollo a la plancha",
                servingGrams = 150.0,
                caloriesPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6
            ),
            ScannedFoodItem(
                id = "2",
                name = "Palta hass",
                servingGrams = 100.0,
                caloriesPer100g = 160.0,
                proteinPer100g = 2.0,
                carbsPer100g = 9.0,
                fatPer100g = 15.0
            ),
            ScannedFoodItem(
                id = "3",
                name = "Arroz blanco cocido",
                servingGrams = 150.0,
                caloriesPer100g = 130.0,
                proteinPer100g = 2.7,
                carbsPer100g = 28.2,
                fatPer100g = 0.3
            )
        )

        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = sampleItems,
                selectedCategory = MealCategory.ALMUERZO,
                totalCalories = 602.5,
                totalProtein = 52.55,
                totalCarbs = 51.3,
                totalFat = 20.85
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Pantalla de Revisión - Cargando Análisis", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreenLoading() {
    CalculadoraCaloriasTheme {
        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                isLoading = true
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Pantalla de Revisión - Lista Vacía / Error", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreenEmpty() {
    CalculadoraCaloriasTheme {
        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = emptyList(),
                selectedCategory = MealCategory.ONCE_CENA,
                errorMessage = "No se detectaron alimentos en la foto."
            ),
            onEvent = {}
        )
    }
}
