package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSectionHeader
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSurface
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.ProfileRow
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_lock
import io.github.behnooddev.voidmanager.shared.resources.copied_cleared
import io.github.behnooddev.voidmanager.shared.resources.copied_plain
import io.github.behnooddev.voidmanager.shared.resources.copy_action
import io.github.behnooddev.voidmanager.shared.resources.copy_failed
import io.github.behnooddev.voidmanager.shared.resources.loading
import io.github.behnooddev.voidmanager.shared.resources.profile_add_field
import io.github.behnooddev.voidmanager.shared.resources.profile_edit
import io.github.behnooddev.voidmanager.shared.resources.profile_empty
import io.github.behnooddev.voidmanager.shared.resources.profile_favorite
import io.github.behnooddev.voidmanager.shared.resources.profile_hide
import io.github.behnooddev.voidmanager.shared.resources.profile_make_primary
import io.github.behnooddev.voidmanager.shared.resources.profile_move_trash
import io.github.behnooddev.voidmanager.shared.resources.profile_not_found
import io.github.behnooddev.voidmanager.shared.resources.profile_other_field
import io.github.behnooddev.voidmanager.shared.resources.profile_primary
import io.github.behnooddev.voidmanager.shared.resources.profile_rename
import io.github.behnooddev.voidmanager.shared.resources.profile_show
import io.github.behnooddev.voidmanager.shared.resources.profile_unfavorite
import io.github.behnooddev.voidmanager.shared.secrets.CopyNotice
import io.github.behnooddev.voidmanager.shared.secrets.SecretsModel
import io.github.behnooddev.voidmanager.shared.vault.DeviceKeyProvider
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val HIDDEN_VALUE = "••••••••"
private const val TICK_MILLIS = 1_000L

/** A person's profile, or the Personal profile when [isSelf] is set (which cannot be trashed). */
@Composable
internal fun ProfileScreen(
    model: PeopleModel,
    secrets: SecretsModel,
    controller: VaultController,
    deviceKeys: DeviceKeyProvider?,
    entityId: String,
    isSelf: Boolean,
    onBack: (() -> Unit)?,
    onRename: () -> Unit,
    onAddField: () -> Unit,
    onEditField: (String) -> Unit,
    onTrashed: () -> Unit,
    onLock: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(entityId) { model.loadProfile(entityId) }
    // The model holds one profile at a time, so make sure it is the one this screen asked for.
    val view = model.profile?.takeIf { it.entity.id == entityId }

    // A shown value hides itself after the reveal time, and when this screen goes away.
    LaunchedEffect(secrets) {
        while (true) {
            delay(TICK_MILLIS)
            secrets.tick()
        }
    }
    DisposableEffect(secrets) { onDispose { secrets.hideAll() } }

    ScreenFrame(
        model = model,
        title = view?.entity?.displayName.orEmpty(),
        onBack = onBack,
        actions = {
            VmButton(text = stringResource(Res.string.action_lock), onClick = onLock, style = VmButtonStyle.Text)
        },
    ) {
        if (view == null) {
            BodyText(stringResource(if (model.problem != null) Res.string.profile_not_found else Res.string.loading))
            return@ScreenFrame
        }
        val entity = view.entity

        if (secrets.needsReauth) ReauthPrompt(secrets, controller, deviceKeys, onActivity)
        when (val notice = secrets.notice) {
            is CopyNotice.Copied ->
                StatusText(
                    if (notice.clearAfterSeconds != null) {
                        stringResource(Res.string.copied_cleared, notice.clearAfterSeconds)
                    } else {
                        stringResource(Res.string.copied_plain)
                    },
                    VmTheme.colors.textSecondary,
                )
            CopyNotice.Failed -> StatusText(stringResource(Res.string.copy_failed), VmTheme.colors.danger)
            null -> Unit
        }

        VmButton(
            text =
                stringResource(
                    if (entity.isFavorite) Res.string.profile_unfavorite else Res.string.profile_favorite,
                ),
            onClick = { scope.launch { model.setFavorite(entity.id, !entity.isFavorite) } },
            modifier = Modifier.fillMaxWidth(),
            style = VmButtonStyle.Secondary,
        )
        VmButton(
            text = stringResource(Res.string.profile_rename),
            onClick = onRename,
            modifier = Modifier.fillMaxWidth(),
            style = VmButtonStyle.Secondary,
        )

        if (view.isEmpty) BodyText(stringResource(Res.string.profile_empty))

        for (section in view.sections) {
            VmSectionHeader(sectionText(section.section))
            for (row in section.rows) {
                ProfileRowCard(
                    row = row,
                    secrets = secrets,
                    onEdit = { onEditField(row.valueId) },
                    onMakePrimary =
                        if (!row.isPrimary &&
                            row.definition?.allowsMultiple == true &&
                            section.rows.count { it.definitionId == row.definitionId } > 1
                        ) {
                            { scope.launch { model.makePrimary(entity.id, row) } }
                        } else {
                            null
                        },
                )
            }
        }

        if (view.addable.isNotEmpty()) {
            VmButton(
                text = stringResource(Res.string.profile_add_field),
                onClick = onAddField,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!isSelf) {
            VmButton(
                text = stringResource(Res.string.profile_move_trash),
                onClick = { scope.launch { if (model.moveToTrash(entity.id)) onTrashed() } },
                modifier = Modifier.fillMaxWidth(),
                style = VmButtonStyle.Secondary,
            )
        }
    }
}

@Composable
private fun ProfileRowCard(
    row: ProfileRow,
    secrets: SecretsModel,
    onEdit: () -> Unit,
    onMakePrimary: (() -> Unit)?,
) {
    val title = row.label ?: row.definition?.defaultLabel ?: stringResource(Res.string.profile_other_field)
    VmSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.sm),
        ) {
            BasicText(
                text = if (row.isPrimary) "$title · ${stringResource(Res.string.profile_primary)}" else title,
                style = VmTheme.typography.caption.copy(color = VmTheme.colors.textSecondary),
            )
            val value = row.text
            if (value != null) {
                SecretLine(null, value, row.sensitivity, key = row.valueId, secrets = secrets, emphasis = true)
            }
            for (part in row.parts) {
                SecretLine(
                    partLabel(part.key),
                    part.text,
                    part.sensitivity,
                    key = "${row.valueId}:${part.key}",
                    secrets = secrets,
                    emphasis = true,
                )
            }
            val note = row.note
            if (note != null) {
                SecretLine(
                    null,
                    note,
                    row.sensitivity,
                    key = "${row.valueId}#note",
                    secrets = secrets,
                    emphasis = false,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                if (row.canEdit) {
                    VmButton(
                        text = stringResource(Res.string.profile_edit),
                        onClick = onEdit,
                        style = VmButtonStyle.Text,
                    )
                }
                if (onMakePrimary != null) {
                    VmButton(
                        text = stringResource(Res.string.profile_make_primary),
                        onClick = onMakePrimary,
                        style = VmButtonStyle.Text,
                    )
                }
            }
        }
    }
}

/**
 * One line of a value, with Show and Hide for protected text and Copy. A protected line stays
 * masked until it is shown on purpose, and a level 3 line asks for re-authentication first.
 */
@Composable
private fun SecretLine(
    label: String?,
    text: FieldText,
    sensitivity: Sensitivity,
    key: String,
    secrets: SecretsModel,
    emphasis: Boolean,
) {
    val shown = !text.isProtected || secrets.isRevealed(key)
    val shownText = if (shown) text.reveal() else HIDDEN_VALUE
    val style =
        if (emphasis) {
            VmTheme.typography.body.copy(
                color = VmTheme.colors.textPrimary,
            )
        } else {
            VmTheme.typography.caption.copy(color = VmTheme.colors.textSecondary)
        }
    Column(verticalArrangement = Arrangement.spacedBy(VmSpacing.xs)) {
        BasicText(text = if (label != null) "$label: $shownText" else shownText, style = style)
        if (text.isProtected || secrets.canCopy) {
            Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                if (text.isProtected) {
                    VmButton(
                        text = stringResource(if (shown) Res.string.profile_hide else Res.string.profile_show),
                        onClick = { secrets.toggleReveal(key, sensitivity) },
                        style = VmButtonStyle.Text,
                    )
                }
                if (secrets.canCopy) {
                    VmButton(
                        text = stringResource(Res.string.copy_action),
                        onClick = { secrets.copy(text.reveal(), sensitivity, text.isProtected) },
                        style = VmButtonStyle.Text,
                    )
                }
            }
        }
    }
}
