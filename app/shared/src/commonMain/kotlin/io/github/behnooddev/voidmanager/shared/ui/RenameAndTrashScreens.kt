package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSurface
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTextField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_cancel
import io.github.behnooddev.voidmanager.shared.resources.action_save
import io.github.behnooddev.voidmanager.shared.resources.loading
import io.github.behnooddev.voidmanager.shared.resources.rename_name
import io.github.behnooddev.voidmanager.shared.resources.rename_title
import io.github.behnooddev.voidmanager.shared.resources.trash_delete
import io.github.behnooddev.voidmanager.shared.resources.trash_delete_confirm
import io.github.behnooddev.voidmanager.shared.resources.trash_empty
import io.github.behnooddev.voidmanager.shared.resources.trash_restore
import io.github.behnooddev.voidmanager.shared.resources.trash_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RenameScreen(
    model: PeopleModel,
    entityId: String,
    onDone: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(entityId) { model.loadProfile(entityId) }
    val current = model.profile?.takeIf { it.entity.id == entityId }?.entity
    var name by remember(current?.id) { mutableStateOf(current?.displayName.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    ScreenFrame(model, title = stringResource(Res.string.rename_title), onBack = onDone) {
        if (current == null) {
            BodyText(stringResource(Res.string.loading))
            return@ScreenFrame
        }
        VmTextField(
            value = name,
            onValueChange = {
                name = it
                onActivity()
            },
            label = stringResource(Res.string.rename_name),
        )
        VmButton(
            text = stringResource(Res.string.action_save),
            onClick = {
                saving = true
                scope.launch { if (model.rename(entityId, name)) onDone() else saving = false }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && !saving,
        )
    }
}

@Composable
internal fun TrashScreen(
    model: PeopleModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(model) { model.loadTrash() }
    var confirming by remember { mutableStateOf<String?>(null) }

    ScreenFrame(model, title = stringResource(Res.string.trash_title), onBack = onBack) {
        if (model.trashed.isEmpty()) BodyText(stringResource(Res.string.trash_empty))
        for (person in model.trashed) {
            VmSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(VmSpacing.sm),
                ) {
                    BodyText(person.displayName, color = VmTheme.colors.textPrimary)
                    if (confirming == person.id) {
                        BodyText(stringResource(Res.string.trash_delete_confirm), color = VmTheme.colors.danger)
                        Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                            VmButton(
                                text = stringResource(Res.string.trash_delete),
                                onClick = {
                                    confirming = null
                                    scope.launch { model.purge(person.id) }
                                },
                            )
                            VmButton(
                                text = stringResource(Res.string.action_cancel),
                                onClick = { confirming = null },
                                style = VmButtonStyle.Text,
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                            VmButton(
                                text = stringResource(Res.string.trash_restore),
                                onClick = { scope.launch { model.restore(person.id) } },
                                style = VmButtonStyle.Secondary,
                            )
                            VmButton(
                                text = stringResource(Res.string.trash_delete),
                                onClick = { confirming = person.id },
                                style = VmButtonStyle.Text,
                            )
                        }
                    }
                }
            }
        }
    }
}
