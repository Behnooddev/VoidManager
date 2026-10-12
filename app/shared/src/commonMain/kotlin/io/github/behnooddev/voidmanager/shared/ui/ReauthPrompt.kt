package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmPasswordField
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSurface
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_cancel
import io.github.behnooddev.voidmanager.shared.resources.field_password
import io.github.behnooddev.voidmanager.shared.resources.password_hide
import io.github.behnooddev.voidmanager.shared.resources.password_show
import io.github.behnooddev.voidmanager.shared.resources.reauth_biometric
import io.github.behnooddev.voidmanager.shared.resources.reauth_confirm
import io.github.behnooddev.voidmanager.shared.resources.reauth_message
import io.github.behnooddev.voidmanager.shared.resources.reauth_title
import io.github.behnooddev.voidmanager.shared.resources.reauth_wrong
import io.github.behnooddev.voidmanager.shared.resources.unlock_wait
import io.github.behnooddev.voidmanager.shared.secrets.SecretsModel
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyProvider
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val WAIT_POLL_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Asks the user to prove who they are before a level 3 value is shown or copied: the master
 * password, or biometrics when they are set up. The password is kept in plain `remember` state.
 */
@Composable
internal fun ReauthPrompt(
    secrets: SecretsModel,
    controller: VaultController,
    deviceKeys: DeviceKeyProvider?,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var waitMillis by remember { mutableStateOf(controller.throttleRemainingMillis()) }
    val deviceReady = remember { deviceKeys != null && controller.deviceUnlockReady(deviceKeys) }
    val busy = controller.busy

    LaunchedEffect(wrong, busy) {
        while (true) {
            waitMillis = controller.throttleRemainingMillis()
            if (waitMillis <= 0L) break
            delay(WAIT_POLL_MILLIS)
        }
    }
    val waiting = waitMillis > 0L

    fun confirm() {
        if (password.isEmpty() || busy || waiting) return
        val chars = password.toCharArray()
        scope.launch {
            if (controller.verifyPassword(chars)) {
                password = ""
                secrets.reauthenticated()
            } else {
                wrong = true
            }
        }
    }

    VmSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.sm),
        ) {
            BasicText(
                text = stringResource(Res.string.reauth_title),
                style = VmTheme.typography.subtitle.copy(color = VmTheme.colors.textPrimary),
            )
            BodyText(stringResource(Res.string.reauth_message))
            VmPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    wrong = false
                    onActivity()
                },
                label = stringResource(Res.string.field_password),
                showLabel = stringResource(Res.string.password_show),
                hideLabel = stringResource(Res.string.password_hide),
                imeAction = ImeAction.Done,
                onDone = ::confirm,
            )
            if (wrong) StatusText(stringResource(Res.string.reauth_wrong), VmTheme.colors.danger)
            if (waiting) {
                val seconds = ((waitMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
                StatusText(stringResource(Res.string.unlock_wait, seconds), VmTheme.colors.warning)
            }
            VmButton(
                text = stringResource(Res.string.reauth_confirm),
                onClick = ::confirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = password.isNotEmpty() && !busy && !waiting,
            )
            if (deviceReady && deviceKeys != null) {
                VmButton(
                    text = stringResource(Res.string.reauth_biometric),
                    onClick = {
                        scope.launch { if (controller.reauthWithDevice(deviceKeys)) secrets.reauthenticated() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = VmButtonStyle.Secondary,
                    enabled = !busy,
                )
            }
            VmButton(
                text = stringResource(Res.string.action_cancel),
                onClick = secrets::cancelReauth,
                style = VmButtonStyle.Text,
            )
        }
    }
}
