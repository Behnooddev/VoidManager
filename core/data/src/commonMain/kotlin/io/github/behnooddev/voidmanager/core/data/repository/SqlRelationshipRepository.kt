package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.IdGenerator
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Relationship
import io.github.behnooddev.voidmanager.core.model.RelationshipType
import io.github.behnooddev.voidmanager.core.model.RelationshipTypes

internal class SqlRelationshipRepository(
    private val database: VoidManagerDatabase,
    private val ids: IdGenerator,
    private val clock: () -> Long,
) : RelationshipRepository {
    private val queries get() = database.relationshipQueries

    override fun create(
        fromEntityId: String,
        toEntityId: String,
        typeKey: String,
        reciprocalTypeKey: String?,
        note: String?,
    ): Outcome<Relationship> {
        if (fromEntityId ==
            toEntityId
        ) {
            return failure(FailureReason.InvalidInput, "A person cannot relate to themselves")
        }
        val type =
            RelationshipTypes.byKey(typeKey) ?: return failure(FailureReason.InvalidInput, "Unknown relationship type")
        val reciprocal =
            if (reciprocalTypeKey == null) {
                RelationshipTypes.reciprocalOf(typeKey)
            } else {
                RelationshipTypes.byKey(reciprocalTypeKey)
            } ?: return failure(FailureReason.InvalidInput, "Unknown reciprocal type")
        val entities = database.entityQueries
        if (entities.selectById(fromEntityId, ::mapEntity).executeAsOneOrNull() == null ||
            entities.selectById(toEntityId, ::mapEntity).executeAsOneOrNull() == null
        ) {
            return failure(FailureReason.NotFound, "No such person")
        }
        val existing =
            queries.selectExisting(
                fromEntityId,
                toEntityId,
                RelationshipType.idForKey(type.key),
                ::mapRelationship,
            )
        if (existing.executeAsOneOrNull() != null) return failure(FailureReason.Conflict, "This relationship exists")

        val pairId = ids.next()
        val forwardId = ids.next()
        val backwardId = ids.next()
        database.transaction {
            queries.insert(
                id = forwardId,
                pairId = pairId,
                fromEntityId = fromEntityId,
                toEntityId = toEntityId,
                typeId = RelationshipType.idForKey(type.key),
                note = note,
                now = clock(),
            )
            queries.insert(
                id = backwardId,
                pairId = pairId,
                fromEntityId = toEntityId,
                toEntityId = fromEntityId,
                typeId = RelationshipType.idForKey(reciprocal.key),
                note = note,
                now = clock(),
            )
        }
        return Outcome.Success(checkNotNull(listFor(fromEntityId).firstOrNull { it.id == forwardId }))
    }

    override fun listFor(entityId: String): List<Relationship> =
        queries.selectFrom(entityId, ::mapRelationship).executeAsList()

    override fun delete(pairId: String): Outcome<Unit> {
        if (queries.selectByPair(pairId, ::mapRelationship).executeAsList().isEmpty()) {
            return failure(FailureReason.NotFound, "No such relationship")
        }
        queries.deletePair(pairId = pairId)
        return Outcome.Success(Unit)
    }
}
