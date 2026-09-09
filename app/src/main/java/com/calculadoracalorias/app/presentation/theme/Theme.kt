package com.calculadoracalorias.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PaltaDarkPrimary,
    onPrimary = PaltaDarkOnPrimary,
    primaryContainer = PaltaDarkPrimaryContainer,
    onPrimaryContainer = PaltaDarkOnPrimaryContainer,
    secondary = PaltaDarkSecondary,
    onSecondary = PaltaDarkOnSecondary,
    secondaryContainer = PaltaDarkSecondaryContainer,
    onSecondaryContainer = PaltaDarkOnSecondaryContainer,
    tertiary = PaltaDarkTertiary,
    onTertiary = PaltaDarkOnTertiary,
    background = PaltaDarkBackground,
    onBackground = PaltaDarkOnBackground,
    surface = PaltaDarkSurface,
    onSurface = PaltaDarkOnSurface,
    surfaceVariant = PaltaDarkSurfaceVariant,
    onSurfaceVariant = PaltaDarkOnSurfaceVariant,
    surfaceContainer = PaltaDarkSurfaceContainer,
    surfaceContainerHigh = PaltaDarkSurfaceContainerHigh,
    surfaceContainerHighest = PaltaDarkSurfaceContainerHighest,
    outline = PaltaDarkOutline,
    outlineVariant = PaltaDarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PaltaLightPrimary,
    onPrimary = PaltaLightOnPrimary,
    primaryContainer = PaltaLightPrimaryContainer,
    onPrimaryContainer = PaltaLightOnPrimaryContainer,
    secondary = PaltaLightSecondary,
    onSecondary = PaltaLightOnSecondary,
    secondaryContainer = PaltaLightSecondaryContainer,
    onSecondaryContainer = PaltaLightOnSecondaryContainer,
    tertiary = PaltaLightTertiary,
    onTertiary = PaltaLightOnTertiary,
    background = PaltaLightBackground,
    onBackground = PaltaLightOnBackground,
    surface = PaltaLightSurface,
    onSurface = PaltaLightOnSurface,
    surfaceVariant = PaltaLightSurfaceVariant,
    onSurfaceVariant = PaltaLightOnSurfaceVariant,
    surfaceContainer = PaltaLightSurfaceContainer,
    surfaceContainerHigh = PaltaLightSurfaceContainerHigh,
    surfaceContainerHighest = PaltaLightSurfaceContainerHighest,
    outline = PaltaLightOutline,
    outlineVariant = PaltaLightOutlineVariant
)

@Composable
fun CalculadoraCaloriasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
