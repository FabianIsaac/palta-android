package com.calculadoracalorias.app.presentation.scan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.calculadoracalorias.app.presentation.edit.EditItemNameDialog
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
    suggestedCategory: MealCategory? = null,
    onNavigateBack: () -> Unit = {},
    onSavedSuccessfully: () -> Unit = {}
) {
    BackHandler { onNavigateBack() }

    val snackbarHostState = remember { SnackbarHostState() }
    var showTextEntrySheet by remember { mutableStateOf(false) }
    var itemToEditName by remember { mutableStateOf<ScannedFoodItem?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }

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

    itemToEditName?.let { item ->
        EditItemNameDialog(
            initialName = item.name,
            onDismiss = { itemToEditName = null },
            onConfirm = { newName ->
                onEvent(FoodScanReviewEvent.OnItemNameChanged(item.id, newName))
                itemToEditName = null
            }
        )
    }

    if (showDatePicker) {
        val initialMillis = remember(uiState.targetDate) {
            uiState.targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
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
                        onEvent(FoodScanReviewEvent.OnTargetDateChanged(selectedDate))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(id = R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDiagnosticDialog && uiState.lastTechnicalError != null) {
        AiDiagnosticDetailsDialog(
            technicalDetails = uiState.lastTechnicalError,
            onDismiss = { showDiagnosticDialog = false }
        )
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
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = if (uiState.isAnalyzingText) {
                            stringResource(id = R.string.loading_interpreting_ai)
                        } else {
                            stringResource(id = R.string.loading_analyzing_image)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.isAnalyzingText && !uiState.lastRawDescription.isNullOrBlank()) {
                            "\"${uiState.lastRawDescription}\""
                        } else {
                            stringResource(id = R.string.loading_analyzing_image_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (uiState.isAnalyzingText && !uiState.lastRawDescription.isNullOrBlank()) {
                            androidx.compose.ui.text.font.FontStyle.Italic
                        } else {
                            androidx.compose.ui.text.font.FontStyle.Normal
                        },
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.loading_nutritional_tip_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.loading_nutritional_tip_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
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

            // Banner informativo de consolidación inteligente (30 min)
            if (uiState.consolidateTargetMealMinutesAgo != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(
                                        id = R.string.consolidation_notice_title,
                                        uiState.selectedCategory.displayName,
                                        uiState.consolidateTargetMealMinutesAgo!!
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(id = R.string.consolidation_notice_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // Selector de Categoría Chilena con íconos y sugerencia horaria
            item {
                MealCategorySelector(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { onEvent(FoodScanReviewEvent.OnMealCategoryChanged(it)) },
                    detectedCategory = suggestedCategory
                )
            }

            // Tarjeta de Borrador y Estado de Error ante fallo de red / IA
            if (uiState.canRetryTextAnalysis && !uiState.lastRawDescription.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(id = R.string.title_error_ai_connection),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.desc_error_ai_draft),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${uiState.lastRawDescription}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (uiState.lastTechnicalError != null) {
                                    TextButton(
                                        onClick = { showDiagnosticDialog = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ver detalle técnico",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showTextEntrySheet = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(stringResource(id = R.string.btn_edit_text))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onEvent(FoodScanReviewEvent.OnRetryNaturalLanguage) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(id = R.string.btn_retry_analysis))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Banner informativo de Estimado Local (Pendiente de refinamiento con IA)
            if (uiState.isPendingAiRefinement) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.badge_pending_ai_refinement),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(id = R.string.desc_pending_ai_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
            // Tarjeta y selector interactivo de fecha objetivo de la comida
            item {
                TargetDateCard(
                    targetDate = uiState.targetDate,
                    onClickChangeDate = { showDatePicker = true }
                )
            }

            item {
                NutritionalSummaryCard(
                    totalCalories = uiState.totalCalories,
                    totalProtein = uiState.totalProtein,
                    totalCarbs = uiState.totalCarbs,
                    totalFat = uiState.totalFat
                )
            }

            // Encabezado de la sección con contador
            item {
                Text(
                    text = "${stringResource(id = R.string.detected_foods_section)} (${uiState.items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Fila de acciones dedicadas para evitar compresión y asegurar legibilidad
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.btn_add_food),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showTextEntrySheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.btn_describe_with_ai),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
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
                    },
                    onEditName = {
                        itemToEditName = item
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

        if (showTextEntrySheet) {
            QuickNaturalLanguageEntrySheet(
                initialText = uiState.lastRawDescription ?: "",
                isAnalyzing = uiState.isAnalyzingText,
                onDismiss = { showTextEntrySheet = false },
                onAnalyzeDescription = { description ->
                    onEvent(FoodScanReviewEvent.OnAnalyzeNaturalLanguage(description))
                    showTextEntrySheet = false
                }
            )
        }
    }
}

fun formatChileanDate(date: LocalDate): String {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val localeSpanish = Locale("es", "CL")
    val dayAndMonth = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", localeSpanish))

    return when (date) {
        today -> "Hoy, $dayAndMonth"
        yesterday -> "Ayer, $dayAndMonth"
        else -> {
            val formatted = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeSpanish))
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeSpanish) else it.toString() }
        }
    }
}

@Composable
fun TargetDateCard(
    targetDate: LocalDate,
    onClickChangeDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = formatChileanDate(targetDate)

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
                        text = stringResource(id = R.string.label_target_date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
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
                    text = stringResource(id = R.string.label_change_date),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
    onRemove: () -> Unit,
    onEditName: () -> Unit = {}
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
                item.householdPortion?.toDisplayText()?.let { portionDisplay ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🥄 $portionDisplay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
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
// Compose Previews (Requisito 4 & Tarea 3.5)
// -------------------------------------------------------------

@Preview(name = "Pantalla de Revisión - Desayuno Chileno con IA", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreen() {
    CalculadoraCaloriasTheme {
        val sampleItems = listOf(
            ScannedFoodItem(
                id = "1",
                name = "Café con leche",
                servingGrams = 200.0,
                caloriesPer100g = 35.0,
                proteinPer100g = 3.4,
                carbsPer100g = 4.8,
                fatPer100g = 0.2,
                householdPortion = com.calculadoracalorias.app.domain.model.HouseholdPortion(
                    unit = com.calculadoracalorias.app.domain.model.HouseholdUnit.CUP,
                    quantity = 1.0,
                    equivalentGrams = 200.0
                )
            ),
            ScannedFoodItem(
                id = "2",
                name = "Azúcar blanca",
                servingGrams = 5.0,
                caloriesPer100g = 387.0,
                proteinPer100g = 0.0,
                carbsPer100g = 100.0,
                fatPer100g = 0.0,
                householdPortion = com.calculadoracalorias.app.domain.model.HouseholdPortion(
                    unit = com.calculadoracalorias.app.domain.model.HouseholdUnit.TEASPOON,
                    quantity = 1.0,
                    equivalentGrams = 5.0
                )
            ),
            ScannedFoodItem(
                id = "3",
                name = "Marraqueta",
                servingGrams = 100.0,
                caloriesPer100g = 280.0,
                proteinPer100g = 9.0,
                carbsPer100g = 56.0,
                fatPer100g = 1.0,
                householdPortion = com.calculadoracalorias.app.domain.model.HouseholdPortion(
                    unit = com.calculadoracalorias.app.domain.model.HouseholdUnit.UNIT,
                    quantity = 1.0,
                    equivalentGrams = 100.0
                )
            ),
            ScannedFoodItem(
                id = "4",
                name = "Palta hass",
                servingGrams = 80.0,
                caloriesPer100g = 160.0,
                proteinPer100g = 2.0,
                carbsPer100g = 9.0,
                fatPer100g = 15.0,
                householdPortion = com.calculadoracalorias.app.domain.model.HouseholdPortion(
                    unit = com.calculadoracalorias.app.domain.model.HouseholdUnit.PORTION,
                    quantity = 1.0,
                    equivalentGrams = 80.0
                )
            )
        )

        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = sampleItems,
                selectedCategory = MealCategory.DESAYUNO,
                totalCalories = 497.4,
                totalProtein = 17.4,
                totalCarbs = 77.8,
                totalFat = 13.4
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

@Preview(name = "Pantalla de Revisión - Con Aviso de Consolidación", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreenConsolidation() {
    CalculadoraCaloriasTheme {
        val sampleItem = ScannedFoodItem(
            id = "w1",
            name = "6 Wantanes fritos",
            servingGrams = 120.0,
            caloriesPer100g = 280.0,
            proteinPer100g = 6.0,
            carbsPer100g = 32.0,
            fatPer100g = 14.0
        )
        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = listOf(sampleItem),
                selectedCategory = MealCategory.ALMUERZO,
                totalCalories = 336.0,
                totalProtein = 7.2,
                totalCarbs = 38.4,
                totalFat = 16.8,
                consolidateTargetMealMinutesAgo = 15
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Pantalla de Revisión - Fecha Pasada (Ayer)", showBackground = true)
@Composable
private fun PreviewFoodScanReviewScreenYesterday() {
    CalculadoraCaloriasTheme {
        val sampleItem = ScannedFoodItem(
            id = "p1",
            name = "Cazuela de vacuno",
            servingGrams = 350.0,
            caloriesPer100g = 95.0,
            proteinPer100g = 7.0,
            carbsPer100g = 8.0,
            fatPer100g = 3.5
        )
        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = listOf(sampleItem),
                selectedCategory = MealCategory.ALMUERZO,
                targetDate = LocalDate.now().minusDays(1),
                totalCalories = 332.5,
                totalProtein = 24.5,
                totalCarbs = 28.0,
                totalFat = 12.25
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Pantalla de Revisión - Error de Conexión IA con Detalle", showBackground = true, widthDp = 360)
@Composable
private fun PreviewFoodScanReviewScreenAiError() {
    CalculadoraCaloriasTheme {
        FoodScanReviewScreen(
            uiState = FoodScanReviewUiState(
                items = emptyList(),
                selectedCategory = MealCategory.ALMUERZO,
                lastRawDescription = "1 empanada de pino al horno y una ensalada a la chilena",
                canRetryTextAnalysis = true,
                lastTechnicalError = com.calculadoracalorias.app.domain.model.AiTechnicalDetails(
                    provider = com.calculadoracalorias.app.domain.model.AiProvider.MINIMAX,
                    endpointUrl = "https://api.minimaxi.chat/v1/chat/completions",
                    model = "MiniMax-M2.7-highspeed",
                    httpStatus = 504,
                    durationMs = 60120L,
                    exceptionMessage = "SocketTimeoutException: timeout after 60000ms"
                )
            ),
            onEvent = {}
        )
    }
}

