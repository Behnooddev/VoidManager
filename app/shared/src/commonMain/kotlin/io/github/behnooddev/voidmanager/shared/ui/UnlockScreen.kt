package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.fillMaxWidth
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
import io.github.behnooddev.voidmanager.core.designsystem.components.VmPasswordField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.field_password
import io.github.behnooddev.voidmanager.shared.resources.password_hide
import io.github.behnooddev.voidmanager.shared.resources.password_show
import io.github.behnooddev.voidmanager.shared.resources.unlock_button
import io.github.behnooddev.voidmanager.shared.resources.unlock_title
import io.github.behnooddev.voidmanager.shared.resources.unlock_wait
import io.github.behnooddev.voidmanager.shared.resources.unlock_working
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val WAIT_POLL_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L

@Composable
internal fun UnlockScreen(controller: VaultController) {
    val scope = rememberCoroutineScope()

    // Plain remember, never rememberSaveable: a password must not be written to saved instance state.
    var password by remember { mutableStateOf("") }
    var waitMillis by remember { mutableStateOf(controller.throttleRemainingMillis()) }

    // Re-reads the throttle after every attempt and counts the wait down until it ends.
    LaunchedEffect(controller.failure, controller.busy) {
        while (true) {
            waitMillis = controller.throttleRemainingMillis()
            if (waitMillis <= 0L) break
            delay(WAIT_POLL_MILLIS)
        }
    }

    val busy = controller.busy
    val waiting = waitMillis > 0L

    fun unlock() {
        if (password.isEmpty() || busy || waiting) return
        val chars = password.toCharArray()
        scope.launch { controller.unlock(chars) }
    }

    AuthLayout(title = stringResource(Res.string.unlock_title)) {
        VmPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(Res.string.field_password),
            showLabel = stringResource(Res.string.password_show),
            hideLabel = stringResource(Res.string.password_hide),
            imeAction = ImeAction.Done,
            onDone = ::unlock,
        )

        controller.failure?.let { StatusText(failureText(it), VmTheme.colors.danger) }
        if (waiting) {
            val seconds = ((waitMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
            StatusText(stringResource(Res.string.unlock_wait, seconds), VmTheme.colors.warning)
        }

        VmButton(
            text = stringResource(if (busy) Res.string.unlock_working else Res.string.unlock_button),
            onClick = ::unlock,
            modifier = Modifier.fillMaxWidth(),
            enabled = password.isNotEmpty() && !busy && !waiting,
        )
    }
}
