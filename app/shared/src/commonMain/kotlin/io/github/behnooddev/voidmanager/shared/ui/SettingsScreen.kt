package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmChip
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_disable
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_enable
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_failed
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_note
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_off
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_on
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_title
import io.github.behnooddev.voidmanager.shared.resources.settings_biometric_unavailable
import io.github.behnooddev.voidmanager.shared.resources.settings_clipboard_note
import io.github.behnooddev.voidmanager.shared.resources.settings_clipboard_title
import io.github.behnooddev.voidmanager.shared.resources.settings_idle_title
import io.github.behnooddev.voidmanager.shared.resources.settings_minutes
import io.github.behnooddev.voidmanager.shared.resources.settings_reauth_title
import io.github.behnooddev.voidmanager.shared.resources.settings_reveal_title
import io.github.behnooddev.voidmanager.shared.resources.settings_seconds
import io.github.behnooddev.voidmanager.shared.settings.AppSettings
import io.github.behnooddev.voidmanager.shared.settings.AppSettingsHolder
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyProvider
import io.github.behnooddev.voidmanager.shared.vault.DeviceUnlockChange
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsScreen(
    controller: VaultController,
    deviceKeys: DeviceKeyProvider?,
    settings: AppSettingsHolder,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val available = remember { deviceKeys != null && deviceKeys.isAvailable() }
    var enabled by remember { mutableStateOf(deviceKeys != null && controller.deviceUnlockReady(deviceKeys)) }
    var enableFailed by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VmSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(VmSpacing.md),
    ) {
        BasicText(
            text = stringResource(Res.string.settings_biometric_title),
            style = VmTheme.typography.subtitle.copy(color = VmTheme.colors.textPrimary),
        )
        BodyText(
            stringResource(
                when {
                    !available -> Res.string.settings_biometric_unavailable
                    enabled -> Res.string.settings_biometric_on
                    else -> Res.string.settings_biometric_off
                },
            ),
        )
        if (available && deviceKeys != null) {
            if (enabled) {
                VmButton(
                    text = stringResource(Res.string.settings_biometric_disable),
                    onClick = {
                        controller.disableDeviceUnlock(deviceKeys)
                        enabled = false
                    },
                )
                BodyText(stringResource(Res.string.settings_biometric_note))
            } else {
                VmButton(
                    text = stringResource(Res.string.settings_biometric_enable),
                    onClick = {
                        enableFailed = false
                        scope.launch {
                            when (controller.enableDeviceUnlock(deviceKeys)) {
                                DeviceUnlockChange.Enabled -> enabled = true
                                DeviceUnlockChange.Cancelled -> Unit
                                DeviceUnlockChange.Failed -> enableFailed = true
                            }
                        }
                    },
                    enabled = !controller.busy,
                )
            }
        }
        if (enableFailed) {
            StatusText(stringResource(Res.string.settings_biometric_failed), VmTheme.colors.danger)
        }

        ChoiceSetting(
            title = stringResource(Res.string.settings_idle_title),
            choices = AppSettings.IDLE_LOCK_CHOICES,
            selected = settings.current.idleLockSeconds,
            onSelect = { value -> settings.update { it.copy(idleLockSeconds = value) } },
        )
        ChoiceSetting(
            title = stringResource(Res.string.settings_reveal_title),
            choices = AppSettings.REVEAL_CHOICES,
            selected = settings.current.revealSeconds,
            onSelect = { value -> settings.update { it.copy(revealSeconds = value) } },
        )
        ChoiceSetting(
            title = stringResource(Res.string.settings_clipboard_title),
            choices = AppSettings.CLIPBOARD_CLEAR_CHOICES,
            selected = settings.current.clipboardClearSeconds,
            onSelect = { value -> settings.update { it.copy(clipboardClearSeconds = value) } },
        )
        BodyText(stringResource(Res.string.settings_clipboard_note))
        ChoiceSetting(
            title = stringResource(Res.string.settings_reauth_title),
            choices = AppSettings.REAUTH_WINDOW_CHOICES,
            selected = settings.current.reauthWindowSeconds,
            onSelect = { value -> settings.update { it.copy(reauthWindowSeconds = value) } },
        )
    }
}

/** A setting with a few fixed choices, shown as a row of chips. */
@Composable
private fun ChoiceSetting(
    title: String,
    choices: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    BasicText(text = title, style = VmTheme.typography.subtitle.copy(color = VmTheme.colors.textPrimary))
    Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
        for (seconds in choices) {
            VmChip(label = durationText(seconds), selected = seconds == selected, onClick = { onSelect(seconds) })
        }
    }
}

@Composable
private fun durationText(seconds: Int): String =
    if (seconds < SECONDS_PER_MINUTE) {
        stringResource(Res.string.settings_seconds, seconds)
    } else {
        stringResource(Res.string.settings_minutes, seconds / SECONDS_PER_MINUTE)
    }

private const val SECONDS_PER_MINUTE = 60
