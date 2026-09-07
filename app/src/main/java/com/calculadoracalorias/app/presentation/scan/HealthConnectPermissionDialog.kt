package com.calculadoracalorias.app.presentation.scan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

@Composable
fun HealthConnectPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(id = R.string.health_connect_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.health_connect_dialog_description))
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.btn_connect_health))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.btn_remind_later))
            }
        }
    )
}

@Preview(name = "Diálogo Permisos Health Connect", showBackground = true)
@Composable
private fun PreviewHealthConnectPermissionDialog() {
    CalculadoraCaloriasTheme {
        HealthConnectPermissionDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
