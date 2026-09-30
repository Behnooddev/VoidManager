package io.github.behnooddev.voidmanager.core.model

enum class FieldSection(
    val code: Int,
    val key: String,
) {
    Identity(0, "identity"),
    Contact(1, "contact"),
    Social(2, "social"),
    Accounts(3, "accounts"),
    Financial(4, "financial"),
    Address(5, "address"),
    Work(6, "work"),
    Education(7, "education"),
    Dates(8, "dates"),
    Notes(9, "notes"),
    Custom(10, "custom"),
    ;

    companion object {
        fun fromCode(code: Int): FieldSection =
            entries.firstOrNull { it.code == code } ?: error("Unknown field section $code")
    }
}

enum class FieldDataType(
    val code: Int,
) {
    Text(0),
    Multiline(1),
    Number(2),
    Date(3),
    Time(4),
    Url(5),
    Phone(6),
    Email(7),
    Username(8),
    Boolean(9),
    Selection(10),
    Account(11),
    Address(12),
    Card(13),
    Education(14),
    Work(15),
    ;

    companion object {
        fun fromCode(code: Int): FieldDataType =
            entries.firstOrNull { it.code == code } ?: error("Unknown field data type $code")
    }
}

/** One named part of a composite value, such as `password` in an account. */
data class FieldPartDefinition(
    val key: String,
    val sensitivity: Sensitivity,
)

/** A field the application ships with. Built-in ids are derived from [systemKey] so they are stable. */
data class FieldDefinition(
    val systemKey: String,
    val section: FieldSection,
    val dataType: FieldDataType,
    val defaultLabel: String,
    val defaultSensitivity: Sensitivity,
    val allowsMultiple: Boolean,
    val parts: List<FieldPartDefinition> = emptyList(),
    val schemaVersion: Int = 1,
) {
    val isComposite: Boolean get() = parts.isNotEmpty()

    val id: String get() = idForSystemKey(systemKey)

    companion object {
        fun idForSystemKey(systemKey: String): String = "sys.$systemKey"
    }
}
