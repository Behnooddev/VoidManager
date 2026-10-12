package io.github.behnooddev.voidmanager.core.backup

/**
 * Everything a vault holds, as plain data, for the logical backup payload. Protected values are
 * decrypted here; inside a backup file they are protected only by the backup key, so these types
 * must never be logged or kept longer than the export or restore call. Their `toString` prints no text.
 */
data class BackupSnapshot(
    val schemaVersion: Int,
    val definitions: List<SnapshotDefinition>,
    val entities: List<SnapshotEntity>,
    val values: List<SnapshotValue>,
    val relationships: List<SnapshotRelationship>,
) {
    override fun toString(): String =
        "BackupSnapshot(schemaVersion=$schemaVersion, definitions=${definitions.size}, entities=${entities.size}, " +
            "values=${values.size}, relationships=${relationships.size})"
}

data class SnapshotDefinitionPart(
    val key: String,
    val sensitivity: Int,
)

data class SnapshotDefinition(
    val id: String,
    val systemKey: String?,
    val section: Int,
    val dataType: Int,
    val defaultLabel: String,
    val labelOverride: String?,
    val defaultSensitivity: Int,
    val allowsMultiple: Boolean,
    val isUserDefined: Boolean,
    val isHidden: Boolean,
    val sortOrder: Int,
    val parts: List<SnapshotDefinitionPart>,
)

data class SnapshotEntity(
    val id: String,
    val kind: Int,
    val displayName: String,
    val isFavorite: Boolean,
    val primaryPhotoId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
) {
    override fun toString(): String = "SnapshotEntity(id=$id, kind=$kind)"
}

data class SnapshotPart(
    val key: String,
    val value: String?,
    val sensitivity: Int,
) {
    override fun toString(): String = "SnapshotPart(key=$key, sensitivity=$sensitivity)"
}

data class SnapshotValue(
    val id: String,
    val entityId: String,
    val definitionId: String,
    val label: String?,
    val value: String?,
    val note: String?,
    val isPrimary: Boolean,
    val sortOrder: Int,
    val sensitivity: Int,
    val sharingPolicy: Int,
    val metadata: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val parts: List<SnapshotPart>,
) {
    override fun toString(): String =
        "SnapshotValue(id=$id, definitionId=$definitionId, sensitivity=$sensitivity, parts=${parts.size})"
}

data class SnapshotRelationship(
    val id: String,
    val pairId: String,
    val fromEntityId: String,
    val toEntityId: String,
    val typeId: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    override fun toString(): String = "SnapshotRelationship(id=$id, typeId=$typeId)"
}
