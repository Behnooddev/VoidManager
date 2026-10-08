package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSectionHeader
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSurface
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.ProfileRow
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_lock
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val HIDDEN_VALUE = "••••••••"

/** A person's profile, or the Personal profile when [isSelf] is set (which cannot be trashed). */
@Composable
internal fun ProfileScreen(
    model: PeopleModel,
    entityId: String,
    isSelf: Boolean,
    onBack: (() -> Unit)?,
    onRename: () -> Unit,
    onAddField: () -> Unit,
    onEditField: (String) -> Unit,
    onTrashed: () -> Unit,
    onLock: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(entityId) { model.loadProfile(entityId) }
    // The model holds one profile at a time, so make sure it is the one this screen asked for.
    val view = model.profile?.takeIf { it.entity.id == entityId }

    // Plain remember: which protected values are shown is forgotten when the screen or the vault goes away.
    var revealed by remember { mutableStateOf(emptySet<String>()) }

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

        Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
            VmButton(
                text =
                    stringResource(
                        if (entity.isFavorite) Res.string.profile_unfavorite else Res.string.profile_favorite,
                    ),
                onClick = { scope.launch { model.setFavorite(entity.id, !entity.isFavorite) } },
                style = VmButtonStyle.Secondary,
            )
            VmButton(
                text = stringResource(Res.string.profile_rename),
                onClick = onRename,
                style = VmButtonStyle.Secondary,
            )
        }

        if (view.isEmpty) BodyText(stringResource(Res.string.profile_empty))

        for (section in view.sections) {
            VmSectionHeader(sectionText(section.section))
            for (row in section.rows) {
                ProfileRowCard(
                    row = row,
                    shown = row.valueId in revealed,
                    onToggle = {
                        revealed =
                            if (row.valueId in revealed) revealed - row.valueId else revealed + row.valueId
                    },
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
    shown: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMakePrimary: (() -> Unit)?,
) {
    val title = row.label ?: row.definition?.defaultLabel ?: stringResource(Res.string.profile_other_field)
    VmSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.xs),
        ) {
            BasicText(
                text = if (row.isPrimary) "$title · ${stringResource(Res.string.profile_primary)}" else title,
                style = VmTheme.typography.caption.copy(color = VmTheme.colors.textSecondary),
            )
            val value = row.text
            if (value != null) {
                BasicText(
                    text = if (value.isProtected && !shown) HIDDEN_VALUE else value.reveal(),
                    style = VmTheme.typography.body.copy(color = VmTheme.colors.textPrimary),
                )
            }
            for (part in row.parts) {
                BasicText(
                    text = "${partLabel(
                        part.key,
                    )}: ${if (part.text.isProtected && !shown) HIDDEN_VALUE else part.text.reveal()}",
                    style = VmTheme.typography.body.copy(color = VmTheme.colors.textPrimary),
                )
            }
            val note = row.note
            if (note != null) {
                BasicText(
                    text = if (note.isProtected && !shown) HIDDEN_VALUE else note.reveal(),
                    style = VmTheme.typography.caption.copy(color = VmTheme.colors.textSecondary),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                if (row.isProtected) {
                    VmButton(
                        text = stringResource(if (shown) Res.string.profile_hide else Res.string.profile_show),
                        onClick = onToggle,
                        style = VmButtonStyle.Text,
                    )
                }
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
