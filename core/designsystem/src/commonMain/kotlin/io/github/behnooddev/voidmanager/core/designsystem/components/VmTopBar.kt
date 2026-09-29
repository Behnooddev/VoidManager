package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/** Top bar with an optional leading slot and trailing actions. Screen-level save actions live in [actions]. */
@Composable
fun VmTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(VmDimens.topBarHeight)
                .background(VmTheme.colors.background)
                .padding(horizontal = VmSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VmSpacing.md),
    ) {
        if (leading != null) leading()
        BasicText(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
            style = VmTheme.typography.title.copy(color = VmTheme.colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}
