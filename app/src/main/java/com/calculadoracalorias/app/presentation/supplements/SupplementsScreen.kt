package com.calculadoracalorias.app.presentation.supplements

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.Supplement
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupplementsScreen(
    uiState: SupplementsUiState,
    onEvent: (SupplementsEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var supplementToDelete by remember { mutableStateOf<Supplement?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onEvent(SupplementsEvent.OnDismissMessage)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            onEvent(SupplementsEvent.OnDismissMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_supplements),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 96.dp)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // SECCIÓN 1: Sugerencias Populares de 1 toque
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.supplements_suggestions_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(id = R.string.supplements_suggestions_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Supplement.PRECONFIGURED_SUPPLEMENTS.forEach { suggestion ->
                                val existingInCatalog = uiState.supplements.find { it.id == suggestion.id }
                                val isActive = existingInCatalog?.isActive == true

                                FilterChip(
                                    selected = isActive,
                                    onClick = { onEvent(SupplementsEvent.OnToggleSuggestion(suggestion)) },
                                    label = { Text("${suggestion.name} (${suggestion.dosageDescription})") },
                                    leadingIcon = if (isActive) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                // SECCIÓN 2: Agregar suplemento personalizado asistido con IA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.supplements_add_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.supplements_add_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Nombre del suplemento
                        OutlinedTextField(
                            value = uiState.inputName,
                            onValueChange = { onEvent(SupplementsEvent.OnInputNameChanged(it)) },
                            label = { Text(stringResource(id = R.string.supplements_name_label)) },
                            placeholder = { Text(stringResource(id = R.string.supplements_name_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón Estimar con IA
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onEvent(SupplementsEvent.OnEstimateClicked)
                            },
                            enabled = uiState.inputName.isNotBlank() && !uiState.isEstimating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isEstimating) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.btn_estimating_with_ai))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.btn_estimate_with_ai))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dosis habitual
                        OutlinedTextField(
                            value = uiState.inputDosage,
                            onValueChange = { onEvent(SupplementsEvent.OnInputDosageChanged(it)) },
                            label = { Text(stringResource(id = R.string.supplements_dosage_label)) },
                            placeholder = { Text(stringResource(id = R.string.supplements_dosage_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fila de Macros: Calorías y Proteína
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.inputCalories,
                                onValueChange = { onEvent(SupplementsEvent.OnInputCaloriesChanged(it)) },
                                label = { Text(stringResource(id = R.string.supplements_calories_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.inputProtein,
                                onValueChange = { onEvent(SupplementsEvent.OnInputProteinChanged(it)) },
                                label = { Text(stringResource(id = R.string.supplements_protein_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fila de Macros: Carbohidratos y Grasas
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.inputCarbs,
                                onValueChange = { onEvent(SupplementsEvent.OnInputCarbsChanged(it)) },
                                label = { Text(stringResource(id = R.string.supplements_carbs_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = uiState.inputFat,
                                onValueChange = { onEvent(SupplementsEvent.OnInputFatChanged(it)) },
                                label = { Text(stringResource(id = R.string.supplements_fat_label)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        onEvent(SupplementsEvent.OnSaveClicked)
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón Guardar
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onEvent(SupplementsEvent.OnSaveClicked)
                            },
                            enabled = uiState.inputName.isNotBlank() && !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(stringResource(id = R.string.btn_save_supplement))
                            }
                        }
                    }
                }
            }

            item {
                // TÍTULO SECCIÓN 3: Rutina configurada
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.supplements_active_routine_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.supplements_active_routine_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.supplements.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.supplements_empty_routine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(uiState.supplements, key = { it.id }) { supplement ->
                    SupplementItemCard(
                        supplement = supplement,
                        onToggleActive = { isActive ->
                            onEvent(SupplementsEvent.OnToggleActive(supplement.id, isActive))
                        },
                        onDelete = {
                            supplementToDelete = supplement
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    supplementToDelete?.let { supplement ->
        AlertDialog(
            onDismissRequest = { supplementToDelete = null },
            title = { Text(stringResource(id = R.string.dialog_delete_supplement_title)) },
            text = { Text(stringResource(id = R.string.dialog_delete_supplement_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        onEvent(SupplementsEvent.OnDeleteSupplement(supplement.id))
                        supplementToDelete = null
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete_supplement_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { supplementToDelete = null }) {
                    Text(stringResource(id = R.string.btn_dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SupplementItemCard(
    supplement: Supplement,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (supplement.isActive) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (supplement.isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = if (supplement.isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplement.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${supplement.dosageDescription} • ${supplement.formatNutritionSummary()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Switch para activar/desactivar en la rutina
            Switch(
                checked = supplement.isActive,
                onCheckedChange = onToggleActive
            )

            // Botón eliminar para personalizados
            if (supplement.isCustom) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.btn_delete_item),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun Supplement.formatNutritionSummary(): String {
    return if (calories > 0.0 || proteinGrams > 0.0 || carbsGrams > 0.0 || fatGrams > 0.0) {
        "${calories.toInt()} kcal (${proteinGrams.toInt()}g P / ${carbsGrams.toInt()}g C / ${fatGrams.toInt()}g G)"
    } else {
        "0 kcal"
    }
}

@Preview(name = "Modo Claro", showBackground = true)
@Composable
private fun SupplementsScreenLightPreview() {
    CalculadoraCaloriasTheme(darkTheme = false) {
        SupplementsScreen(
            uiState = SupplementsUiState(
                supplements = Supplement.PRECONFIGURED_SUPPLEMENTS,
                inputName = "Proteína Whey",
                inputDosage = "1 scoop (30g)",
                inputCalories = "120",
                inputProtein = "24",
                inputCarbs = "2",
                inputFat = "1.5"
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Modo Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SupplementsScreenDarkPreview() {
    CalculadoraCaloriasTheme(darkTheme = true) {
        SupplementsScreen(
            uiState = SupplementsUiState(
                supplements = Supplement.PRECONFIGURED_SUPPLEMENTS
            ),
            onEvent = {}
        )
    }
}
