package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.data.logic.TextNormalizer
import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.IdGenerator
import io.github.behnooddev.voidmanager.core.model.Outcome

internal class SqlEntityRepository(
    private val database: VoidManagerDatabase,
    private val ids: IdGenerator,
    private val clock: () -> Long,
) : EntityRepository {
    private val queries get() = database.entityQueries

    override fun createPerson(displayName: String): Outcome<Entity> {
        val name = displayName.trim()
        if (name.isEmpty()) return failure(FailureReason.InvalidInput, "The name is empty")
        val id = ids.next()
        queries.insert(
            id = id,
            kind = EntityKind.Person.code.toLong(),
            displayName = name,
            sortKey = TextNormalizer.normalize(name),
            now = clock(),
        )
        return Outcome.Success(checkNotNull(get(id)))
    }

    override fun ensureSelf(displayName: String): Entity {
        queries.selectSelf(::mapEntity).executeAsOneOrNull()?.let { return it }
        val name = displayName.trim().ifEmpty { DEFAULT_SELF_NAME }
        val id = ids.next()
        queries.insert(
            id = id,
            kind = EntityKind.Self.code.toLong(),
            displayName = name,
            sortKey = TextNormalizer.normalize(name),
            now = clock(),
        )
        return checkNotNull(get(id))
    }

    override fun get(id: String): Entity? = queries.selectById(id, ::mapEntity).executeAsOneOrNull()

    override fun listActive(): List<Entity> = queries.selectActive(::mapEntity).executeAsList()

    override fun listFavorites(): List<Entity> = queries.selectFavorites(::mapEntity).executeAsList()

    override fun listTrashed(): List<Entity> = queries.selectTrashed(::mapEntity).executeAsList()

    override fun rename(
        id: String,
        displayName: String,
    ): Outcome<Entity> {
        val name = displayName.trim()
        if (name.isEmpty()) return failure(FailureReason.InvalidInput, "The name is empty")
        if (get(id) == null) return failure(FailureReason.NotFound, "No such entity")
        queries.rename(displayName = name, sortKey = TextNormalizer.normalize(name), now = clock(), id = id)
        return Outcome.Success(checkNotNull(get(id)))
    }

    override fun setFavorite(
        id: String,
        favorite: Boolean,
    ): Outcome<Entity> {
        if (get(id) == null) return failure(FailureReason.NotFound, "No such entity")
        queries.setFavorite(favorite = favorite.toLong(), now = clock(), id = id)
        return Outcome.Success(checkNotNull(get(id)))
    }

    override fun moveToTrash(id: String): Outcome<Unit> {
        val entity = get(id) ?: return failure(FailureReason.NotFound, "No such entity")
        if (entity.kind ==
            EntityKind.Self
        ) {
            return failure(FailureReason.NotAllowed, "The Personal profile cannot be trashed")
        }
        if (entity.isTrashed) return failure(FailureReason.Conflict, "Already in Trash")
        queries.moveToTrash(now = clock(), id = id)
        return Outcome.Success(Unit)
    }

    override fun restore(id: String): Outcome<Unit> {
        val entity = get(id) ?: return failure(FailureReason.NotFound, "No such entity")
        if (!entity.isTrashed) return failure(FailureReason.Conflict, "Not in Trash")
        queries.restore(now = clock(), id = id)
        return Outcome.Success(Unit)
    }

    override fun purge(id: String): Outcome<Unit> {
        val entity = get(id) ?: return failure(FailureReason.NotFound, "No such entity")
        if (!entity.isTrashed) {
            return failure(
                FailureReason.NotAllowed,
                "Only entities in Trash can be deleted permanently",
            )
        }
        queries.purge(id = id)
        return Outcome.Success(Unit)
    }

    private companion object {
        const val DEFAULT_SELF_NAME = "Me"
    }
}
