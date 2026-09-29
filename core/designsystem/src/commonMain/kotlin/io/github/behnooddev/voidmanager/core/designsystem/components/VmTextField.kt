package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/**
 * Single-purpose text input. [label] is the accessible name and is shown as the hint while the field is empty.
 */
@Composable
fun VmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    val colors = VmTheme.colors
    val shape = VmTheme.shapes.medium
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = VmDimens.minTouchTarget)
                .semantics { contentDescription = label }
                .clip(shape)
                .background(colors.surfaceRaised, shape)
                .border(
                    width = if (focused) 2.dp else VmDimens.hairline,
                    color = if (focused) colors.accent else colors.border,
                    shape = shape,
                ).padding(horizontal = VmSpacing.lg, vertical = VmSpacing.md),
        textStyle = VmTheme.typography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.accent),
        singleLine = singleLine,
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    BasicText(text = label, style = VmTheme.typography.body.copy(color = colors.textTertiary))
                }
                innerTextField()
            }
        },
    )
}
