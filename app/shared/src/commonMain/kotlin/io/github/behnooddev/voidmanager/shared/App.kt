package io.github.behnooddev.voidmanager.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmEmptyState
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavItem
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavigationBar
import io.github.behnooddev.voidmanager.core.designsystem.components.VmNavigationRail
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTopBar
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.designsystem.theme.VoidManagerTheme
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_lock
import io.github.behnooddev.voidmanager.shared.resources.nav_home
import io.github.behnooddev.voidmanager.shared.resources.nav_people
import io.github.behnooddev.voidmanager.shared.resources.nav_personal
import io.github.behnooddev.voidmanager.shared.resources.nav_settings
import io.github.behnooddev.voidmanager.shared.resources.placeholder_message
import io.github.behnooddev.voidmanager.shared.resources.placeholder_title
import io.github.behnooddev.voidmanager.shared.ui.SettingsScreen
import io.github.behnooddev.voidmanager.shared.ui.SetupScreen
import io.github.behnooddev.voidmanager.shared.ui.UnlockScreen
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyProvider
import io.github.behnooddev.voidmanager.shared.vault.Stage
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Width from which the navigation rail replaces the bottom bar. */
private const val EXPANDED_WIDTH_DP = 840

/** How often the idle timer is checked while the app is in the foreground. */
private const val AUTO_LOCK_TICK_MILLIS = 1_000L

@Composable
fun App(
    controller: VaultController,
    deviceKeys: DeviceKeyProvider? = null,
) {
    VoidManagerTheme {
        LaunchedEffect(controller) {
            while (true) {
                delay(AUTO_LOCK_TICK_MILLIS)
                controller.checkAutoLock()
            }
        }
        // Any touch or key press counts as activity. The observers never consume the event.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(controller) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                controller.onInteraction()
                            }
                        }
                    }.onPreviewKeyEvent {
                        controller.onInteraction()
                        false
                    },
        ) {
            when (controller.stage) {
                Stage.NeedsSetup -> SetupScreen(controller)
                Stage.Locked -> UnlockScreen(controller, deviceKeys)
                is Stage.Unlocked -> AppShell(controller, deviceKeys)
            }
        }
    }
}

@Composable
private fun AppShell(
    controller: VaultController,
    deviceKeys: DeviceKeyProvider?,
) {
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
                DestinationContent(destinations[selectedIndex], controller, deviceKeys, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                DestinationContent(destinations[selectedIndex], controller, deviceKeys, Modifier.weight(1f))
                VmNavigationBar(items = items, selectedIndex = selectedIndex, onSelect = { selectedIndex = it })
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: Destination,
    controller: VaultController,
    deviceKeys: DeviceKeyProvider?,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        VmTopBar(
            title = destinationLabel(destination),
            actions = {
                VmButton(
                    text = stringResource(Res.string.action_lock),
                    onClick = controller::lock,
                    style = VmButtonStyle.Text,
                )
            },
        )
        if (destination == Destination.Settings) {
            SettingsScreen(controller, deviceKeys)
        } else {
            VmEmptyState(
                title = stringResource(Res.string.placeholder_title),
                message = stringResource(Res.string.placeholder_message),
            )
        }
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
