package io.github.behnooddev.voidmanager.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import io.github.behnooddev.voidmanager.core.designsystem.components.VmChip
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSectionHeader
import io.github.behnooddev.voidmanager.core.designsystem.components.VmSurface
import io.github.behnooddev.voidmanager.core.designsystem.components.VmTextField
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmSpacing
import io.github.behnooddev.voidmanager.core.designsystem.theme.VmTheme
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.shared.people.CustomFieldForm
import io.github.behnooddev.voidmanager.shared.people.PeopleModel
import io.github.behnooddev.voidmanager.shared.resources.Res
import io.github.behnooddev.voidmanager.shared.resources.action_back
import io.github.behnooddev.voidmanager.shared.resources.action_cancel
import io.github.behnooddev.voidmanager.shared.resources.action_save
import io.github.behnooddev.voidmanager.shared.resources.fields_add_custom
import io.github.behnooddev.voidmanager.shared.resources.fields_badge_custom
import io.github.behnooddev.voidmanager.shared.resources.fields_badge_hidden
import io.github.behnooddev.voidmanager.shared.resources.fields_delete
import io.github.behnooddev.voidmanager.shared.resources.fields_delete_confirm
import io.github.behnooddev.voidmanager.shared.resources.fields_hide
import io.github.behnooddev.voidmanager.shared.resources.fields_intro
import io.github.behnooddev.voidmanager.shared.resources.fields_new_label
import io.github.behnooddev.voidmanager.shared.resources.fields_new_level
import io.github.behnooddev.voidmanager.shared.resources.fields_new_multiple
import io.github.behnooddev.voidmanager.shared.resources.fields_new_type
import io.github.behnooddev.voidmanager.shared.resources.fields_rename
import io.github.behnooddev.voidmanager.shared.resources.fields_rename_hint
import io.github.behnooddev.voidmanager.shared.resources.fields_reset_name
import io.github.behnooddev.voidmanager.shared.resources.fields_show
import io.github.behnooddev.voidmanager.shared.resources.settings_fields_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Which fields exist in this vault: hide the ones that are not used, rename them, and add or delete
 * fields of the user's own. Saved values are never touched by hiding a field.
 */
@Composable
internal fun FieldsScreen(
    model: PeopleModel,
    onBack: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(model) { model.loadDefinitions() }
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VmSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(VmSpacing.md),
    ) {
        VmButton(text = stringResource(Res.string.action_back), onClick = onBack, style = VmButtonStyle.Text)
        BasicText(
            text = stringResource(Res.string.settings_fields_title),
            style = VmTheme.typography.title.copy(color = VmTheme.colors.textPrimary),
        )
        BodyText(stringResource(Res.string.fields_intro))
        ProblemBanner(model)

        if (adding) {
            NewFieldForm(model, onDone = { adding = false }, onActivity = onActivity)
        } else {
            VmButton(
                text = stringResource(Res.string.fields_add_custom),
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val bySection =
            model.definitions
                .groupBy { it.section }
                .toList()
                .sortedBy { (section, _) -> section.code }
        for ((section, definitions) in bySection) {
            VmSectionHeader(sectionText(section))
            for (definition in definitions) {
                FieldRow(
                    definition = definition,
                    renaming = renaming == definition.id,
                    renameText = renameText,
                    confirmingDelete = confirmDelete == definition.id,
                    onToggleHidden = { scope.launch { model.setFieldHidden(definition.id, !definition.isHidden) } },
                    onStartRename = {
                        renaming = definition.id
                        renameText = definition.label
                    },
                    onRenameText = {
                        renameText = it
                        onActivity()
                    },
                    onSaveRename = {
                        scope.launch { if (model.renameField(definition.id, renameText)) renaming = null }
                    },
                    onResetName = {
                        scope.launch { if (model.renameField(definition.id, "")) renaming = null }
                    },
                    onCancelRename = { renaming = null },
                    onDelete = {
                        if (confirmDelete != definition.id) {
                            confirmDelete = definition.id
                        } else {
                            confirmDelete = null
                            scope.launch { model.deleteCustomField(definition.id) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldRow(
    definition: FieldDefinitionInfo,
    renaming: Boolean,
    renameText: String,
    confirmingDelete: Boolean,
    onToggleHidden: () -> Unit,
    onStartRename: () -> Unit,
    onRenameText: (String) -> Unit,
    onSaveRename: () -> Unit,
    onResetName: () -> Unit,
    onCancelRename: () -> Unit,
    onDelete: () -> Unit,
) {
    VmSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.sm),
        ) {
            val badges =
                buildList {
                    if (definition.isUserDefined) add(stringResource(Res.string.fields_badge_custom))
                    if (definition.isHidden) add(stringResource(Res.string.fields_badge_hidden))
                }
            BasicText(
                text = if (badges.isEmpty()) definition.label else "${definition.label} · ${badges.joinToString(", ")}",
                style = VmTheme.typography.body.copy(color = VmTheme.colors.textPrimary),
            )
            if (renaming) {
                VmTextField(
                    value = renameText,
                    onValueChange = onRenameText,
                    label = stringResource(Res.string.fields_rename_hint),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                    VmButton(
                        text = stringResource(Res.string.action_save),
                        onClick = onSaveRename,
                        enabled = renameText.isNotBlank(),
                    )
                    if (!definition.isUserDefined && definition.labelOverride != null) {
                        VmButton(
                            text = stringResource(Res.string.fields_reset_name),
                            onClick = onResetName,
                            style = VmButtonStyle.Secondary,
                        )
                    }
                    VmButton(
                        text = stringResource(Res.string.action_cancel),
                        onClick = onCancelRename,
                        style = VmButtonStyle.Text,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                    VmButton(
                        text =
                            stringResource(
                                if (definition.isHidden) Res.string.fields_show else Res.string.fields_hide,
                            ),
                        onClick = onToggleHidden,
                        style = VmButtonStyle.Text,
                    )
                    VmButton(
                        text = stringResource(Res.string.fields_rename),
                        onClick = onStartRename,
                        style = VmButtonStyle.Text,
                    )
                    if (definition.isUserDefined) {
                        VmButton(
                            text =
                                stringResource(
                                    if (confirmingDelete) {
                                        Res.string.fields_delete_confirm
                                    } else {
                                        Res.string.fields_delete
                                    },
                                ),
                            onClick = onDelete,
                            style = VmButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewFieldForm(
    model: PeopleModel,
    onDone: () -> Unit,
    onActivity: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(FieldDataType.Text) }
    var level by remember { mutableStateOf(Sensitivity.Personal) }
    var multiple by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    VmSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(VmSpacing.md),
            verticalArrangement = Arrangement.spacedBy(VmSpacing.md),
        ) {
            VmTextField(
                value = label,
                onValueChange = {
                    label = it
                    onActivity()
                },
                label = stringResource(Res.string.fields_new_label),
            )
            BasicText(
                text = stringResource(Res.string.fields_new_type),
                style = VmTheme.typography.subtitle.copy(color = VmTheme.colors.textPrimary),
            )
            for (rowTypes in CustomFieldForm.TYPES.chunked(TYPES_PER_ROW)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                    for (candidate in rowTypes) {
                        VmChip(
                            label = typeText(candidate),
                            selected = candidate == type,
                            onClick = { type = candidate },
                        )
                    }
                }
            }
            LevelPicker(
                title = stringResource(Res.string.fields_new_level),
                choices = CustomFieldForm.LEVELS,
                selected = level,
                onSelect = { level = it },
            )
            VmChip(label = stringResource(Res.string.fields_new_multiple), selected = multiple, onClick = {
                multiple =
                    !multiple
            })
            Row(horizontalArrangement = Arrangement.spacedBy(VmSpacing.sm)) {
                VmButton(
                    text = stringResource(Res.string.action_save),
                    onClick = {
                        saving = true
                        scope.launch {
                            if (model.createCustomField(label, type, level, multiple)) onDone() else saving = false
                        }
                    },
                    enabled = CustomFieldForm.canSave(label) && !saving,
                )
                VmButton(text = stringResource(Res.string.action_cancel), onClick = onDone, style = VmButtonStyle.Text)
            }
        }
    }
}

private const val TYPES_PER_ROW = 2
