package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

enum class VmButtonStyle { Primary, Secondary, Text }

@Composable
fun VmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: VmButtonStyle = VmButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val colors = VmTheme.colors
    val container =
        when (style) {
            VmButtonStyle.Primary -> colors.accent
            VmButtonStyle.Secondary -> colors.surfaceRaised
            VmButtonStyle.Text -> Color.Transparent
        }
    val contentColor =
        when (style) {
            VmButtonStyle.Primary -> colors.onAccent
            VmButtonStyle.Secondary -> colors.textPrimary
            VmButtonStyle.Text -> colors.accent
        }

    VmClickableBox(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = container,
        borderColor = if (style == VmButtonStyle.Secondary) colors.border else null,
        contentPadding = PaddingValues(horizontal = VmSpacing.lg, vertical = VmSpacing.md),
    ) {
        BasicText(text = text, style = VmTheme.typography.bodyStrong.copy(color = contentColor))
    }
}
