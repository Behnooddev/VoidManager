package io.github.behnooddev.voidmanager.core.model

/** Classification of a stored value. The level drives storage, search, sharing, export and logging rules. */
enum class Sensitivity(
    val level: Int,
) {
    Public(0),
    Personal(1),
    Sensitive(2),
    Secret(3),
    ;

    /** Levels 2 and above are stored only as ciphertext. */
    val isStoredEncrypted: Boolean get() = level >= ENCRYPTED_FROM_LEVEL

    /** Only levels below 2 may appear in the search index. */
    val isSearchable: Boolean get() = level < ENCRYPTED_FROM_LEVEL

    companion object {
        private const val ENCRYPTED_FROM_LEVEL = 2

        fun fromLevel(level: Int): Sensitivity =
            entries.firstOrNull { it.level == level } ?: error("Unknown sensitivity level $level")
    }
}

enum class SharingPolicy(
    val code: Int,
) {
    Inherit(0),
    Allow(1),
    Ask(2),
    Never(3),
    ;

    companion object {
        fun fromCode(code: Int): SharingPolicy =
            entries.firstOrNull { it.code == code } ?: error("Unknown sharing policy $code")
    }
}

enum class EntityKind(
    val code: Int,
) {
    Person(0),
    Self(1),
    ;

    companion object {
        fun fromCode(code: Int): EntityKind =
            entries.firstOrNull { it.code == code } ?: error("Unknown entity kind $code")
    }
}
