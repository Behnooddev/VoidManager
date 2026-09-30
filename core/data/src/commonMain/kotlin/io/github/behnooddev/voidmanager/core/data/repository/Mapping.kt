package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Relationship

internal fun mapEntity(
    id: String,
    kind: Long,
    displayName: String,
    sortKey: String,
    isFavorite: Long,
    primaryPhotoId: String?,
    createdAt: Long,
    updatedAt: Long,
    deletedAt: Long?,
): Entity =
    Entity(
        id = id,
        kind = EntityKind.fromCode(kind.toInt()),
        displayName = displayName,
        isFavorite = isFavorite == 1L,
        primaryPhotoId = primaryPhotoId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

internal fun mapRelationship(
    id: String,
    pairId: String,
    fromEntityId: String,
    toEntityId: String,
    typeId: String,
    note: String?,
    createdAt: Long,
    updatedAt: Long,
): Relationship =
    Relationship(
        id = id,
        pairId = pairId,
        fromEntityId = fromEntityId,
        toEntityId = toEntityId,
        typeId = typeId,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun failure(
    reason: FailureReason,
    detail: String,
): Outcome.Failure = Outcome.Failure(reason, detail)

internal fun Boolean.toLong(): Long = if (this) 1L else 0L
