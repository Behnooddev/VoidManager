package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens

enum class VmIconKind { Home, People, Personal, Settings }

/**
 * Line icons drawn on a 24 unit grid. They are decorative: the element that contains an icon
 * provides the accessible name, so the icon itself carries no semantics.
 */
@Composable
fun VmIcon(
    kind: VmIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = VmDimens.iconSize,
) {
    Canvas(modifier.size(iconSize)) {
        val unit = size.minDimension / GRID
        val stroke = Stroke(width = STROKE_UNITS * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            VmIconKind.Home -> drawHome(tint, stroke, unit)
            VmIconKind.People -> drawPeople(tint, stroke, unit)
            VmIconKind.Personal -> drawPersonal(tint, stroke, unit)
            VmIconKind.Settings -> drawSettings(tint, stroke, unit)
        }
    }
}

private const val GRID = 24f
private const val STROKE_UNITS = 1.75f

private fun point(
    x: Float,
    y: Float,
    unit: Float,
) = Offset(x * unit, y * unit)

private fun DrawScope.drawHome(
    color: Color,
    stroke: Stroke,
    u: Float,
) {
    val body =
        Path().apply {
            moveTo(5f * u, 11f * u)
            lineTo(12f * u, 4.5f * u)
            lineTo(19f * u, 11f * u)
            lineTo(19f * u, 19.5f * u)
            lineTo(5f * u, 19.5f * u)
            close()
        }
    drawPath(body, color, style = stroke)
    val door =
        Path().apply {
            moveTo(10f * u, 19.5f * u)
            lineTo(10f * u, 14f * u)
            lineTo(14f * u, 14f * u)
            lineTo(14f * u, 19.5f * u)
        }
    drawPath(door, color, style = stroke)
}

private fun DrawScope.drawPeople(
    color: Color,
    stroke: Stroke,
    u: Float,
) {
    drawCircle(color, radius = 3f * u, center = point(9f, 8.5f, u), style = stroke)
    val body =
        Path().apply {
            moveTo(3.5f * u, 19.5f * u)
            cubicTo(3.5f * u, 15.5f * u, 6f * u, 14f * u, 9f * u, 14f * u)
            cubicTo(12f * u, 14f * u, 14.5f * u, 15.5f * u, 14.5f * u, 19.5f * u)
        }
    drawPath(body, color, style = stroke)
    drawCircle(color, radius = 2.5f * u, center = point(16.5f, 9.5f, u), style = stroke)
    val second =
        Path().apply {
            moveTo(16f * u, 14.2f * u)
            cubicTo(19f * u, 14.4f * u, 20.5f * u, 16f * u, 20.5f * u, 19.5f * u)
        }
    drawPath(second, color, style = stroke)
}

private fun DrawScope.drawPersonal(
    color: Color,
    stroke: Stroke,
    u: Float,
) {
    val shield =
        Path().apply {
            moveTo(12f * u, 3.5f * u)
            lineTo(19f * u, 6.5f * u)
            lineTo(19f * u, 12f * u)
            cubicTo(19f * u, 16.5f * u, 16f * u, 19.5f * u, 12f * u, 20.5f * u)
            cubicTo(8f * u, 19.5f * u, 5f * u, 16.5f * u, 5f * u, 12f * u)
            lineTo(5f * u, 6.5f * u)
            close()
        }
    drawPath(shield, color, style = stroke)
    drawCircle(color, radius = 1.6f * u, center = point(12f, 10.8f, u), style = stroke)
    drawLine(color, point(12f, 12.4f, u), point(12f, 15f, u), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawSettings(
    color: Color,
    stroke: Stroke,
    u: Float,
) {
    // Three sliders: each row is a line broken around its knob.
    val rows = listOf(7f to 9f, 12f to 15f, 17f to 8f)
    for ((y, knobX) in rows) {
        drawLine(color, point(4f, y, u), point(knobX - 2.6f, y, u), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color, point(knobX + 2.6f, y, u), point(20f, y, u), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawCircle(color, radius = 2.2f * u, center = point(knobX, y, u), style = stroke)
    }
}
