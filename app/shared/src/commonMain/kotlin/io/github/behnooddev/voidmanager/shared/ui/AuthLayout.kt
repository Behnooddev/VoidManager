package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

private const val CONTENT_MAX_WIDTH = 480

/** The frame shared by the setup and unlock screens: one centered, scrollable column. */
@Composable
internal fun AuthLayout(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(VmTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(VmSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.lg),
        ) {
            BasicText(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = VmTheme.typography.display.copy(color = VmTheme.colors.textPrimary),
            )
            content()
        }
    }
}

@Composable
internal fun BodyText(
    text: String,
    color: Color = VmTheme.colors.textSecondary,
    modifier: Modifier = Modifier,
) {
    BasicText(text = text, modifier = modifier, style = VmTheme.typography.body.copy(color = color))
}

/** A message that assistive technology announces when it appears. */
@Composable
internal fun StatusText(
    text: String,
    color: Color,
) {
    BasicText(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = VmTheme.typography.body.copy(color = color),
    )
}
