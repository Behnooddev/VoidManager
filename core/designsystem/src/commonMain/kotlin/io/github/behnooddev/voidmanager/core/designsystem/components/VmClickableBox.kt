package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/**
 * Shared base for tappable elements: minimum touch target, pressed and disabled feedback,
 * and a visible focus ring for keyboard navigation on desktop.
 */
@Composable
internal fun VmClickableBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = VmTheme.shapes.medium,
    role: Role = Role.Button,
    enabled: Boolean = true,
    background: Color = Color.Transparent,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val colors = VmTheme.colors
    val alpha =
        when {
            !enabled -> DISABLED_ALPHA
            pressed -> PRESSED_ALPHA
            else -> 1f
        }

    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = VmDimens.minTouchTarget, minHeight = VmDimens.minTouchTarget)
                .alpha(alpha)
                .clip(shape)
                .background(background, shape)
                .then(if (borderColor != null) Modifier.border(VmDimens.hairline, borderColor, shape) else Modifier)
                .then(if (focused) Modifier.border(FOCUS_RING_WIDTH.dp, colors.accent, shape) else Modifier)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onClick = onClick,
                ).padding(contentPadding),
        contentAlignment = contentAlignment,
        content = content,
    )
}

private const val DISABLED_ALPHA = 0.38f
private const val PRESSED_ALPHA = 0.85f
private const val FOCUS_RING_WIDTH = 2
