package com.calculadoracalorias.app.presentation.navigation

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.calculadoracalorias.app.R
import com.calculadoracalorias.app.presentation.AppDestination
import com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme

@Composable
fun AppBottomNavigationBar(
    currentDestination: AppDestination,
    onNavigateToDestination: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Fondo de la barra con forma de cuna recortada (Notched Shape)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BottomBarNotchedShape(cradleRadius = 38.dp, roundedCornerRadius = 24.dp, cradleDepthRatio = 0.65f),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Diario
                BottomNavigationItem(
                    label = stringResource(id = R.string.nav_diary),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    isSelected = currentDestination == AppDestination.SUMMARY,
                    onClick = { onNavigateToDestination(AppDestination.SUMMARY) },
                    modifier = Modifier.weight(1f)
                )

                // 2. Estadísticas
                BottomNavigationItem(
                    label = stringResource(id = R.string.nav_statistics),
                    icon = Icons.Default.BarChart,
                    isSelected = currentDestination == AppDestination.STATISTICS,
                    onClick = { onNavigateToDestination(AppDestination.STATISTICS) },
                    modifier = Modifier.weight(1f)
                )

                // Espacio central para la cuna del FAB
                Spacer(modifier = Modifier.weight(1.1f))

                // 3. Suplementos
                BottomNavigationItem(
                    label = stringResource(id = R.string.nav_supplements),
                    icon = Icons.Default.Medication,
                    isSelected = currentDestination == AppDestination.SUPPLEMENTS,
                    onClick = { onNavigateToDestination(AppDestination.SUPPLEMENTS) },
                    modifier = Modifier.weight(1f)
                )

                // 4. Ajustes
                BottomNavigationItem(
                    label = stringResource(id = R.string.nav_settings),
                    icon = Icons.Default.Settings,
                    isSelected = currentDestination == AppDestination.SETTINGS,
                    onClick = { onNavigateToDestination(AppDestination.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Botón central acoplado en la cuna (Docked FAB de Cámara)
        FloatingActionButton(
            onClick = { onNavigateToDestination(AppDestination.CAMERA) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = stringResource(id = R.string.nav_scan),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomNavigationItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = textWeight,
            color = tintColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "Modo Claro", showBackground = true)
@Composable
private fun AppBottomNavigationBarLightPreview() {
    CalculadoraCaloriasTheme(darkTheme = false) {
        AppBottomNavigationBar(
            currentDestination = AppDestination.SUMMARY,
            onNavigateToDestination = {}
        )
    }
}

@Preview(name = "Modo Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppBottomNavigationBarDarkPreview() {
    CalculadoraCaloriasTheme(darkTheme = true) {
        AppBottomNavigationBar(
            currentDestination = AppDestination.SUPPLEMENTS,
            onNavigateToDestination = {}
        )
    }
}
