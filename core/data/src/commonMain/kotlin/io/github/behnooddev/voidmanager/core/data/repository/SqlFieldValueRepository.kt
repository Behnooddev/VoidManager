package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.data.CipherContext
import io.github.behnooddev.voidmanager.core.data.ValueCipher
import io.github.behnooddev.voidmanager.core.data.logic.FieldNormalizer
import io.github.behnooddev.voidmanager.core.data.logic.SensitivityPolicy
import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldPart
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.IdGenerator
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy

internal class SqlFieldValueRepository(
    private val database: VoidManagerDatabase,
    private val cipher: ValueCipher,
    private val ids: IdGenerator,
    private val clock: () -> Long,
) : FieldValueRepository {
    private val values get() = database.fieldValueQueries
    private val definitions get() = database.fieldDefinitionQueries

    private class Definition(
        val dataType: FieldDataType,
        val defaultSensitivity: Sensitivity,
        val allowsMultiple: Boolean,
        val isComposite: Boolean,
        val partSensitivity: Map<String, Sensitivity>,
    )

    private class Stored(
        val plain: String?,
        val cipher: ByteArray?,
        val normalized: String?,
    )

    override fun add(
        entityId: String,
        input: FieldValueInput,
    ): Outcome<FieldValue> {
        if (database.entityQueries.selectById(entityId, ::mapEntity).executeAsOneOrNull() == null) {
            return failure(FailureReason.NotFound, "No such entity")
        }
        val definition =
            loadDefinition(input.definitionId)
                ?: return failure(FailureReason.NotFound, "No such field definition")
        val invalid = validate(definition, input)
        if (invalid != null) return invalid
        val effective =
            SensitivityPolicy.effective(definition.defaultSensitivity, input.sensitivity)
                ?: return failure(
                    FailureReason.SensitivityLowered,
                    "Sensitivity cannot be lowered below the field default",
                )
        if (!definition.allowsMultiple) {
            val existing = values.countForDefinitionOnEntity(entityId, input.definitionId).executeAsOne()
            if (existing > 0) return failure(FailureReason.Conflict, "This field allows one value")
        }

        val id = ids.next()
        database.transaction {
            writeNew(id, entityId, definition, input, effective)
        }
        return Outcome.Success(checkNotNull(get(id)))
    }

    override fun replace(
        id: String,
        input: FieldValueInput,
    ): Outcome<FieldValue> {
        val current = get(id) ?: return failure(FailureReason.NotFound, "No such value")
        if (current.definitionId != input.definitionId) {
            return failure(FailureReason.InvalidInput, "The field type of a value cannot change")
        }
        val definition =
            loadDefinition(input.definitionId)
                ?: return failure(FailureReason.NotFound, "No such field definition")
        val invalid = validate(definition, input)
        if (invalid != null) return invalid
        val effective =
            SensitivityPolicy.effective(definition.defaultSensitivity, input.sensitivity)
                ?: return failure(
                    FailureReason.SensitivityLowered,
                    "Sensitivity cannot be lowered below the field default",
                )

        database.transaction {
            val now = clock()
            val value = store(definition.dataType, "field_value", id, "value", input.value, effective)
            val note =
                store(FieldDataType.Multiline, "field_value", id, "note", input.note, effective, normalize = false)
            values.replaceValue(
                label = input.label,
                valuePlain = value.plain,
                valueCipher = value.cipher,
                notePlain = note.plain,
                noteCipher = note.cipher,
                normalized = value.normalized,
                isPrimary = input.isPrimary.toLong(),
                sortOrder = input.sortOrder.toLong(),
                sensitivity = effective.level.toLong(),
                sharingPolicy = input.sharingPolicy.code.toLong(),
                metadata = input.metadata,
                now = now,
                id = id,
            )
            values.deleteParts(fieldValueId = id)
            writeParts(id, definition, input, effective)
            if (input.isPrimary) {
                values.clearPrimary(entityId = current.entityId, definitionId = input.definitionId, exceptId = id)
            }
            database.entityQueries.touch(now = now, id = current.entityId)
        }
        return Outcome.Success(checkNotNull(get(id)))
    }

    override fun delete(id: String): Outcome<Unit> {
        val current = get(id) ?: return failure(FailureReason.NotFound, "No such value")
        database.transaction {
            values.deleteValue(id = id)
            database.entityQueries.touch(now = clock(), id = current.entityId)
        }
        return Outcome.Success(Unit)
    }

    override fun get(id: String): FieldValue? {
        val parts = values.selectPartsForValue(id, ::mapPart).executeAsList()
        return values
            .selectValueById(id) {
                rowId,
                entityId,
                definitionId,
                label,
                valuePlain,
                valueCipher,
                notePlain,
                noteCipher,
                _,
                isPrimary,
                sortOrder,
                sensitivity,
                sharingPolicy,
                metadata,
                createdAt,
                updatedAt,
                ->
                buildValue(
                    rowId,
                    entityId,
                    definitionId,
                    label,
                    valuePlain,
                    valueCipher,
                    notePlain,
                    noteCipher,
                    isPrimary,
                    sortOrder,
                    sensitivity,
                    sharingPolicy,
                    metadata,
                    createdAt,
                    updatedAt,
                    parts.filter { it.first == rowId }.map { it.second },
                )
            }.executeAsOneOrNull()
    }

    override fun listForEntity(entityId: String): List<FieldValue> {
        val parts = values.selectPartsForEntity(entityId, ::mapPart).executeAsList()
        return values
            .selectValuesForEntity(entityId) {
                rowId,
                owner,
                definitionId,
                label,
                valuePlain,
                valueCipher,
                notePlain,
                noteCipher,
                _,
                isPrimary,
                sortOrder,
                sensitivity,
                sharingPolicy,
                metadata,
                createdAt,
                updatedAt,
                ->
                buildValue(
                    rowId,
                    owner,
                    definitionId,
                    label,
                    valuePlain,
                    valueCipher,
                    notePlain,
                    noteCipher,
                    isPrimary,
                    sortOrder,
                    sensitivity,
                    sharingPolicy,
                    metadata,
                    createdAt,
                    updatedAt,
                    parts.filter { it.first == rowId }.map { it.second },
                )
            }.executeAsList()
    }

    private fun mapPart(
        fieldValueId: String,
        partKey: String,
        valuePlain: String?,
        valueCipher: ByteArray?,
        @Suppress("UNUSED_PARAMETER") normalized: String?,
        sensitivity: Long,
    ): Pair<String, FieldPart> {
        val level = Sensitivity.fromLevel(sensitivity.toInt())
        return fieldValueId to
            FieldPart(
                key = partKey,
                value = read("field_part", fieldValueId, partKey, valuePlain, valueCipher, level),
                sensitivity = level,
            )
    }

    private fun buildValue(
        id: String,
        entityId: String,
        definitionId: String,
        label: String?,
        valuePlain: String?,
        valueCipher: ByteArray?,
        notePlain: String?,
        noteCipher: ByteArray?,
        isPrimary: Long,
        sortOrder: Long,
        sensitivity: Long,
        sharingPolicy: Long,
        metadata: String?,
        createdAt: Long,
        updatedAt: Long,
        parts: List<FieldPart>,
    ): FieldValue {
        val level = Sensitivity.fromLevel(sensitivity.toInt())
        return FieldValue(
            id = id,
            entityId = entityId,
            definitionId = definitionId,
            label = label,
            value = read("field_value", id, "value", valuePlain, valueCipher, level),
            note = read("field_value", id, "note", notePlain, noteCipher, level),
            isPrimary = isPrimary == 1L,
            sortOrder = sortOrder.toInt(),
            sensitivity = level,
            sharingPolicy = SharingPolicy.fromCode(sharingPolicy.toInt()),
            metadata = metadata,
            parts = parts,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun read(
        table: String,
        rowId: String,
        slot: String,
        plain: String?,
        cipherBytes: ByteArray?,
        sensitivity: Sensitivity,
    ): FieldText? =
        when {
            cipherBytes != null ->
                FieldText(
                    cipher.decrypt(CipherContext(table, rowId, slot), cipherBytes),
                    isProtected = true,
                )
            plain != null -> FieldText(plain, isProtected = sensitivity.isStoredEncrypted)
            else -> null
        }

    private fun loadDefinition(definitionId: String): Definition? {
        val base =
            definitions
                .selectById(definitionId) {
                    _,
                    _,
                    _,
                    dataType,
                    _,
                    defaultSensitivity,
                    allowsMultiple,
                    isComposite,
                    _,
                    _,
                    _,
                    _,
                    _,
                    ->
                    Definition(
                        dataType = FieldDataType.fromCode(dataType.toInt()),
                        defaultSensitivity = Sensitivity.fromLevel(defaultSensitivity.toInt()),
                        allowsMultiple = allowsMultiple == 1L,
                        isComposite = isComposite == 1L,
                        partSensitivity = emptyMap(),
                    )
                }.executeAsOneOrNull() ?: return null
        val parts =
            definitions
                .selectPartsFor(definitionId) { _, partKey, sensitivity, _ ->
                    partKey to Sensitivity.fromLevel(sensitivity.toInt())
                }.executeAsList()
                .toMap()
        return Definition(base.dataType, base.defaultSensitivity, base.allowsMultiple, base.isComposite, parts)
    }

    private fun validate(
        definition: Definition,
        input: FieldValueInput,
    ): Outcome.Failure? {
        if (!definition.isComposite) {
            if (input.parts.isNotEmpty()) return failure(FailureReason.InvalidInput, "This field has no parts")
        } else {
            if (input.value != null) return failure(FailureReason.InvalidInput, "This field is stored as parts")
            val unknown = input.parts.keys - definition.partSensitivity.keys
            if (unknown.isNotEmpty()) return failure(FailureReason.InvalidInput, "Unknown part: ${unknown.first()}")
        }
        return null
    }

    private fun writeNew(
        id: String,
        entityId: String,
        definition: Definition,
        input: FieldValueInput,
        effective: Sensitivity,
    ) {
        val now = clock()
        val value = store(definition.dataType, "field_value", id, "value", input.value, effective)
        val note = store(FieldDataType.Multiline, "field_value", id, "note", input.note, effective, normalize = false)
        values.insertValue(
            id = id,
            entityId = entityId,
            definitionId = input.definitionId,
            label = input.label,
            valuePlain = value.plain,
            valueCipher = value.cipher,
            notePlain = note.plain,
            noteCipher = note.cipher,
            normalized = value.normalized,
            isPrimary = input.isPrimary.toLong(),
            sortOrder = input.sortOrder.toLong(),
            sensitivity = effective.level.toLong(),
            sharingPolicy = input.sharingPolicy.code.toLong(),
            metadata = input.metadata,
            now = now,
        )
        writeParts(id, definition, input, effective)
        if (input.isPrimary) {
            values.clearPrimary(entityId = entityId, definitionId = input.definitionId, exceptId = id)
        }
        database.entityQueries.touch(now = now, id = entityId)
    }

    private fun writeParts(
        id: String,
        definition: Definition,
        input: FieldValueInput,
        effective: Sensitivity,
    ) {
        for ((key, text) in input.parts) {
            val partDefault = definition.partSensitivity.getValue(key)
            val level = if (partDefault.level >= effective.level) partDefault else effective
            val stored = store(FieldDataType.Text, "field_part", id, key, text, level)
            values.insertPart(
                fieldValueId = id,
                partKey = key,
                valuePlain = stored.plain,
                valueCipher = stored.cipher,
                normalized = stored.normalized,
                sensitivity = level.level.toLong(),
            )
        }
    }

    private fun store(
        type: FieldDataType,
        table: String,
        rowId: String,
        slot: String,
        text: String?,
        sensitivity: Sensitivity,
        normalize: Boolean = true,
    ): Stored {
        if (text == null) return Stored(null, null, null)
        if (sensitivity.isStoredEncrypted) {
            return Stored(null, cipher.encrypt(CipherContext(table, rowId, slot), text), null)
        }
        val normalized = if (normalize) FieldNormalizer.normalizedFor(type, text) else null
        return Stored(text, null, normalized)
    }

    private companion object {
        const val LOWERED_MESSAGE = "Sensitivity cannot be lowered below the field default"
    }
}
