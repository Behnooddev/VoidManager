package io.github.behnooddev.voidmanager.core.model

/**
 * A field definition as one vault holds it: a built-in field with this vault's overrides (hidden,
 * relabeled, ordered), or a field the user defined. [FieldDefinition] is the shipped registry entry;
 * this is what the screens work with.
 */
data class FieldDefinitionInfo(
    val id: String,
    /** Null for a user-defined field. */
    val systemKey: String?,
    val section: FieldSection,
    val dataType: FieldDataType,
    val defaultLabel: String,
    val labelOverride: String?,
    val defaultSensitivity: Sensitivity,
    val allowsMultiple: Boolean,
    val parts: List<FieldPartDefinition>,
    val isUserDefined: Boolean,
    /** A hidden field is left out of the add list. Values already saved stay visible. */
    val isHidden: Boolean,
    val sortOrder: Int,
) {
    val isComposite: Boolean get() = parts.isNotEmpty()

    /** What the user sees: the override when there is one, otherwise the default label. */
    val label: String get() = labelOverride?.takeIf { it.isNotBlank() } ?: defaultLabel
}

fun FieldDefinition.toInfo(sortOrder: Int = 0): FieldDefinitionInfo =
    FieldDefinitionInfo(
        id = id,
        systemKey = systemKey,
        section = section,
        dataType = dataType,
        defaultLabel = defaultLabel,
        labelOverride = null,
        defaultSensitivity = defaultSensitivity,
        allowsMultiple = allowsMultiple,
        parts = parts,
        isUserDefined = false,
        isHidden = false,
        sortOrder = sortOrder,
    )
