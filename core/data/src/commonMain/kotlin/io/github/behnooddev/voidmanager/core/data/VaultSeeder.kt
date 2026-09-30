package io.github.behnooddev.voidmanager.core.data

import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.RelationshipType
import io.github.behnooddev.voidmanager.core.model.RelationshipTypes

/** Writes the built-in field definitions and relationship types. Safe to run on every open. */
object VaultSeeder {
    const val REGISTRY_VERSION_KEY = "registry_version"

    fun seed(database: VoidManagerDatabase) {
        database.transaction {
            FieldRegistry.definitions.forEachIndexed { index, definition ->
                database.fieldDefinitionQueries.upsertBuiltIn(
                    id = definition.id,
                    systemKey = definition.systemKey,
                    section = definition.section.code.toLong(),
                    dataType = definition.dataType.code.toLong(),
                    defaultLabel = definition.defaultLabel,
                    defaultSensitivity = definition.defaultSensitivity.level.toLong(),
                    allowsMultiple = if (definition.allowsMultiple) 1L else 0L,
                    isComposite = if (definition.isComposite) 1L else 0L,
                    schemaVersion = definition.schemaVersion.toLong(),
                    sortOrder = index.toLong(),
                )
                database.fieldDefinitionQueries.deleteParts(definitionId = definition.id)
                definition.parts.forEachIndexed { partIndex, part ->
                    database.fieldDefinitionQueries.insertPart(
                        definitionId = definition.id,
                        partKey = part.key,
                        sensitivity = part.sensitivity.level.toLong(),
                        sortOrder = partIndex.toLong(),
                    )
                }
            }
            for (type in RelationshipTypes.all) {
                database.relationshipQueries.upsertType(
                    id = type.id,
                    label = type.label,
                    reciprocalTypeId = RelationshipType.idForKey(type.reciprocalKey),
                )
            }
            database.miscQueries.putMeta(REGISTRY_VERSION_KEY, FieldRegistry.VERSION.toString())
        }
    }
}
