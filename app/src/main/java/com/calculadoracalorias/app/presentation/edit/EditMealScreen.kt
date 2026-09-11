package com.calculadoracalorias.app.presentation.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import com.calculadoracalorias.app.presentation.scan.formatChileanDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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
import com.calculadoracalorias.app.domain.model.NutritionSummary
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.presentation.scan.NutritionalSummaryCard
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditMealScreen(
    uiState: EditMealUiState,
    onEvent: (EditMealEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchCatalog: (suspend (String) -> List<ScannedFoodItem>)? = null,
    onGetPopularFoods: (suspend () -> List<ScannedFoodItem>)? = null,
    onScanNutritionLabel: (suspend (ByteArray) -> Result<com.calculadoracalorias.app.domain.model.NutritionLabelScanResult>)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showAddIngredientSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onEvent(EditMealEvent.OnDismiss)
        }
    }

    LaunchedEffect(uiState.isSavedSuccessfully, uiState.isDeletedSuccessfully) {
        if (uiState.isSavedSuccessfully || uiState.isDeletedSuccessfully) {
            onNavigateBack()
        }
    }

    editingItemIndex?.let { index ->
        if (index in uiState.items.indices) {
            val currentItem = uiState.items[index]
            EditItemNameDialog(
                initialName = currentItem.name,
                onDismiss = { editingItemIndex = null },
                onConfirm = { newName ->
                    onEvent(EditMealEvent.OnItemNameChanged(index, newName))
                    editingItemIndex = null
                }
            )
        }
    }

    if (showAddIngredientSheet) {
        AddIngredientSheet(
            onDismiss = { showAddIngredientSheet = false },
            onIngredientAdded = { newItem ->
                onEvent(EditMealEvent.OnItemAdded(newItem))
            },
            onSearchCatalog = onSearchCatalog ?: { emptyList() },
            onGetPopularFoods = onGetPopularFoods ?: { emptyList() }
        )
    }

    if (showDatePicker) {
        val currentLocalDate = remember(uiState.timestamp) {
            if (uiState.timestamp > 0L) {
                Instant.ofEpochMilli(uiState.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                LocalDate.now()
            }
        }
        val initialMillis = remember(currentLocalDate) {
            currentLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onEvent(EditMealEvent.OnDateChanged(selectedDate))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_dialog_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onEvent(EditMealEvent.OnDismissDeleteDialog) },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_meal_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.dialog_delete_meal_msg))
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(EditMealEvent.OnConfirmDeleteClicked) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.btn_dialog_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(EditMealEvent.OnDismissDeleteDialog) }) {
                    Text(text = stringResource(R.string.btn_dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_edit_meal),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_dialog_cancel)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(EditMealEvent.OnDeleteMealClicked) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete_meal),
                            tint = MaterialTheme.colorScheme.error
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
                        onClick = { onEvent(EditMealEvent.OnSaveMealClicked) },
                        enabled = !uiState.isSaving && !uiState.isDeleting && !uiState.isLoading && uiState.items.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving || uiState.isDeleting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_save_changes),
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
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
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
            item {
                Text(
                    text = stringResource(R.string.edit_meal_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Categoría
            item {
                Text(
                    text = stringResource(R.string.category_label),
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
                            selected = uiState.category == category,
                            onClick = { onEvent(EditMealEvent.OnCategoryChanged(category)) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
            }

            // Fecha de la comida con opción para cambiarla
            item {
                EditMealDateCard(
                    timestamp = uiState.timestamp,
                    onClickChangeDate = { showDatePicker = true }
                )
            }

            // Resumen Nutricional Recalculado
            item {
                NutritionalSummaryCard(
                    totalCalories = uiState.summary.totalCalories,
                    totalProtein = uiState.summary.totalProtein,
                    totalCarbs = uiState.summary.totalCarbs,
                    totalFat = uiState.summary.totalFat
                )
            }

            // Encabezado Alimentos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.detected_foods_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddIngredientSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.btn_add_ingredient))
                    }
                }
            }

            // Lista editable de alimentos
            itemsIndexed(uiState.items, key = { index, item -> item.id + "_$index" }) { index, item ->
                EditableFoodItemRow(
                    item = item,
                    onPortionChanged = { newGrams ->
                        onEvent(EditMealEvent.OnServingGramsChanged(index, newGrams))
                    },
                    onRemove = {
                        onEvent(EditMealEvent.OnRemoveItem(index))
                    },
                    onEditName = {
                        editingItemIndex = index
                    }
                )
            }

            // Botón prominente de Agregar Ingrediente
            item {
                OutlinedButton(
                    onClick = { showAddIngredientSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_add_ingredient),
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                            text = stringResource(R.string.empty_items_hint),
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
fun EditableFoodItemRow(
    item: ScannedFoodItem,
    onPortionChanged: (Double) -> Unit,
    onRemove: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(item.servingGrams) {
        mutableStateOf(if (item.servingGrams % 1.0 == 0.0) item.servingGrams.toInt().toString() else item.servingGrams.toString())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onEditName() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit_name),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.1f kcal  •  P: %.1fg  C: %.1fg  G: %.1fg".format(
                        Locale.getDefault(),
                        item.totalCalories,
                        item.totalProtein,
                        item.totalCarbs,
                        item.totalFat
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed >= 0.0) {
                        onPortionChanged(parsed)
                    }
                },
                modifier = Modifier.width(100.dp),
                suffix = { Text(stringResource(R.string.unit_grams)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete_item),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EditMealDateCard(
    timestamp: Long,
    onClickChangeDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneId = ZoneId.systemDefault()
    val formattedDateTime = remember(timestamp) {
        if (timestamp > 0L) {
            val zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId)
            val dateStr = formatChileanDate(zonedDateTime.toLocalDate())
            val timeStr = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            "$dateStr • $timeStr"
        } else {
            val now = LocalDate.now()
            formatChileanDate(now)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClickChangeDate),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
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
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.label_target_date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            TextButton(
                onClick = onClickChangeDate,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.label_change_date),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(name = "EditMealScreen Preview - Hoy", showBackground = true)
@Composable
fun EditMealScreenPreview() {
    val sampleItem = ScannedFoodItem(
        id = "1",
        name = "Pan marraqueta con palta",
        servingGrams = 120.0,
        caloriesPer100g = 250.0,
        proteinPer100g = 8.0,
        carbsPer100g = 45.0,
        fatPer100g = 5.0
    )
    val state = EditMealUiState(
        mealId = 1L,
        category = MealCategory.DESAYUNO,
        timestamp = System.currentTimeMillis(),
        items = listOf(sampleItem),
        summary = NutritionSummary(300.0, 9.6, 54.0, 6.0),
        isLoading = false
    )

    CalculadoraCaloriasTheme {
        Surface {
            EditMealScreen(
                uiState = state,
                onEvent = {},
                onNavigateBack = {}
            )
        }
    }
}

@Preview(name = "EditMealScreen Preview - Ayer", showBackground = true)
@Composable
fun EditMealScreenYesterdayPreview() {
    val sampleItem = ScannedFoodItem(
        id = "2",
        name = "Charquicán con huevo frito",
        servingGrams = 300.0,
        caloriesPer100g = 140.0,
        proteinPer100g = 8.5,
        carbsPer100g = 16.0,
        fatPer100g = 5.2
    )
    val yesterdayTimestamp = LocalDate.now().minusDays(1)
        .atTime(13, 30)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val state = EditMealUiState(
        mealId = 2L,
        category = MealCategory.ALMUERZO,
        timestamp = yesterdayTimestamp,
        items = listOf(sampleItem),
        summary = NutritionSummary(420.0, 25.5, 48.0, 15.6),
        isLoading = false
    )

    CalculadoraCaloriasTheme {
        Surface {
            EditMealScreen(
                uiState = state,
                onEvent = {},
                onNavigateBack = {}
            )
        }
    }
}
