package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import io.github.behnooddev.voidmanager.core.designsystem.components.VmChip
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.level_raise_only
import io.github.behnooddev.voidmanager.shared.resources.level_title
import org.jetbrains.compose.resources.stringResource

/** A row of protection levels with a line that says what the chosen one means. */
@Composable
internal fun LevelPicker(
    title: String,
    choices: List<Sensitivity>,
    selected: Sensitivity,
    onSelect: (Sensitivity) -> Unit,
    showRaiseOnlyNote: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VmSpacing.xs)) {
        BasicText(text = title, style = VmTheme.typography.subtitle.copy(color = VmTheme.colors.textPrimary))
        Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
            for (level in choices) {
                VmChip(label = levelText(level), selected = level == selected, onClick = { onSelect(level) })
            }
        }
        BodyText(levelHelp(selected))
        if (showRaiseOnlyNote) BodyText(stringResource(Res.string.level_raise_only))
    }
}

@Composable
internal fun defaultLevelTitle(): String = stringResource(Res.string.level_title)
