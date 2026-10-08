package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy

/** One filled part of a composite value. [text] prints as a mask when the part is protected. */
data class PartRow(
    val key: String,
    val text: FieldText,
    val sensitivity: Sensitivity,
)

/**
 * One value on a profile. [text] is a [FieldText], which prints as a mask when the value is
 * protected; the screen reads the real text only after the user asks to reveal it.
 * A composite value (address, card, account, work, education) has no [text]; its filled parts are in [parts].
 */
data class ProfileRow(
    val valueId: String,
    val definitionId: String,
    val definition: FieldDefinition?,
    val label: String?,
    val text: FieldText?,
    val note: FieldText?,
    val isPrimary: Boolean,
    val parts: List<PartRow>,
    /** Kept so that editing a value does not reset what the user set on it. */
    val sensitivity: Sensitivity,
    val sharingPolicy: SharingPolicy,
    val sortOrder: Int,
    val metadata: String?,
) {
    val isProtected: Boolean get() =
        text?.isProtected == true ||
            note?.isProtected == true ||
            parts.any { it.text.isProtected }
    val canEdit: Boolean get() = definition != null && FieldInput.isEditable(definition)
}

data class ProfileSection(
    val section: FieldSection,
    val rows: List<ProfileRow>,
)

data class ProfileView(
    val entity: Entity,
    val sections: List<ProfileSection>,
    /** Fields that can still be added, for the Add button. */
    val addable: List<FieldDefinition>,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

object ProfileBuilder {
    private val BY_ID: Map<String, FieldDefinition> = FieldRegistry.definitions.associateBy { it.id }

    fun build(
        entity: Entity,
        values: List<FieldValue>,
    ): ProfileView {
        val rows =
            values
                .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
                .map { toRow(it) }
        val sections =
            rows
                .groupBy { it.definition?.section ?: FieldSection.Custom }
                .toList()
                .sortedBy { (section, _) -> section.code }
                .map { (section, sectionRows) ->
                    ProfileSection(section, sectionRows.sortedByDescending { it.isPrimary })
                }
        return ProfileView(entity, sections, FieldInput.addable(values))
    }

    private fun toRow(value: FieldValue): ProfileRow {
        val definition = BY_ID[value.definitionId]
        val order = definition?.parts?.map { it.key }.orEmpty()
        val parts =
            value.parts
                .mapNotNull { part -> part.value?.let { PartRow(part.key, it, part.sensitivity) } }
                .sortedBy { part -> order.indexOf(part.key).let { if (it < 0) order.size else it } }
        return ProfileRow(
            valueId = value.id,
            definitionId = value.definitionId,
            definition = definition,
            label = value.label,
            text = if (parts.isEmpty()) value.value else null,
            note = value.note,
            isPrimary = value.isPrimary,
            parts = parts,
            sensitivity = value.sensitivity,
            sharingPolicy = value.sharingPolicy,
            sortOrder = value.sortOrder,
            metadata = value.metadata,
        )
    }
}

/**
 * The input that stores this row again, for changes that touch only a flag such as the primary mark.
 * It reads protected text, so it is meant for a call that goes straight to the repository.
 */
fun ProfileRow.toInput(isPrimary: Boolean): FieldValueInput =
    FieldValueInput(
        definitionId = definitionId,
        label = label,
        value = if (parts.isEmpty()) text?.reveal() else null,
        note = note?.reveal(),
        isPrimary = isPrimary,
        sortOrder = sortOrder,
        sensitivity = sensitivity,
        sharingPolicy = sharingPolicy,
        metadata = metadata,
        parts = parts.associate { it.key to it.text.reveal() },
    )
