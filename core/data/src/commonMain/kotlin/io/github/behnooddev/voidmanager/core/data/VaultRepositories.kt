package io.github.behnooddev.voidmanager.core.data

import io.github.behnooddev.voidmanager.core.data.repository.EntityRepository
import io.github.behnooddev.voidmanager.core.data.repository.FieldValueRepository
import io.github.behnooddev.voidmanager.core.data.repository.RelationshipRepository
import io.github.behnooddev.voidmanager.core.data.repository.SearchRepository
import io.github.behnooddev.voidmanager.core.data.repository.SqlEntityRepository
import io.github.behnooddev.voidmanager.core.data.repository.SqlFieldValueRepository
import io.github.behnooddev.voidmanager.core.data.repository.SqlRelationshipRepository
import io.github.behnooddev.voidmanager.core.data.repository.SqlSearchRepository
import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.IdGenerator

/** Builds the repositories for an opened vault and seeds the built-in registry. */
class VaultRepositories(
    database: VoidManagerDatabase,
    cipher: ValueCipher,
    ids: IdGenerator,
    clock: () -> Long,
) {
    val entities: EntityRepository = SqlEntityRepository(database, ids, clock)
    val fields: FieldValueRepository = SqlFieldValueRepository(database, cipher, ids, clock)
    val relationships: RelationshipRepository = SqlRelationshipRepository(database, ids, clock)
    val search: SearchRepository = SqlSearchRepository(database)

    init {
        VaultSeeder.seed(database)
    }
}
