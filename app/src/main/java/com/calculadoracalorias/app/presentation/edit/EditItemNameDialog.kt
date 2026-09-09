package com.calculadoracalorias.app.presentation.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

@Composable
fun EditItemNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textFieldValue by remember(initialName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val isValid = textFieldValue.text.trim().isNotBlank()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_item_name_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text(stringResource(R.string.edit_item_name_label)) },
                    placeholder = { Text(stringResource(R.string.edit_item_name_hint)) },
                    singleLine = true,
                    isError = !isValid && textFieldValue.text.isNotEmpty(),
                    supportingText = {
                        if (!isValid && textFieldValue.text.isNotEmpty()) {
                            Text(stringResource(R.string.edit_item_name_empty_error))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = textFieldValue.text.trim()
                    if (trimmed.isNotBlank()) {
                        onConfirm(trimmed)
                    }
                },
                enabled = isValid
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditItemNameDialogPreview() {
    CalculadoraCaloriasTheme {
        EditItemNameDialog(
            initialName = "Carne picada",
            onDismiss = {},
            onConfirm = {}
        )
    }
}
