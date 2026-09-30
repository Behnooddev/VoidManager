package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Relationship

/**
 * Repository calls are blocking. Callers run them off the main thread; suspend wrappers are added
 * with the first feature that needs them.
 */
interface EntityRepository {
    fun createPerson(displayName: String): Outcome<Entity>

    /** Returns the Personal profile, creating it on first use. */
    fun ensureSelf(displayName: String): Entity

    fun get(id: String): Entity?

    fun listActive(): List<Entity>

    fun listFavorites(): List<Entity>

    fun listTrashed(): List<Entity>

    fun rename(
        id: String,
        displayName: String,
    ): Outcome<Entity>

    fun setFavorite(
        id: String,
        favorite: Boolean,
    ): Outcome<Entity>

    /** Moves a person to Trash. The Personal profile cannot be trashed. */
    fun moveToTrash(id: String): Outcome<Unit>

    fun restore(id: String): Outcome<Unit>

    /** Permanently deletes a trashed entity with everything that belongs to it. */
    fun purge(id: String): Outcome<Unit>
}

interface FieldValueRepository {
    fun add(
        entityId: String,
        input: FieldValueInput,
    ): Outcome<FieldValue>

    fun replace(
        id: String,
        input: FieldValueInput,
    ): Outcome<FieldValue>

    fun delete(id: String): Outcome<Unit>

    fun get(id: String): FieldValue?

    fun listForEntity(entityId: String): List<FieldValue>
}

interface RelationshipRepository {
    /**
     * Creates the relationship and its reciprocal in one transaction.
     * [reciprocalTypeKey] overrides the default reciprocal of [typeKey].
     */
    fun create(
        fromEntityId: String,
        toEntityId: String,
        typeKey: String,
        reciprocalTypeKey: String? = null,
        note: String? = null,
    ): Outcome<Relationship>

    fun listFor(entityId: String): List<Relationship>

    /** Deletes both directions of the relationship that [pairId] identifies. */
    fun delete(pairId: String): Outcome<Unit>
}

interface SearchRepository {
    /** Searches level 0 and 1 data of people that are not in Trash. Level 2 and 3 values are never searched. */
    fun search(
        query: String,
        limit: Int = DEFAULT_LIMIT,
    ): List<Entity>

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
