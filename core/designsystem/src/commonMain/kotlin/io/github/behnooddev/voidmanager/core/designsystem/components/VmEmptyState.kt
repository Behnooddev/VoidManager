package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/** Explains what is missing and, when possible, offers the action that fixes it. */
@Composable
fun VmEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(VmSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            text = title,
            style =
                VmTheme.typography.subtitle.copy(
                    color = VmTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
        )
        Spacer(Modifier.height(VmSpacing.sm))
        BasicText(
            text = message,
            modifier = Modifier.widthIn(max = MESSAGE_MAX_WIDTH.dp),
            style =
                VmTheme.typography.body.copy(
                    color = VmTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                ),
        )
        if (action != null) {
            Spacer(Modifier.height(VmSpacing.xl))
            action()
        }
    }
}

private const val MESSAGE_MAX_WIDTH = 360
