package io.github.behnooddev.voidmanager.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing

/**
 * A password input that is masked until the user asks to see it. The visibility choice is plain
 * `remember` state, never saved: a password must not end up in a saved instance state.
 * The keyboard is asked not to learn or autocorrect the text.
 */
@Composable
fun VmPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm),
    ) {
        VmTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = Modifier.weight(1f),
            isSecret = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = imeAction,
                ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        VmButton(
            text = if (visible) hideLabel else showLabel,
            onClick = { visible = !visible },
            style = VmButtonStyle.Text,
        )
    }
}
