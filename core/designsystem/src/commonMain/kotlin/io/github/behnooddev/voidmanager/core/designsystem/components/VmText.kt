package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

@Composable
fun VmSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        modifier =
            modifier
                .padding(horizontal = VmSpacing.lg, vertical = VmSpacing.sm)
                .semantics { heading() },
        style = VmTheme.typography.label.copy(color = VmTheme.colors.textSecondary),
    )
}
