package com.calculadoracalorias.app.presentation.navigation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Forma cóncava para la barra de navegación inferior con una cuna suave (notch)
 * centrada para albergar el botón de acción principal de forma acoplada (docked).
 */
class BottomBarNotchedShape(
    private val cradleRadius: Dp = 38.dp,
    private val roundedCornerRadius: Dp = 24.dp,
    private val cradleDepthRatio: Float = 0.65f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height

        with(density) {
            val r = cradleRadius.toPx()
            val corner = roundedCornerRadius.toPx()
            val centerX = width / 2f
            val depth = r * cradleDepthRatio

            // 1. Esquina superior izquierda redondeada
            path.moveTo(0f, corner)
            path.quadraticTo(0f, 0f, corner, 0f)

            // 2. Línea recta superior izquierda hasta el inicio de la cuna
            val notchStart = centerX - r * 1.25f
            path.lineTo(notchStart, 0f)

            // 3. Curva descendente hacia el centro de la cuna
            path.cubicTo(
                centerX - r * 0.75f, 0f,
                centerX - r * 0.65f, depth,
                centerX, depth
            )

            // 4. Curva ascendente saliendo de la cuna
            val notchEnd = centerX + r * 1.25f
            path.cubicTo(
                centerX + r * 0.65f, depth,
                centerX + r * 0.75f, 0f,
                notchEnd, 0f
            )

            // 5. Línea recta superior derecha hasta la esquina
            path.lineTo(width - corner, 0f)
            path.quadraticTo(width, 0f, width, corner)

            // 6. Borde derecho
            path.lineTo(width, height)

            // 7. Base
            path.lineTo(0f, height)

            path.close()
        }

        return Outline.Generic(path)
    }
}
