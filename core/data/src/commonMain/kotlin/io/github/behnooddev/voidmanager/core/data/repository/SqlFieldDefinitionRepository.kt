package io.github.behnooddev.voidmanager.core.data.repository

import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.FieldPartDefinition
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.IdGenerator
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity

/** Reads the field registry of one vault and changes the parts of it that the user may change. */
internal class SqlFieldDefinitionRepository(
    private val database: VoidManagerDatabase,
    private val ids: IdGenerator,
) : FieldDefinitionRepository {
    private val queries get() = database.fieldDefinitionQueries

    override fun list(): List<FieldDefinitionInfo> =
        queries
            .selectAll { id, _, _, _, _, _, _, _, _, _, _, _, _ -> id }
            .executeAsList()
            .mapNotNull { load(it) }

    override fun createCustom(
        label: String,
        dataType: FieldDataType,
        sensitivity: Sensitivity,
        allowsMultiple: Boolean,
    ): Outcome<FieldDefinitionInfo> {
        val clean = label.trim()
        if (clean.isEmpty() || clean.length > MAX_LABEL_LENGTH) {
            return failure(FailureReason.InvalidInput, "A field label has 1 to $MAX_LABEL_LENGTH characters")
        }
        if (dataType !in
            CUSTOM_TYPES
        ) {
            return failure(FailureReason.InvalidInput, "This type cannot be used for a custom field")
        }
        if (labelTaken(
                clean,
                exceptId = null,
            )
        ) {
            return failure(FailureReason.Conflict, "Another field already has this label")
        }

        val id = "$CUSTOM_ID_PREFIX${ids.next()}"
        database.transaction {
            val next = (list().maxOfOrNull { it.sortOrder } ?: 0) + 1
            queries.insertCustom(
                id = id,
                section = FieldSection.Custom.code.toLong(),
                dataType = dataType.code.toLong(),
                defaultLabel = clean,
                defaultSensitivity = sensitivity.level.toLong(),
                allowsMultiple = allowsMultiple.toLong(),
                sortOrder = next.toLong(),
            )
        }
        return found(id)
    }

    override fun setHidden(
        id: String,
        hidden: Boolean,
    ): Outcome<FieldDefinitionInfo> {
        if (load(id) == null) return failure(FailureReason.NotFound, "No such field")
        queries.setHidden(isHidden = hidden.toLong(), id = id)
        return found(id)
    }

    override fun setLabel(
        id: String,
        label: String?,
    ): Outcome<FieldDefinitionInfo> {
        val current = load(id) ?: return failure(FailureReason.NotFound, "No such field")
        val clean = label?.trim().orEmpty()
        if (clean.length >
            MAX_LABEL_LENGTH
        ) {
            return failure(FailureReason.InvalidInput, "A field label has at most $MAX_LABEL_LENGTH characters")
        }
        if (clean.isEmpty() &&
            current.isUserDefined
        ) {
            return failure(FailureReason.InvalidInput, "A custom field needs a label")
        }
        if (clean.isNotEmpty() &&
            labelTaken(clean, exceptId = id)
        ) {
            return failure(FailureReason.Conflict, "Another field already has this label")
        }

        database.transaction {
            when {
                current.isUserDefined -> queries.setCustomLabel(defaultLabel = clean, id = id)
                clean.isEmpty() || clean == current.defaultLabel ->
                    queries.setLabelOverride(
                        labelOverride = null,
                        id = id,
                    )
                else -> queries.setLabelOverride(labelOverride = clean, id = id)
            }
        }
        return found(id)
    }

    override fun deleteCustom(id: String): Outcome<Unit> {
        val current = load(id) ?: return failure(FailureReason.NotFound, "No such field")
        if (!current.isUserDefined) return failure(FailureReason.NotAllowed, "A built-in field cannot be deleted")
        if (queries.countValuesForDefinition(id).executeAsOne() > 0L) {
            return failure(FailureReason.Conflict, "Move or delete the values of this field first")
        }
        queries.deleteCustom(id)
        return Outcome.Success(Unit)
    }

    private fun found(id: String): Outcome<FieldDefinitionInfo> =
        load(id)?.let { Outcome.Success(it) } ?: failure(FailureReason.NotFound, "No such field")

    /** True when another visible label matches [label], ignoring case. */
    private fun labelTaken(
        label: String,
        exceptId: String?,
    ): Boolean = list().any { it.id != exceptId && it.label.equals(label, ignoreCase = true) }

    private fun load(id: String): FieldDefinitionInfo? {
        val parts =
            queries
                .selectPartsFor(
                    id,
                ) { _, partKey, sensitivity, _ ->
                    FieldPartDefinition(
                        partKey,
                        Sensitivity.fromLevel(sensitivity.toInt()),
                    )
                }.executeAsList()
        return queries
            .selectById(id) {
                definitionId,
                systemKey,
                section,
                dataType,
                defaultLabel,
                defaultSensitivity,
                allowsMultiple,
                _,
                isUserDefined,
                _,
                sortOrder,
                isHidden,
                labelOverride,
                ->
                FieldDefinitionInfo(
                    id = definitionId,
                    systemKey = systemKey,
                    section = FieldSection.fromCode(section.toInt()),
                    dataType = FieldDataType.fromCode(dataType.toInt()),
                    defaultLabel = defaultLabel,
                    labelOverride = labelOverride,
                    defaultSensitivity = Sensitivity.fromLevel(defaultSensitivity.toInt()),
                    allowsMultiple = allowsMultiple == 1L,
                    parts = parts,
                    isUserDefined = isUserDefined == 1L,
                    isHidden = isHidden == 1L,
                    sortOrder = sortOrder.toInt(),
                )
            }.executeAsOneOrNull()
    }

    private companion object {
        const val MAX_LABEL_LENGTH = 60
        const val CUSTOM_ID_PREFIX = "usr."

        /** The scalar types a custom field may use. Composite types need a part layout and are not offered. */
        val CUSTOM_TYPES =
            setOf(
                FieldDataType.Text,
                FieldDataType.Multiline,
                FieldDataType.Number,
                FieldDataType.Date,
                FieldDataType.Url,
                FieldDataType.Phone,
                FieldDataType.Email,
                FieldDataType.Username,
            )
    }
}
