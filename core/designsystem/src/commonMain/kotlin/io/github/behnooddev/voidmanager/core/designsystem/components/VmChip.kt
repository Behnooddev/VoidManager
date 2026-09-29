package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

@Composable
fun VmChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VmTheme.colors
    VmClickableBox(
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(CHIP_RADIUS.dp),
        role = Role.Checkbox,
        background = if (selected) colors.accentSubtle else colors.surfaceRaised,
        borderColor = if (selected) colors.accent else colors.border,
        contentPadding = PaddingValues(horizontal = VmSpacing.md, vertical = VmSpacing.sm),
    ) {
        BasicText(
            text = label,
            style = VmTheme.typography.label.copy(color = if (selected) colors.accent else colors.textPrimary),
        )
    }
}

private const val CHIP_RADIUS = 20
