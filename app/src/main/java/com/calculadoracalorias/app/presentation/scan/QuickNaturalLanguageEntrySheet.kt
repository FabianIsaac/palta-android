package com.calculadoracalorias.app.presentation.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickNaturalLanguageEntrySheet(
    initialText: String = "",
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    isAnalyzing: Boolean = false,
    onDismiss: () -> Unit,
    onAnalyzeDescription: (String) -> Unit
) {
    var textInput by remember(initialText) { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = { if (!isAnalyzing) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.title_describe_meal_ai),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isAnalyzing) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.desc_natural_language_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chips con sugerencias rápidas chilenas
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val suggestions = listOf(
                    "☕ Café con leche y azúcar",
                    "🥑 Marraqueta con palta",
                    "🍳 2 huevos revueltos",
                    "🍵 Té con galletas"
                )
                suggestions.forEach { suggestion ->
                    val cleanText = suggestion.substringAfter(" ")
                    SuggestionChip(
                        onClick = {
                            textInput = if (textInput.isBlank()) cleanText else "$textInput, $cleanText"
                        },
                        label = { Text(suggestion, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.placeholder_describe_meal),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 5,
                enabled = !isAnalyzing,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (textInput.isNotBlank() && !isAnalyzing) {
                            onAnalyzeDescription(textInput.trim())
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isAnalyzing
                ) {
                    Text("Cancelar")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onAnalyzeDescription(textInput.trim())
                        }
                    },
                    enabled = textInput.isNotBlank() && !isAnalyzing,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.loading_interpreting_ai))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.btn_interpret_ai))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Previews en Español de Chile sin voseo (Requisito 4 & Tarea 3.5)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Diálogo Entrada Rápida IA - Normal", showBackground = true)
@Composable
private fun PreviewQuickNaturalLanguageEntrySheet() {
    CalculadoraCaloriasTheme {
        QuickNaturalLanguageEntrySheet(
            isAnalyzing = false,
            onDismiss = {},
            onAnalyzeDescription = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Diálogo Entrada Rápida IA - Analizando", showBackground = true)
@Composable
private fun PreviewQuickNaturalLanguageEntrySheetAnalyzing() {
    CalculadoraCaloriasTheme {
        QuickNaturalLanguageEntrySheet(
            isAnalyzing = true,
            onDismiss = {},
            onAnalyzeDescription = {}
        )
    }
}
