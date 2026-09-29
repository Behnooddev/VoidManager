package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/** A flat container with an optional hairline border. Use sparingly; not every group needs a card. */
@Composable
fun VmSurface(
    modifier: Modifier = Modifier,
    color: Color = VmTheme.colors.surface,
    shape: Shape = VmTheme.shapes.medium,
    bordered: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(color, shape)
                .then(if (bordered) Modifier.border(VmDimens.hairline, VmTheme.colors.border, shape) else Modifier)
                .clip(shape),
        content = content,
    )
}
