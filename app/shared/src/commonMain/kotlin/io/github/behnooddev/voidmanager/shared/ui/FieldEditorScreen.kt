package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButton
import io.github.behnooddev.voidmanager.core.designsystem.components.VmButtonStyle
import io.github.behnooddev.voidmanager.core.designsystem.components.VmListItem
import io.github.behnooddev.voidmanager.core.designsystem.components.VmPasswordField
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTextField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy
import io.github.behnooddev.voidmanager.shared.people.CompositeInput
import io.github.behnooddev.voidmanager.shared.people.FieldInput
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.people.ProfileRow
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_save
import io.github.behnooddev.voidmanager.shared.resources.field_delete
import io.github.behnooddev.voidmanager.shared.resources.field_delete_confirm
import io.github.behnooddev.voidmanager.shared.resources.field_edit_title
import io.github.behnooddev.voidmanager.shared.resources.field_label
import io.github.behnooddev.voidmanager.shared.resources.field_new_title
import io.github.behnooddev.voidmanager.shared.resources.field_none_left
import io.github.behnooddev.voidmanager.shared.resources.field_note
import io.github.behnooddev.voidmanager.shared.resources.field_pick
import io.github.behnooddev.voidmanager.shared.resources.field_value
import io.github.behnooddev.voidmanager.shared.resources.loading
import io.github.behnooddev.voidmanager.shared.resources.password_hide
import io.github.behnooddev.voidmanager.shared.resources.password_show
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Adds a value ([valueId] null) or edits one. Composite fields are not editable yet. */
@Composable
internal fun FieldEditorScreen(
    model: PeopleModel,
    entityId: String,
    valueId: String?,
    onDone: () -> Unit,
    onActivity: () -> Unit,
) {
    LaunchedEffect(entityId) { model.loadProfile(entityId) }
    val view = model.profile?.takeIf { it.entity.id == entityId }
    val title = stringResource(if (valueId == null) Res.string.field_new_title else Res.string.field_edit_title)

    if (view == null) {
        ScreenFrame(model, title = title, onBack = onDone) { BodyText(stringResource(Res.string.loading)) }
        return
    }
    val existing: ProfileRow? =
        valueId?.let { id ->
            view.sections.flatMap { it.rows }.firstOrNull { it.valueId == id }
        }
    val hasValueOfType = { definitionId: String ->
        view.sections.any { s -> s.rows.any { it.definitionId == definitionId } }
    }

    // The type is chosen first when adding; it is fixed when editing.
    var chosenId by remember { mutableStateOf<String?>(null) }
    val definition: FieldDefinition? =
        existing?.definition ?: chosenId?.let { id -> FieldRegistry.definitions.firstOrNull { it.id == id } }

    if (definition == null) {
        ScreenFrame(model, title = title, onBack = onDone) {
            if (view.addable.isEmpty()) {
                BodyText(stringResource(Res.string.field_none_left))
            } else {
                BodyText(stringResource(Res.string.field_pick))
                for (option in view.addable) {
                    VmListItem(title = option.defaultLabel, onClick = { chosenId = option.id })
                }
            }
        }
        return
    }

    FieldForm(
        model = model,
        entityId = entityId,
        definition = definition,
        existing = existing,
        becomesPrimary = existing?.isPrimary ?: !hasValueOfType(definition.id),
        title = title,
        onDone = onDone,
        onActivity = onActivity,
    )
}

@Composable
private fun FieldForm(
    model: PeopleModel,
    entityId: String,
    definition: FieldDefinition,
    existing: ProfileRow?,
    becomesPrimary: Boolean,
    title: String,
    onDone: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Plain remember: the text of a protected value must not be written to saved state.
    var label by remember(existing?.valueId) { mutableStateOf(existing?.label.orEmpty()) }
    var scalar by remember(existing?.valueId) { mutableStateOf(existing?.text?.reveal().orEmpty()) }
    var parts by remember(existing?.valueId) {
        mutableStateOf(existing?.parts?.associate { it.key to it.text.reveal() } ?: emptyMap())
    }
    var note by remember(existing?.valueId) { mutableStateOf(existing?.note?.reveal().orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val composite = definition.isComposite
    val scalarIssue = if (composite) null else FieldInput.validate(definition.dataType, scalar)
    val partIssues = if (composite) CompositeInput.issues(definition, parts) else emptyMap()
    val canSave = if (composite) CompositeInput.canSave(definition, parts) else scalarIssue == null

    fun save() {
        if (saving || !canSave) return
        saving = true
        val input =
            FieldValueInput(
                definitionId = definition.id,
                label = label.trim().ifEmpty { null },
                value = if (composite) null else scalar.trim(),
                note = note.trim().ifEmpty { null },
                isPrimary = becomesPrimary,
                sortOrder = existing?.sortOrder ?: 0,
                sensitivity = existing?.sensitivity,
                sharingPolicy = existing?.sharingPolicy ?: SharingPolicy.Inherit,
                metadata = existing?.metadata,
                parts = if (composite) CompositeInput.cleaned(definition, parts) else emptyMap(),
            )
        scope.launch {
            if (model.saveField(entityId, existing?.valueId, input)) onDone() else saving = false
        }
    }

    ScreenFrame(model, title = title, onBack = onDone) {
        BodyText(definition.defaultLabel, color = VmTheme.colors.textPrimary)
        VmTextField(
            value = label,
            onValueChange = {
                label = it
                onActivity()
            },
            label = stringResource(Res.string.field_label),
        )
        if (composite) {
            for (part in definition.parts) {
                val text = parts[part.key].orEmpty()
                val onChange: (String) -> Unit = {
                    parts = parts + (part.key to it)
                    onActivity()
                }
                if (part.sensitivity == Sensitivity.Secret) {
                    VmPasswordField(
                        value = text,
                        onValueChange = onChange,
                        label = partLabel(part.key),
                        showLabel = stringResource(Res.string.password_show),
                        hideLabel = stringResource(Res.string.password_hide),
                    )
                } else {
                    VmTextField(
                        value = text,
                        onValueChange = onChange,
                        label = partLabel(part.key),
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardForPart(part.key)),
                    )
                }
                partIssues[part.key]?.let { issue ->
                    fieldIssueText(issue)?.let { StatusText(it, VmTheme.colors.warning) }
                }
            }
        } else {
            VmTextField(
                value = scalar,
                onValueChange = {
                    scalar = it
                    onActivity()
                },
                label = stringResource(Res.string.field_value),
                singleLine = definition.dataType != FieldDataType.Multiline,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardFor(definition.dataType)),
            )
            val issueText = scalarIssue?.let { fieldIssueText(it) }
            if (issueText != null && scalar.isNotBlank()) StatusText(issueText, VmTheme.colors.warning)
        }
        VmTextField(
            value = note,
            onValueChange = {
                note = it
                onActivity()
            },
            label = stringResource(Res.string.field_note),
            singleLine = false,
        )
        VmButton(
            text = stringResource(Res.string.action_save),
            onClick = ::save,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSave && !saving,
        )
        if (existing != null) {
            VmButton(
                text = stringResource(if (confirmDelete) Res.string.field_delete_confirm else Res.string.field_delete),
                onClick = {
                    if (!confirmDelete) {
                        confirmDelete = true
                    } else {
                        scope.launch { if (model.deleteField(entityId, existing.valueId)) onDone() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                style = VmButtonStyle.Secondary,
            )
        }
    }
}

private fun keyboardForPart(key: String): KeyboardType =
    when (key) {
        "phone" -> KeyboardType.Phone
        "email" -> KeyboardType.Email
        "website" -> KeyboardType.Uri
        "number", "postal_code" -> KeyboardType.Number
        else -> KeyboardType.Text
    }

private fun keyboardFor(type: FieldDataType): KeyboardType =
    when (type) {
        FieldDataType.Phone -> KeyboardType.Phone
        FieldDataType.Email -> KeyboardType.Email
        FieldDataType.Number -> KeyboardType.Number
        FieldDataType.Url -> KeyboardType.Uri
        else -> KeyboardType.Text
    }
