package com.calculadoracalorias.app.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calculadoracalorias.app.domain.model.AiProvider
import com.calculadoracalorias.app.domain.model.AiTechnicalDetails
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiDiagnosticDetailsDialog(
    technicalDetails: AiTechnicalDetails,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val formattedDate = remember(technicalDetails.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date(technicalDetails.timestamp))
    }

    val diagnosticSummary = remember(technicalDetails) {
        buildString {
            appendLine("=== Diagnóstico de Conexión IA ===")
            appendLine("Fecha: $formattedDate")
            appendLine("Proveedor: ${technicalDetails.provider.displayName}")
            appendLine("Modelo: ${technicalDetails.model}")
            appendLine("Endpoint: ${technicalDetails.endpointUrl}")
            appendLine("Código HTTP: ${technicalDetails.httpStatus?.toString() ?: "Sin respuesta (Timeout/Red)"}")
            appendLine("Duración: ${technicalDetails.durationMs} ms")
            if (!technicalDetails.errorBody.isNullOrBlank()) {
                appendLine("Cuerpo de respuesta / Error:")
                appendLine(technicalDetails.errorBody)
            }
            if (!technicalDetails.exceptionMessage.isNullOrBlank()) {
                appendLine("Excepción:")
                appendLine(technicalDetails.exceptionMessage)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Detalle técnico del error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Información para desarrollador y depuración:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DiagnosticRow(label = "Proveedor:", value = technicalDetails.provider.displayName)
                        DiagnosticRow(label = "Modelo:", value = technicalDetails.model)
                        DiagnosticRow(
                            label = "Estado HTTP:",
                            value = technicalDetails.httpStatus?.let { "HTTP $it" } ?: "Sin respuesta (Red)"
                        )
                        DiagnosticRow(label = "Duración:", value = "${technicalDetails.durationMs} ms")
                        DiagnosticRow(label = "Endpoint:", value = technicalDetails.endpointUrl)
                    }
                }

                val errorContent = technicalDetails.errorBody ?: technicalDetails.exceptionMessage
                if (!errorContent.isNullOrBlank()) {
                    Text(
                        text = "Respuesta del servidor / Causa:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = errorContent,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(diagnosticSummary))
                    isCopied = true
                }
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isCopied) "¡Copiado!" else "Copiar detalle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Diálogo Detalle Técnico", showBackground = true)
@Composable
private fun PreviewAiDiagnosticDetailsDialog() {
    CalculadoraCaloriasTheme {
        AiDiagnosticDetailsDialog(
            technicalDetails = AiTechnicalDetails(
                provider = AiProvider.NVIDIA_NIM,
                model = "meta/llama-3.3-70b-instruct",
                endpointUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
                httpStatus = 401,
                errorBody = "{\"detail\":\"User not authorized or invalid token\"}",
                durationMs = 240
            ),
            onDismiss = {}
        )
    }
}
