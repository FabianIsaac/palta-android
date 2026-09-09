package com.calculadoracalorias.app.presentation.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.calculadoracalorias.app.domain.model.AiProvider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.domain.model.VisionSource
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.calculadoracalorias.app.presentation.settings.components.BackupAndRestoreSection
import com.calculadoracalorias.app.presentation.settings.components.RestoreBackupConfirmationDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.draw.clip
import com.calculadoracalorias.app.domain.model.MealTimeWindows
import java.time.LocalTime
import java.util.Locale

private enum class TimePickerTarget {
    BREAKFAST_START,
    BREAKFAST_END,
    LUNCH_START,
    LUNCH_END,
    DINNER_START,
    DINNER_END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentAiProvider: AiProvider = AiProvider.NVIDIA_NIM,
    currentApiKey: String = "",
    currentApiKeyNvidia: String = "",
    currentApiKeyGemini: String = "",
    currentApiKeyMiniMax: String = "",
    currentApiKeyCustom: String = "",
    currentCustomEndpointUrl: String = "",
    currentCustomTextModel: String = "",
    currentCustomVisionModel: String = "",
    currentVisionSource: VisionSource = VisionSource.LOCAL_DEVICE,
    currentHealthSyncEnabled: Boolean = true,
    currentMealTimeWindows: MealTimeWindows = MealTimeWindows(),
    currentTargetCalories: Double = 2000.0,
    currentTargetProteinGrams: Double = 150.0,
    currentTargetCarbsGrams: Double = 200.0,
    currentTargetFatGrams: Double = 65.0,
    isHealthConnectAvailable: Boolean = true,
    hasHealthConnectPermission: Boolean = false,
    onRequestHealthConnectPermission: () -> Unit = {},
    onSelectAiProvider: (AiProvider) -> Unit = {},
    onSaveApiKeyForProvider: (AiProvider, String) -> Unit = { _, _ -> },
    onSaveCustomAiParameters: (endpointUrl: String, textModel: String, visionModel: String) -> Unit = { _, _, _ -> },
    onSaveSettings: (apiKey: String, source: VisionSource, syncEnabled: Boolean) -> Unit = { _, _, _ -> },
    onSaveMealTimeWindows: (windows: MealTimeWindows) -> Unit = {},
    onSaveDailyBudget: (calories: Double, protein: Double, carbs: Double, fat: Double) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    lastBackupTimestamp: Long? = null,
    autoBackupEnabled: Boolean = false,
    autoBackupFolderUri: String? = null,
    autoBackupFolderName: String? = null,
    lastAutoBackupTimestamp: Long? = null,
    isExporting: Boolean = false,
    isImporting: Boolean = false,
    pendingRestorePreview: BackupPreviewInfo? = null,
    onToggleAutoBackup: (Boolean) -> Unit = {},
    onSelectAutoBackupFolder: (Uri, String?) -> Unit = { _, _ -> },
    onUnlinkAutoBackupFolder: () -> Unit = {},
    onExportBackup: (Uri) -> Unit = {},
    onSelectBackupFile: (Uri) -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onDismissRestoreDialog: () -> Unit = {},
    userMessage: String? = null,
    errorMessage: String? = null,
    onClearUserMessage: () -> Unit = {},
    onClearErrorMessage: () -> Unit = {}
) {
    BackHandler { onNavigateBack() }

    val context = androidx.compose.ui.platform.LocalContext.current

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportBackup(uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onSelectBackupFile(uri)
        }
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val folderName = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)?.name
            onSelectAutoBackupFolder(uri, folderName)
        }
    }

    var selectedAiProvider by remember(currentAiProvider) { mutableStateOf(currentAiProvider) }
    var apiKeyNvidia by remember(currentApiKeyNvidia) { mutableStateOf(currentApiKeyNvidia) }
    var apiKeyGemini by remember(currentApiKeyGemini) { mutableStateOf(currentApiKeyGemini) }
    var apiKeyMiniMax by remember(currentApiKeyMiniMax, currentApiKey) {
        mutableStateOf(if (currentApiKeyMiniMax.isNotBlank()) currentApiKeyMiniMax else currentApiKey)
    }
    var apiKeyCustom by remember(currentApiKeyCustom) { mutableStateOf(currentApiKeyCustom) }
    var customEndpointUrl by remember(currentCustomEndpointUrl) { mutableStateOf(currentCustomEndpointUrl) }
    var customTextModel by remember(currentCustomTextModel) { mutableStateOf(currentCustomTextModel) }
    var customVisionModel by remember(currentCustomVisionModel) { mutableStateOf(currentCustomVisionModel) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    var selectedVisionSource by remember(currentVisionSource) { mutableStateOf(currentVisionSource) }
    var isHealthSyncEnabled by remember(currentHealthSyncEnabled) { mutableStateOf(currentHealthSyncEnabled) }
    var mealTimeWindows by remember(currentMealTimeWindows) { mutableStateOf(currentMealTimeWindows) }
    var activeTimePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var targetCaloriesText by remember(currentTargetCalories) {
        mutableStateOf(if (currentTargetCalories > 0) "%.0f".format(Locale.getDefault(), currentTargetCalories) else "2000")
    }
    var targetProteinText by remember(currentTargetProteinGrams) {
        mutableStateOf(if (currentTargetProteinGrams > 0) "%.0f".format(Locale.getDefault(), currentTargetProteinGrams) else "150")
    }
    var targetCarbsText by remember(currentTargetCarbsGrams) {
        mutableStateOf(if (currentTargetCarbsGrams > 0) "%.0f".format(Locale.getDefault(), currentTargetCarbsGrams) else "200")
    }
    var targetFatText by remember(currentTargetFatGrams) {
        mutableStateOf(if (currentTargetFatGrams > 0) "%.0f".format(Locale.getDefault(), currentTargetFatGrams) else "65")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val savedSuccessMessage = stringResource(id = R.string.msg_settings_saved)
    var isSavedFeedbackActive by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage)
            onClearUserMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearErrorMessage()
        }
    }

    if (pendingRestorePreview != null) {
        RestoreBackupConfirmationDialog(
            previewInfo = pendingRestorePreview,
            onConfirm = onConfirmRestore,
            onDismiss = onDismissRestoreDialog
        )
    }

    if (activeTimePickerTarget != null) {
        val initialTime = when (activeTimePickerTarget) {
            TimePickerTarget.BREAKFAST_START -> mealTimeWindows.breakfastStart
            TimePickerTarget.BREAKFAST_END -> mealTimeWindows.breakfastEnd
            TimePickerTarget.LUNCH_START -> mealTimeWindows.lunchStart
            TimePickerTarget.LUNCH_END -> mealTimeWindows.lunchEnd
            TimePickerTarget.DINNER_START -> mealTimeWindows.dinnerStart
            TimePickerTarget.DINNER_END -> mealTimeWindows.dinnerEnd
            null -> LocalTime.now()
        }

        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { activeTimePickerTarget = null },
            confirmButton = {
                Button(
                    onClick = {
                        val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        mealTimeWindows = when (activeTimePickerTarget) {
                            TimePickerTarget.BREAKFAST_START -> mealTimeWindows.copy(breakfastStart = newTime)
                            TimePickerTarget.BREAKFAST_END -> mealTimeWindows.copy(breakfastEnd = newTime)
                            TimePickerTarget.LUNCH_START -> mealTimeWindows.copy(lunchStart = newTime)
                            TimePickerTarget.LUNCH_END -> mealTimeWindows.copy(lunchEnd = newTime)
                            TimePickerTarget.DINNER_START -> mealTimeWindows.copy(dinnerStart = newTime)
                            TimePickerTarget.DINNER_END -> mealTimeWindows.copy(dinnerEnd = newTime)
                            null -> mealTimeWindows
                        }
                        activeTimePickerTarget = null
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { activeTimePickerTarget = null }) {
                    Text("Cancelar")
                }
            },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_select_time_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.title_settings)) },
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 96.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Meta Nutricional Diaria (Calorías y Macronutrientes)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_daily_budget_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(id = R.string.settings_daily_budget_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = targetCaloriesText,
                        onValueChange = { targetCaloriesText = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(id = R.string.label_target_calories_input)) },
                        trailingIcon = { Text("kcal", modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetProteinText,
                            onValueChange = { targetProteinText = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(id = R.string.label_target_protein_input)) },
                            trailingIcon = { Text("g", modifier = Modifier.padding(end = 4.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetCarbsText,
                            onValueChange = { targetCarbsText = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(id = R.string.label_target_carbs_input)) },
                            trailingIcon = { Text("g", modifier = Modifier.padding(end = 4.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetFatText,
                            onValueChange = { targetFatText = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(id = R.string.label_target_fat_input)) },
                            trailingIcon = { Text("g", modifier = Modifier.padding(end = 4.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val pVal = targetProteinText.toDoubleOrNull() ?: 0.0
                    val cVal = targetCarbsText.toDoubleOrNull() ?: 0.0
                    val fVal = targetFatText.toDoubleOrNull() ?: 0.0
                    val calFromMacros = (pVal * 4.0 + cVal * 4.0 + fVal * 9.0).toInt()
                    val targetCalInt = (targetCaloriesText.toDoubleOrNull() ?: 0.0).toInt()
                    val diffKcal = calFromMacros - targetCalInt

                    val coherenceText = if (diffKcal == 0) {
                        stringResource(id = R.string.hint_macro_calories_sum, calFromMacros, stringResource(id = R.string.label_macros_match))
                    } else {
                        stringResource(id = R.string.hint_macro_calories_sum, calFromMacros, stringResource(id = R.string.label_macros_diff, diffKcal))
                    }

                    Text(
                        text = coherenceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diffKcal == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val cal = targetCaloriesText.toDoubleOrNull() ?: 2000.0
                            val p = kotlin.math.round((cal * 0.30) / 4.0).toInt()
                            val c = kotlin.math.round((cal * 0.40) / 4.0).toInt()
                            val f = kotlin.math.round((cal * 0.30) / 9.0).toInt()
                            targetProteinText = p.toString()
                            targetCarbsText = c.toString()
                            targetFatText = f.toString()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_auto_balance_macros))
                    }
                }
            }

            // Sección de Horarios Habituales de Comidas
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_meal_windows_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(id = R.string.settings_meal_windows_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MealTimeRow(
                        mealIcon = "☕",
                        mealName = "Desayuno",
                        startTime = mealTimeWindows.breakfastStart,
                        endTime = mealTimeWindows.breakfastEnd,
                        onPickStart = { activeTimePickerTarget = TimePickerTarget.BREAKFAST_START },
                        onPickEnd = { activeTimePickerTarget = TimePickerTarget.BREAKFAST_END }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MealTimeRow(
                        mealIcon = "🍲",
                        mealName = "Almuerzo",
                        startTime = mealTimeWindows.lunchStart,
                        endTime = mealTimeWindows.lunchEnd,
                        onPickStart = { activeTimePickerTarget = TimePickerTarget.LUNCH_START },
                        onPickEnd = { activeTimePickerTarget = TimePickerTarget.LUNCH_END }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MealTimeRow(
                        mealIcon = "🫖",
                        mealName = "Once / Cena",
                        startTime = mealTimeWindows.dinnerStart,
                        endTime = mealTimeWindows.dinnerEnd,
                        onPickStart = { activeTimePickerTarget = TimePickerTarget.DINNER_START },
                        onPickEnd = { activeTimePickerTarget = TimePickerTarget.DINNER_END }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { mealTimeWindows = MealTimeWindows() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_reset_meal_windows))
                    }
                }
            }

            // Sección de Motor de Inteligencia Artificial
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_ai_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(id = R.string.settings_ai_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opciones de Proveedor de IA
                    AiProvider.entries.forEach { provider ->
                        val isSelected = selectedAiProvider == provider
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAiProvider = provider
                                        selectedVisionSource = provider.toVisionSource()
                                    }
                                ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAiProvider = provider
                                        selectedVisionSource = provider.toVisionSource()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    val (title, desc) = when (provider) {
                                        AiProvider.NVIDIA_NIM -> Pair(
                                            stringResource(id = R.string.ai_provider_nvidia),
                                            stringResource(id = R.string.ai_provider_nvidia_desc)
                                        )
                                        AiProvider.GOOGLE_GEMINI -> Pair(
                                            stringResource(id = R.string.ai_provider_gemini),
                                            stringResource(id = R.string.ai_provider_gemini_desc)
                                        )
                                        AiProvider.MINIMAX -> Pair(
                                            stringResource(id = R.string.ai_provider_minimax),
                                            stringResource(id = R.string.ai_provider_minimax_desc)
                                        )
                                        AiProvider.CUSTOM -> Pair(
                                            stringResource(id = R.string.ai_provider_custom),
                                            stringResource(id = R.string.ai_provider_custom_desc)
                                        )
                                        AiProvider.LOCAL -> Pair(
                                            stringResource(id = R.string.ai_provider_local),
                                            stringResource(id = R.string.ai_provider_local_desc)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Campo de Clave de API para proveedores en la nube
            if (selectedAiProvider.isCloud) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${stringResource(id = R.string.settings_api_key_label)} (${selectedAiProvider.displayName})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val helpText = when (selectedAiProvider) {
                            AiProvider.NVIDIA_NIM -> stringResource(id = R.string.ai_provider_nvidia_help)
                            AiProvider.GOOGLE_GEMINI -> stringResource(id = R.string.ai_provider_gemini_help)
                            AiProvider.MINIMAX -> stringResource(id = R.string.ai_provider_minimax_help)
                            AiProvider.CUSTOM -> "Compatible con cualquier servidor que soporte la API de OpenAI Chat Completions."
                            AiProvider.LOCAL -> ""
                        }

                        if (helpText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = helpText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val activeKey = when (selectedAiProvider) {
                            AiProvider.LOCAL -> ""
                            AiProvider.NVIDIA_NIM -> apiKeyNvidia
                            AiProvider.GOOGLE_GEMINI -> apiKeyGemini
                            AiProvider.MINIMAX -> apiKeyMiniMax
                            AiProvider.CUSTOM -> apiKeyCustom
                        }

                        OutlinedTextField(
                            value = activeKey,
                            onValueChange = { newKey ->
                                when (selectedAiProvider) {
                                    AiProvider.LOCAL -> {}
                                    AiProvider.NVIDIA_NIM -> apiKeyNvidia = newKey
                                    AiProvider.GOOGLE_GEMINI -> apiKeyGemini = newKey
                                    AiProvider.MINIMAX -> apiKeyMiniMax = newKey
                                    AiProvider.CUSTOM -> apiKeyCustom = newKey
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(id = R.string.settings_api_key_hint)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isApiKeyVisible) "Ocultar clave" else "Mostrar clave"
                                    )
                                }
                            },
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )

                        if (activeKey.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(id = R.string.settings_api_key_missing_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Opciones avanzadas (Endpoints y nombres de modelos personalizados)
            if (selectedAiProvider.isCloud) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_advanced_options_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isAdvancedExpanded = !isAdvancedExpanded }) {
                                Icon(
                                    imageVector = if (isAdvancedExpanded || selectedAiProvider == AiProvider.CUSTOM) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        if (isAdvancedExpanded || selectedAiProvider == AiProvider.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = customEndpointUrl,
                                onValueChange = { customEndpointUrl = it },
                                label = { Text(stringResource(id = R.string.settings_custom_endpoint_label)) },
                                placeholder = { Text(selectedAiProvider.defaultEndpointUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customTextModel,
                                onValueChange = { customTextModel = it },
                                label = { Text(stringResource(id = R.string.settings_custom_text_model_label)) },
                                placeholder = { Text(selectedAiProvider.defaultTextModel) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customVisionModel,
                                onValueChange = { customVisionModel = it },
                                label = { Text(stringResource(id = R.string.settings_custom_vision_model_label)) },
                                placeholder = { Text(selectedAiProvider.defaultVisionModel) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Sección de Google Health Connect
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasHealthConnectPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasHealthConnectPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasHealthConnectPermission) Icons.Default.CheckCircle else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (hasHealthConnectPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Health Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasHealthConnectPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isHealthConnectAvailable) {
                        Text(
                            text = "Health Connect no está instalado o disponible en tu dispositivo. Puedes instalarlo gratis desde Google Play Store.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (hasHealthConnectPermission) {
                        Text(
                            text = "✅ Conectado. Las comidas confirmadas se sincronizan automáticamente con tu registro de salud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "Conecta la app con Health Connect para registrar automáticamente calorías y macronutrientes en tu centro de salud de Android.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestHealthConnectPermission,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Conectar con Health Connect")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_health_connect_toggle),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasHealthConnectPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isHealthSyncEnabled,
                            onCheckedChange = { isHealthSyncEnabled = it }
                        )
                    }
                }
            }

            // Sección de Copia de Seguridad y Respaldo en la Nube (Google Drive) y Local
            BackupAndRestoreSection(
                lastBackupTimestamp = lastBackupTimestamp,
                autoBackupEnabled = autoBackupEnabled,
                autoBackupFolderUri = autoBackupFolderUri,
                autoBackupFolderName = autoBackupFolderName,
                lastAutoBackupTimestamp = lastAutoBackupTimestamp,
                isExporting = isExporting,
                isImporting = isImporting,
                onToggleAutoBackup = onToggleAutoBackup,
                onSelectFolderClick = {
                    openDocumentTreeLauncher.launch(null)
                },
                onUnlinkFolderClick = onUnlinkAutoBackupFolder,
                onCreateBackupClick = {
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    createDocumentLauncher.launch("palta_respaldo_$todayStr.json")
                },
                onRestoreBackupClick = {
                    openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Guardar
            Button(
                onClick = {
                    onSelectAiProvider(selectedAiProvider)
                    onSaveApiKeyForProvider(AiProvider.NVIDIA_NIM, apiKeyNvidia)
                    onSaveApiKeyForProvider(AiProvider.GOOGLE_GEMINI, apiKeyGemini)
                    onSaveApiKeyForProvider(AiProvider.MINIMAX, apiKeyMiniMax)
                    onSaveApiKeyForProvider(AiProvider.CUSTOM, apiKeyCustom)
                    onSaveCustomAiParameters(customEndpointUrl, customTextModel, customVisionModel)
                    val activeKey = when (selectedAiProvider) {
                        AiProvider.LOCAL -> ""
                        AiProvider.NVIDIA_NIM -> apiKeyNvidia
                        AiProvider.GOOGLE_GEMINI -> apiKeyGemini
                        AiProvider.MINIMAX -> apiKeyMiniMax
                        AiProvider.CUSTOM -> apiKeyCustom
                    }
                    onSaveSettings(activeKey, selectedAiProvider.toVisionSource(), isHealthSyncEnabled)
                    onSaveMealTimeWindows(mealTimeWindows)
                    val finalCalories = targetCaloriesText.toDoubleOrNull() ?: currentTargetCalories
                    val finalProtein = targetProteinText.toDoubleOrNull() ?: currentTargetProteinGrams
                    val finalCarbs = targetCarbsText.toDoubleOrNull() ?: currentTargetCarbsGrams
                    val finalFat = targetFatText.toDoubleOrNull() ?: currentTargetFatGrams
                    onSaveDailyBudget(finalCalories, finalProtein, finalCarbs, finalFat)
                    isSavedFeedbackActive = true
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(savedSuccessMessage)
                        delay(2000)
                        isSavedFeedbackActive = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSavedFeedbackActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.msg_settings_saved),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.btn_save_settings),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MealTimeRow(
    mealIcon: String,
    mealName: String,
    startTime: LocalTime,
    endTime: LocalTime,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = mealIcon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickStart),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", startTime.hour, startTime.minute),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "a",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickEnd),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", endTime.hour, endTime.minute),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Preview(name = "Configuración - NVIDIA NIM", showBackground = true)
@Composable
private fun PreviewSettingsScreenNvidia() {
    CalculadoraCaloriasTheme {
        SettingsScreen(
            currentAiProvider = AiProvider.NVIDIA_NIM,
            currentApiKeyNvidia = "nvapi-sample-key",
            currentHealthSyncEnabled = true
        )
    }
}

@Preview(name = "Configuración - Google Gemini", showBackground = true)
@Composable
private fun PreviewSettingsScreenGemini() {
    CalculadoraCaloriasTheme {
        SettingsScreen(
            currentAiProvider = AiProvider.GOOGLE_GEMINI,
            currentApiKeyGemini = "AIzaSy-sample",
            currentHealthSyncEnabled = true
        )
    }
}

@Preview(name = "Configuración - Local", showBackground = true)
@Composable
private fun PreviewSettingsScreenLocal() {
    CalculadoraCaloriasTheme {
        SettingsScreen(
            currentAiProvider = AiProvider.LOCAL,
            currentHealthSyncEnabled = true
        )
    }
}
