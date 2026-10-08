package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

/** A tappable row with a title, an optional second line and an optional short text at the end. */
@Composable
fun VmListItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
) {
    VmClickableBox(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = VmDimens.minTouchTarget),
        contentPadding = PaddingValues(horizontal = VmSpacing.lg, vertical = VmSpacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VmSpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = title, style = VmTheme.typography.body.copy(color = VmTheme.colors.textPrimary))
                if (subtitle != null) {
                    BasicText(
                        text = subtitle,
                        style = VmTheme.typography.caption.copy(color = VmTheme.colors.textSecondary),
                    )
                }
            }
            if (trailing != null) {
                BasicText(text = trailing, style = VmTheme.typography.label.copy(color = VmTheme.colors.textSecondary))
            }
        }
    }
}
