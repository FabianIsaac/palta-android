package com.calculadoracalorias.app.presentation.edit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.NutritionLabelScanResult
import com.calculadoracalorias.app.domain.model.ScannedFoodItem
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientSheet(
    onDismiss: () -> Unit,
    onIngredientAdded: (ScannedFoodItem) -> Unit,
    onSearchCatalog: suspend (String) -> List<ScannedFoodItem>,
    onGetPopularFoods: suspend () -> List<ScannedFoodItem>,
    onScanNutritionLabel: (suspend (ByteArray) -> Result<NutritionLabelScanResult>)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_ingredient_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.btn_cancel)
                    )
                }
            }

            // Pestañas (Catálogo chileno / Personalizado)
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_catalog),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_custom),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                CatalogTabContent(
                    onSearchCatalog = onSearchCatalog,
                    onGetPopularFoods = onGetPopularFoods,
                    onAddFood = { item ->
                        onIngredientAdded(item)
                        onDismiss()
                    }
                )
            } else {
                CustomTabContent(
                    onScanNutritionLabel = onScanNutritionLabel,
                    onAddFood = { item ->
                        onIngredientAdded(item)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun CatalogTabContent(
    onSearchCatalog: suspend (String) -> List<ScannedFoodItem>,
    onGetPopularFoods: suspend () -> List<ScannedFoodItem>,
    onAddFood: (ScannedFoodItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ScannedFoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ScannedFoodItem?>(null) }
    var gramsInput by remember { mutableStateOf("100") }

    LaunchedEffect(query) {
        isLoading = true
        if (query.isBlank()) {
            results = onGetPopularFoods()
        } else {
            delay(200) // Debounce para búsqueda fluida
            results = onSearchCatalog(query)
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Barra de búsqueda
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_catalog_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.btn_cancel)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Si hay un alimento seleccionado para ajustar porción
        selectedItem?.let { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            item.householdPortion?.toDisplayText()?.let { portionText ->
                                Text(
                                    text = "🥄 Porción habitual: $portionText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { selectedItem = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_cancel),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = gramsInput,
                            onValueChange = { gramsInput = it },
                            label = { Text(stringResource(R.string.portion_grams_label)) },
                            suffix = { Text(stringResource(R.string.unit_grams)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        val parsedGrams = gramsInput.toDoubleOrNull() ?: 0.0
                        Button(
                            onClick = {
                                if (parsedGrams > 0) {
                                    val finalItem = item.copy(
                                        id = UUID.randomUUID().toString(),
                                        servingGrams = parsedGrams
                                    )
                                    onAddFood(finalItem)
                                }
                            },
                            enabled = parsedGrams > 0,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_add_to_meal))
                        }
                    }

                    val currentGrams = gramsInput.toDoubleOrNull() ?: 0.0
                    if (currentGrams > 0) {
                        val cals = (item.caloriesPer100g * currentGrams) / 100.0
                        val prot = (item.proteinPer100g * currentGrams) / 100.0
                        val carb = (item.carbsPer100g * currentGrams) / 100.0
                        val fat = (item.fatPer100g * currentGrams) / 100.0
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%.1f kcal  •  P: %.1fg  C: %.1fg  G: %.1fg".format(
                                Locale.getDefault(), cals, prot, carb, fat
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Título de lista
        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.popular_ingredients_title)
            } else {
                stringResource(R.string.detected_foods_section)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    CatalogFoodRow(
                        item = item,
                        isSelected = selectedItem?.id == item.id,
                        onClick = {
                            selectedItem = item
                            val defaultGrams = if (item.servingGrams > 0.0) item.servingGrams else 100.0
                            gramsInput = if (defaultGrams % 1.0 == 0.0) defaultGrams.toInt().toString() else defaultGrams.toString()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogFoodRow(
    item: ScannedFoodItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%.0f kcal  •  P: %.1fg  C: %.1fg  G: %.1fg (por 100g)".format(
                        Locale.getDefault(),
                        item.caloriesPer100g,
                        item.proteinPer100g,
                        item.carbsPer100g,
                        item.fatPer100g
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomTabContent(
    onScanNutritionLabel: (suspend (ByteArray) -> Result<NutritionLabelScanResult>)? = null,
    onAddFood: (ScannedFoodItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var gramsText by remember { mutableStateOf("100") }
    var caloriesPer100gText by remember { mutableStateOf("") }
    var proteinPer100gText by remember { mutableStateOf("") }
    var carbsPer100gText by remember { mutableStateOf("") }
    var fatPer100gText by remember { mutableStateOf("") }

    var isScanning by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    fun processImageBytes(bytes: ByteArray) {
        if (onScanNutritionLabel == null) return
        isScanning = true
        scanError = null
        coroutineScope.launch {
            val result = onScanNutritionLabel(bytes)
            isScanning = false
            if (result.isSuccess) {
                val scan = result.getOrThrow()
                if (!scan.productName.isNullOrBlank()) {
                    name = scan.productName
                }
                val servingGrams = scan.servingGrams ?: 100.0
                gramsText = if (servingGrams % 1.0 == 0.0) servingGrams.toInt().toString() else "%.1f".format(Locale.US, servingGrams)

                val calsPer100g = if (servingGrams > 0) (scan.calories * 100.0) / servingGrams else scan.calories
                val protPer100g = if (servingGrams > 0) (scan.proteinGrams * 100.0) / servingGrams else scan.proteinGrams
                val carbsPer100g = if (servingGrams > 0) (scan.carbsGrams * 100.0) / servingGrams else scan.carbsGrams
                val fatPer100g = if (servingGrams > 0) (scan.fatGrams * 100.0) / servingGrams else scan.fatGrams

                caloriesPer100gText = if (calsPer100g % 1.0 == 0.0) calsPer100g.toInt().toString() else "%.1f".format(Locale.US, calsPer100g)
                proteinPer100gText = if (protPer100g % 1.0 == 0.0) protPer100g.toInt().toString() else "%.1f".format(Locale.US, protPer100g)
                carbsPer100gText = if (carbsPer100g % 1.0 == 0.0) carbsPer100g.toInt().toString() else "%.1f".format(Locale.US, carbsPer100g)
                fatPer100gText = if (fatPer100g % 1.0 == 0.0) fatPer100g.toInt().toString() else "%.1f".format(Locale.US, fatPer100g)
            } else {
                scanError = result.exceptionOrNull()?.message ?: "No se pudo interpretar la tabla nutricional. Revisa la imagen o ingresa los datos manualmente."
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            processImageBytes(stream.toByteArray())
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
            if (bytes != null) {
                processImageBytes(bytes)
            }
        }
    }

    val isValidName = name.trim().isNotBlank()
    val grams = gramsText.toDoubleOrNull() ?: 0.0
    val isValidGrams = grams > 0.0
    val canSubmit = isValidName && isValidGrams

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Botón opcional de Escanear Tabla Nutricional
        if (onScanNutritionLabel != null) {
            OutlinedButton(
                onClick = { showImageSourceDialog = true },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_scanning_label))
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_scan_nutrition_label))
                }
            }
        }

        scanError?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.custom_food_name_label)) },
            placeholder = { Text(stringResource(R.string.custom_food_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it },
                label = { Text(stringResource(R.string.portion_grams_label)) },
                suffix = { Text(stringResource(R.string.unit_grams)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = caloriesPer100gText,
                onValueChange = { caloriesPer100gText = it },
                label = { Text(stringResource(R.string.custom_food_calories_label)) },
                suffix = { Text(stringResource(R.string.unit_kcal)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = proteinPer100gText,
                onValueChange = { proteinPer100gText = it },
                label = { Text(stringResource(R.string.custom_food_protein_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = carbsPer100gText,
                onValueChange = { carbsPer100gText = it },
                label = { Text(stringResource(R.string.custom_food_carbs_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = fatPer100gText,
                onValueChange = { fatPer100gText = it },
                label = { Text(stringResource(R.string.custom_food_fat_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (canSubmit) {
                    val cals = caloriesPer100gText.toDoubleOrNull() ?: 0.0
                    val prot = proteinPer100gText.toDoubleOrNull() ?: 0.0
                    val carbs = carbsPer100gText.toDoubleOrNull() ?: 0.0
                    val fat = fatPer100gText.toDoubleOrNull() ?: 0.0

                    val item = ScannedFoodItem(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        servingGrams = grams,
                        caloriesPer100g = cals,
                        proteinPer100g = prot,
                        carbsPer100g = carbs,
                        fatPer100g = fat
                    )
                    onAddFood(item)
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.btn_add_to_meal))
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(id = R.string.dialog_select_image_source_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.btn_take_photo_camera))
                    }

                    OutlinedButton(
                        onClick = {
                            showImageSourceDialog = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.btn_pick_gallery))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text(stringResource(id = R.string.btn_dialog_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AddIngredientSheetPreview() {
    CalculadoraCaloriasTheme {
        AddIngredientSheet(
            onDismiss = {},
            onIngredientAdded = {},
            onSearchCatalog = { emptyList() },
            onGetPopularFoods = {
                listOf(
                    ScannedFoodItem(
                        id = "1",
                        name = "Palta hass",
                        servingGrams = 50.0,
                        caloriesPer100g = 160.0,
                        proteinPer100g = 2.0,
                        carbsPer100g = 9.0,
                        fatPer100g = 15.0
                    )
                )
            }
        )
    }
}
