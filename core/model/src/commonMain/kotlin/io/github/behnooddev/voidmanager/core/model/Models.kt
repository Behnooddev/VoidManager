package io.github.behnooddev.voidmanager.core.model

data class Entity(
    val id: String,
    val kind: EntityKind,
    val displayName: String,
    val isFavorite: Boolean,
    val primaryPhotoId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Non-null while the entity is in Trash. */
    val deletedAt: Long?,
) {
    val isTrashed: Boolean get() = deletedAt != null
}

data class FieldPart(
    val key: String,
    val value: FieldText?,
    val sensitivity: Sensitivity,
)

data class FieldValue(
    val id: String,
    val entityId: String,
    val definitionId: String,
    val label: String?,
    val value: FieldText?,
    val note: FieldText?,
    val isPrimary: Boolean,
    val sortOrder: Int,
    val sensitivity: Sensitivity,
    val sharingPolicy: SharingPolicy,
    val metadata: String?,
    val parts: List<FieldPart>,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Input for creating or replacing a field value. Plain text; the repository applies the sensitivity rules. */
data class FieldValueInput(
    val definitionId: String,
    val label: String? = null,
    val value: String? = null,
    val note: String? = null,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    /** Requested sensitivity. Null means the definition default. It can raise but not lower the default. */
    val sensitivity: Sensitivity? = null,
    val sharingPolicy: SharingPolicy = SharingPolicy.Inherit,
    val metadata: String? = null,
    val parts: Map<String, String> = emptyMap(),
)

data class Relationship(
    val id: String,
    val pairId: String,
    val fromEntityId: String,
    val toEntityId: String,
    val typeId: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Result type for operations that can fail for reasons the caller should handle. */
sealed interface Outcome<out T> {
    data class Success<T>(
        val value: T,
    ) : Outcome<T>

    data class Failure(
        val reason: FailureReason,
        val detail: String,
    ) : Outcome<Nothing>
}

enum class FailureReason {
    NotFound,
    InvalidInput,
    SensitivityLowered,
    NotAllowed,
    Conflict,
}
