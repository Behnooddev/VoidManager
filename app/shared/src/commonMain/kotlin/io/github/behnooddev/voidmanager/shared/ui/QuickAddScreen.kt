package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTextField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.QuickAdd
import io.github.behnooddev.voidmanager.shared.people.QuickAddInput
import io.github.behnooddev.voidmanager.shared.people.QuickAddIssue
import io.github.behnooddev.voidmanager.shared.people.QuickAddResult
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_save
import io.github.behnooddev.voidmanager.shared.resources.quickadd_email
import io.github.behnooddev.voidmanager.shared.resources.quickadd_email_invalid
import io.github.behnooddev.voidmanager.shared.resources.quickadd_name
import io.github.behnooddev.voidmanager.shared.resources.quickadd_note
import io.github.behnooddev.voidmanager.shared.resources.quickadd_phone
import io.github.behnooddev.voidmanager.shared.resources.quickadd_phone_invalid
import io.github.behnooddev.voidmanager.shared.resources.quickadd_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun QuickAddScreen(
    model: PeopleModel,
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(QuickAddInput()) }
    var saving by remember { mutableStateOf(false) }
    val issues = QuickAdd.check(input)

    fun save() {
        if (saving || issues.isNotEmpty()) return
        saving = true
        scope.launch {
            when (val result = model.quickAdd(input)) {
                is QuickAddResult.Saved -> onSaved(result.entityId)
                is QuickAddResult.Partial -> onSaved(result.entityId)
                QuickAddResult.Invalid, QuickAddResult.Failed -> saving = false
            }
        }
    }

    ScreenFrame(model, title = stringResource(Res.string.quickadd_title), onBack = onBack) {
        VmTextField(
            value = input.name,
            onValueChange = {
                input = input.copy(name = it)
                onActivity()
            },
            label = stringResource(Res.string.quickadd_name),
        )
        VmTextField(
            value = input.phone,
            onValueChange = {
                input = input.copy(phone = it)
                onActivity()
            },
            label = stringResource(Res.string.quickadd_phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        if (QuickAddIssue.PhoneInvalid in issues) {
            StatusText(stringResource(Res.string.quickadd_phone_invalid), VmTheme.colors.warning)
        }
        VmTextField(
            value = input.email,
            onValueChange = {
                input = input.copy(email = it)
                onActivity()
            },
            label = stringResource(Res.string.quickadd_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        if (QuickAddIssue.EmailInvalid in issues) {
            StatusText(stringResource(Res.string.quickadd_email_invalid), VmTheme.colors.warning)
        }
        VmTextField(
            value = input.note,
            onValueChange = {
                input = input.copy(note = it)
                onActivity()
            },
            label = stringResource(Res.string.quickadd_note),
            singleLine = false,
        )
        VmButton(
            text = stringResource(Res.string.action_save),
            onClick = ::save,
            modifier = Modifier.fillMaxWidth(),
            enabled = issues.isEmpty() && !saving,
        )
    }
}
