package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.FieldDefinition
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy

/**
 * One value on a profile. [text] is a [FieldText], which prints as a mask when the value is
 * protected; the screen reads the real text only after the user asks to reveal it.
 * For a composite value (address, card, account) [text] joins the parts that are not protected and
 * [hiddenParts] counts the protected parts that are left out.
 */
data class ProfileRow(
    val valueId: String,
    val definitionId: String,
    val definition: FieldDefinition?,
    val label: String?,
    val text: FieldText?,
    val note: FieldText?,
    val isPrimary: Boolean,
    val hiddenParts: Int,
    /** Kept so that editing a value does not reset what the user set on it. */
    val sensitivity: Sensitivity,
    val sharingPolicy: SharingPolicy,
    val sortOrder: Int,
    val metadata: String?,
) {
    val isProtected: Boolean get() = text?.isProtected == true || note?.isProtected == true
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
        val visibleParts = value.parts.filter { it.value != null && it.value?.isProtected == false }
        val text =
            if (definition?.isComposite == true) {
                val joined = visibleParts.joinToString(", ") { it.value?.reveal().orEmpty() }
                if (joined.isEmpty()) null else FieldText(joined, isProtected = false)
            } else {
                value.value
            }
        val hidden = if (definition?.isComposite == true) value.parts.count { it.value?.isProtected == true } else 0
        return ProfileRow(
            valueId = value.id,
            definitionId = value.definitionId,
            definition = definition,
            label = value.label,
            text = text,
            note = value.note,
            isPrimary = value.isPrimary,
            hiddenParts = hidden,
            sensitivity = value.sensitivity,
            sharingPolicy = value.sharingPolicy,
            sortOrder = value.sortOrder,
            metadata = value.metadata,
        )
    }
}
