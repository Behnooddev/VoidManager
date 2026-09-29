package io.github.behnooddev.voidmanager.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.components.VmEmptyState
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavItem
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavigationBar
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavigationRail
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTopBar
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.designsystem.theme.VoidManagerTheme
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.nav_home
import io.github.behnooddev.voidmanager.shared.resources.nav_people
import io.github.behnooddev.voidmanager.shared.resources.nav_personal
import io.github.behnooddev.voidmanager.shared.resources.nav_settings
import io.github.behnooddev.voidmanager.shared.resources.placeholder_message
import io.github.behnooddev.voidmanager.shared.resources.placeholder_title
import org.jetbrains.compose.resources.stringResource

/** Width from which the navigation rail replaces the bottom bar. */
private const val EXPANDED_WIDTH_DP = 840

@Composable
fun App() {
    VoidManagerTheme {
        AppShell()
    }
}

@Composable
private fun AppShell() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val destinations = Destination.entries
    val items = destinations.map { VmNavItem(label = destinationLabel(it), icon = it.icon) }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(VmTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        if (maxWidth >= EXPANDED_WIDTH_DP.dp) {
            Row(Modifier.fillMaxSize()) {
                VmNavigationRail(items = items, selectedIndex = selectedIndex, onSelect = { selectedIndex = it })
                DestinationContent(destinations[selectedIndex], Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                DestinationContent(destinations[selectedIndex], Modifier.weight(1f))
                VmNavigationBar(items = items, selectedIndex = selectedIndex, onSelect = { selectedIndex = it })
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        VmTopBar(title = destinationLabel(destination))
        VmEmptyState(
            title = stringResource(Res.string.placeholder_title),
            message = stringResource(Res.string.placeholder_message),
        )
    }
}

@Composable
private fun destinationLabel(destination: Destination): String =
    stringResource(
        when (destination) {
            Destination.Home -> Res.string.nav_home
            Destination.People -> Res.string.nav_people
            Destination.Personal -> Res.string.nav_personal
            Destination.Settings -> Res.string.nav_settings
        },
    )
