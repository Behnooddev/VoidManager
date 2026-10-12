package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.Sensitivity

/** The protection levels a screen offers. A value can be raised, and the app never lowers what is already set. */
object ProtectionLevels {
    /** The default level of the field and above, and never below what the value already has. */
    fun choices(
        fieldDefault: Sensitivity,
        current: Sensitivity?,
    ): List<Sensitivity> {
        val floor = maxOf(fieldDefault.level, current?.level ?: 0)
        return Sensitivity.entries.filter { it.level >= floor }
    }
}

/** What a new custom field needs. The data layer applies the same limits. */
object CustomFieldForm {
    const val MAX_LABEL_LENGTH = 60

    val TYPES: List<FieldDataType> =
        listOf(
            FieldDataType.Text,
            FieldDataType.Multiline,
            FieldDataType.Number,
            FieldDataType.Date,
            FieldDataType.Url,
            FieldDataType.Phone,
            FieldDataType.Email,
            FieldDataType.Username,
        )

    /** A new custom field starts at one of these; Secret needs re-authentication to show. */
    val LEVELS: List<Sensitivity> = listOf(Sensitivity.Personal, Sensitivity.Sensitive, Sensitivity.Secret)

    fun canSave(label: String): Boolean = label.trim().let { it.isNotEmpty() && it.length <= MAX_LABEL_LENGTH }
}
