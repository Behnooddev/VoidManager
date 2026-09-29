package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmDimens
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme

data class VmNavItem(
    val label: String,
    val icon: VmIconKind,
)

/** Bottom navigation for compact widths. */
@Composable
fun VmNavigationBar(
    items: List<VmNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VmTheme.colors
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(VmDimens.hairline, colors.border),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(VmDimens.navigationBarHeight)
                    .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                NavigationEntry(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Navigation rail for wide layouts. */
@Composable
fun VmNavigationRail(
    items: List<VmNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VmTheme.colors
    Column(
        modifier =
            modifier
                .width(VmDimens.navigationRailWidth)
                .fillMaxHeight()
                .background(colors.surface)
                .border(VmDimens.hairline, colors.border)
                .padding(vertical = VmSpacing.lg)
                .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(VmSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEachIndexed { index, item ->
            NavigationEntry(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NavigationEntry(
    item: VmNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VmTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val tint = if (selected) colors.accent else colors.textSecondary
    val shape = VmTheme.shapes.medium

    Column(
        modifier =
            modifier
                .defaultMinSize(minHeight = VmDimens.minTouchTarget)
                .clip(shape)
                .then(if (focused) Modifier.border(2.dp, colors.accent, shape) else Modifier)
                .selectable(
                    selected = selected,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(vertical = VmSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VmSpacing.xs),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(VmTheme.shapes.large)
                    .background(if (selected) colors.accentSubtle else Color.Transparent)
                    .padding(horizontal = VmSpacing.lg, vertical = VmSpacing.xs),
        ) {
            VmIcon(kind = item.icon, tint = tint)
        }
        BasicText(text = item.label, style = VmTheme.typography.caption.copy(color = tint))
    }
}
