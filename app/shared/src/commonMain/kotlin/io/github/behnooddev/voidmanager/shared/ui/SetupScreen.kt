package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmChip
import io.github.behnooddev.voidmanager.core.designsystem.components.VmPasswordField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.security.PasswordIssue
import io.github.behnooddev.voidmanager.core.security.PasswordPolicy
import io.github.behnooddev.voidmanager.core.security.PasswordStrength
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.field_confirm
import io.github.behnooddev.voidmanager.shared.resources.field_password
import io.github.behnooddev.voidmanager.shared.resources.issue_common
import io.github.behnooddev.voidmanager.shared.resources.issue_repeated
import io.github.behnooddev.voidmanager.shared.resources.issue_sequence
import io.github.behnooddev.voidmanager.shared.resources.issue_too_short
import io.github.behnooddev.voidmanager.shared.resources.password_hide
import io.github.behnooddev.voidmanager.shared.resources.password_show
import io.github.behnooddev.voidmanager.shared.resources.setup_ack
import io.github.behnooddev.voidmanager.shared.resources.setup_create
import io.github.behnooddev.voidmanager.shared.resources.setup_creating
import io.github.behnooddev.voidmanager.shared.resources.setup_intro
import io.github.behnooddev.voidmanager.shared.resources.setup_mismatch
import io.github.behnooddev.voidmanager.shared.resources.setup_title
import io.github.behnooddev.voidmanager.shared.resources.setup_warning
import io.github.behnooddev.voidmanager.shared.resources.strength_fair
import io.github.behnooddev.voidmanager.shared.resources.strength_good
import io.github.behnooddev.voidmanager.shared.resources.strength_strong
import io.github.behnooddev.voidmanager.shared.resources.strength_weak
import io.github.behnooddev.voidmanager.shared.vault.SetupForm
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SetupScreen(controller: VaultController) {
    val scope = rememberCoroutineScope()
    val policy = remember { PasswordPolicy() }

    // Plain remember, never rememberSaveable: a password must not be written to saved instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var acknowledged by remember { mutableStateOf(false) }

    val check = SetupForm.check(password, confirmation, acknowledged, policy)
    val busy = controller.busy

    fun create() {
        if (!check.canCreate || busy) return
        val chars = password.toCharArray()
        password = ""
        confirmation = ""
        scope.launch { controller.create(chars) }
    }

    AuthLayout(title = stringResource(Res.string.setup_title)) {
        BodyText(stringResource(Res.string.setup_intro))
        BodyText(stringResource(Res.string.setup_warning), color = VmTheme.colors.warning)

        VmPasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(Res.string.field_password),
            showLabel = stringResource(Res.string.password_show),
            hideLabel = stringResource(Res.string.password_hide),
        )
        if (password.isNotEmpty()) {
            BodyText(strengthText(check.assessment.strength))
            for (issue in check.assessment.issues) {
                StatusText(issueText(issue, policy.minLength), VmTheme.colors.warning)
            }
        }

        VmPasswordField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = stringResource(Res.string.field_confirm),
            showLabel = stringResource(Res.string.password_show),
            hideLabel = stringResource(Res.string.password_hide),
            imeAction = ImeAction.Done,
            onDone = ::create,
        )
        if (check.showMismatch) {
            StatusText(stringResource(Res.string.setup_mismatch), VmTheme.colors.danger)
        }

        VmChip(
            label = stringResource(Res.string.setup_ack),
            selected = acknowledged,
            onClick = { acknowledged = !acknowledged },
        )

        controller.failure?.let { StatusText(failureText(it), VmTheme.colors.danger) }

        VmButton(
            text = stringResource(if (busy) Res.string.setup_creating else Res.string.setup_create),
            onClick = ::create,
            modifier = Modifier.fillMaxWidth(),
            enabled = check.canCreate && !busy,
        )
    }
}

@Composable
private fun strengthText(strength: PasswordStrength): String =
    stringResource(
        when (strength) {
            PasswordStrength.Weak -> Res.string.strength_weak
            PasswordStrength.Fair -> Res.string.strength_fair
            PasswordStrength.Good -> Res.string.strength_good
            PasswordStrength.Strong -> Res.string.strength_strong
        },
    )

@Composable
private fun issueText(
    issue: PasswordIssue,
    minLength: Int,
): String =
    when (issue) {
        PasswordIssue.TooShort -> stringResource(Res.string.issue_too_short, minLength)
        PasswordIssue.CommonPassword -> stringResource(Res.string.issue_common)
        PasswordIssue.SingleRepeatedCharacter -> stringResource(Res.string.issue_repeated)
        PasswordIssue.SimpleSequence -> stringResource(Res.string.issue_sequence)
    }
