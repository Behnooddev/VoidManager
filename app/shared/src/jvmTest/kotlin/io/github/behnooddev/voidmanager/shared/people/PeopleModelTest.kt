package io.github.behnooddev.voidmanager.shared.people

import io.github.behnooddev.voidmanager.core.data.repository.EntityRepository
import io.github.behnooddev.voidmanager.core.data.repository.FieldDefinitionRepository
import io.github.behnooddev.voidmanager.core.data.repository.FieldValueRepository
import io.github.behnooddev.voidmanager.core.data.repository.SearchRepository
import io.github.behnooddev.voidmanager.core.model.Entity
import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldDataType
import io.github.behnooddev.voidmanager.core.model.FieldDefinitionInfo
import io.github.behnooddev.voidmanager.core.model.FieldPart
import io.github.behnooddev.voidmanager.core.model.FieldRegistry
import io.github.behnooddev.voidmanager.core.model.FieldSection
import io.github.behnooddev.voidmanager.core.model.FieldText
import io.github.behnooddev.voidmanager.core.model.FieldValue
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.model.SharingPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun <T> fail(reason: FailureReason): Outcome<T> = Outcome.Failure(reason, "synthetic")

private class MemoryEntities : EntityRepository {
    val rows = linkedMapOf<String, Entity>()
    var counter = 0
    var throwOnRead = false

    private fun guard() {
        if (throwOnRead) error("the vault was locked")
    }

    override fun createPerson(displayName: String): Outcome<Entity> {
        if (displayName.isBlank()) return fail(FailureReason.InvalidInput)
        val entity = Entity("p${++counter}", EntityKind.Person, displayName.trim(), false, null, 1L, 1L, null)
        rows[entity.id] = entity
        return Outcome.Success(entity)
    }

    override fun ensureSelf(displayName: String): Entity =
        rows.values.firstOrNull { it.kind == EntityKind.Self }
            ?: Entity("self", EntityKind.Self, displayName.ifBlank { "Me" }, false, null, 1L, 1L, null).also {
                rows[it.id] =
                    it
            }

    override fun get(id: String): Entity? {
        guard()
        return rows[id]
    }

    override fun listActive(): List<Entity> {
        guard()
        return rows.values.filter { !it.isTrashed && it.kind == EntityKind.Person }.sortedBy { it.displayName }
    }

    override fun listFavorites() = listActive().filter { it.isFavorite }

    override fun listTrashed(): List<Entity> {
        guard()
        return rows.values.filter { it.isTrashed }
    }

    override fun rename(
        id: String,
        displayName: String,
    ): Outcome<Entity> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        if (displayName.isBlank()) return fail(FailureReason.InvalidInput)
        return Outcome.Success(current.copy(displayName = displayName.trim()).also { rows[id] = it })
    }

    override fun setFavorite(
        id: String,
        favorite: Boolean,
    ): Outcome<Entity> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        return Outcome.Success(current.copy(isFavorite = favorite).also { rows[id] = it })
    }

    override fun moveToTrash(id: String): Outcome<Unit> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        if (current.kind == EntityKind.Self) return fail(FailureReason.NotAllowed)
        rows[id] = current.copy(deletedAt = 5L)
        return Outcome.Success(Unit)
    }

    override fun restore(id: String): Outcome<Unit> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        rows[id] = current.copy(deletedAt = null)
        return Outcome.Success(Unit)
    }

    override fun purge(id: String): Outcome<Unit> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        if (!current.isTrashed) return fail(FailureReason.NotAllowed)
        rows.remove(id)
        return Outcome.Success(Unit)
    }
}

private class MemoryFields : FieldValueRepository {
    val rows = linkedMapOf<String, FieldValue>()
    var counter = 0
    var rejectDefinition: String? = null

    private fun build(
        id: String,
        entityId: String,
        input: FieldValueInput,
    ) = FieldValue(
        id = id,
        entityId = entityId,
        definitionId = input.definitionId,
        label = input.label,
        value = input.value?.let { FieldText(it, false) },
        note = null,
        isPrimary = input.isPrimary,
        sortOrder = input.sortOrder,
        sensitivity = input.sensitivity ?: Sensitivity.Personal,
        sharingPolicy = SharingPolicy.Inherit,
        metadata = null,
        parts =
            input.parts.map { (key, text) ->
                FieldPart(key, FieldText(text, key == "password"), Sensitivity.Personal)
            },
        createdAt = counter.toLong(),
        updatedAt = counter.toLong(),
    )

    override fun add(
        entityId: String,
        input: FieldValueInput,
    ): Outcome<FieldValue> {
        if (input.definitionId == rejectDefinition) return fail(FailureReason.Conflict)
        val value = build("f${++counter}", entityId, input)
        rows[value.id] = value
        return Outcome.Success(value)
    }

    override fun replace(
        id: String,
        input: FieldValueInput,
    ): Outcome<FieldValue> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        return Outcome.Success(build(id, current.entityId, input).also { rows[id] = it })
    }

    override fun delete(id: String): Outcome<Unit> =
        if (rows.remove(id) !=
            null
        ) {
            Outcome.Success(Unit)
        } else {
            fail(FailureReason.NotFound)
        }

    override fun get(id: String) = rows[id]

    override fun listForEntity(entityId: String) = rows.values.filter { it.entityId == entityId }
}

private class MemoryDefinitions(
    private val fieldValues: MemoryFields,
) : FieldDefinitionRepository {
    val rows =
        linkedMapOf<String, FieldDefinitionInfo>().also { map ->
            FieldRegistry.infos.forEach { map[it.id] = it }
        }
    var counter = 0

    override fun list(): List<FieldDefinitionInfo> = rows.values.toList()

    override fun createCustom(
        label: String,
        dataType: FieldDataType,
        sensitivity: Sensitivity,
        allowsMultiple: Boolean,
    ): Outcome<FieldDefinitionInfo> {
        val clean = label.trim()
        if (clean.isEmpty()) return fail(FailureReason.InvalidInput)
        if (rows.values.any { it.label.equals(clean, ignoreCase = true) }) return fail(FailureReason.Conflict)
        val info =
            FieldDefinitionInfo(
                "usr.${++counter}",
                null,
                FieldSection.Custom,
                dataType,
                clean,
                null,
                sensitivity,
                allowsMultiple,
                emptyList(),
                true,
                false,
                100 + counter,
            )
        rows[info.id] = info
        return Outcome.Success(info)
    }

    override fun setHidden(
        id: String,
        hidden: Boolean,
    ): Outcome<FieldDefinitionInfo> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        return Outcome.Success(current.copy(isHidden = hidden).also { rows[id] = it })
    }

    override fun setLabel(
        id: String,
        label: String?,
    ): Outcome<FieldDefinitionInfo> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        val clean = label?.trim().orEmpty()
        if (clean.isNotEmpty() &&
            rows.values.any { it.id != id && it.label.equals(clean, ignoreCase = true) }
        ) {
            return fail(FailureReason.Conflict)
        }
        return Outcome.Success(current.copy(labelOverride = clean.ifEmpty { null }).also { rows[id] = it })
    }

    override fun deleteCustom(id: String): Outcome<Unit> {
        val current = rows[id] ?: return fail(FailureReason.NotFound)
        if (!current.isUserDefined) return fail(FailureReason.NotAllowed)
        if (fieldValues.rows.values.any { it.definitionId == id }) return fail(FailureReason.Conflict)
        rows.remove(id)
        return Outcome.Success(Unit)
    }
}

private class MemorySearch(
    private val entities: MemoryEntities,
) : SearchRepository {
    override fun search(
        query: String,
        limit: Int,
    ) = entities.listActive().filter { it.displayName.contains(query, ignoreCase = true) }.take(limit)
}

class PeopleModelTest {
    private val entities = MemoryEntities()
    private val fields = MemoryFields()
    private val definitions = MemoryDefinitions(fields)
    private val model = PeopleModel(entities, fields, definitions, MemorySearch(entities), Dispatchers.Unconfined)

    private fun add(name: String): String = (assertIs<Outcome.Success<Entity>>(entities.createPerson(name))).value.id

    @Test
    fun theListShowsActivePeopleAndSearchNarrowsIt() =
        runBlocking {
            add("Zed Example")
            add("Ada Example")
            model.loadPeople()
            assertEquals(listOf("Ada Example", "Zed Example"), model.people.map { it.displayName })

            model.setQuery("zed")
            assertEquals(listOf("Zed Example"), model.people.map { it.displayName })
        }

    @Test
    fun quickAddSavesThePersonWithTheirValues() =
        runBlocking {
            val result = model.quickAdd(QuickAddInput(name = " Synthetic Person ", phone = "5550100", email = "a@b.co"))
            val saved = assertIs<QuickAddResult.Saved>(result)
            assertEquals(listOf("Synthetic Person"), model.people.map { it.displayName })
            assertEquals(listOf("sys.phone", "sys.email"), fields.listForEntity(saved.entityId).map { it.definitionId })
            assertNull(model.problem)
        }

    @Test
    fun quickAddRefusesInvalidInputWithoutTouchingTheVault() =
        runBlocking {
            assertEquals(QuickAddResult.Invalid, model.quickAdd(QuickAddInput(name = "")))
            assertEquals(QuickAddResult.Invalid, model.quickAdd(QuickAddInput(name = "A", email = "nope")))
            assertTrue(entities.rows.isEmpty())
        }

    @Test
    fun aValueThatFailsLeavesAPartiallySavedPersonAndSaysSo() =
        runBlocking {
            fields.rejectDefinition = "sys.email"
            val result = model.quickAdd(QuickAddInput(name = "A", phone = "5550100", email = "a@b.co"))
            assertIs<QuickAddResult.Partial>(result)
            assertEquals(Problem.PartiallySaved, model.problem)
            assertEquals(1, entities.rows.size)
        }

    @Test
    fun theProfileLoadsAndFieldsCanBeAddedEditedAndDeleted() =
        runBlocking {
            val id = add("A")
            assertTrue(model.saveField(id, null, FieldValueInput("sys.phone", value = "5550100")))
            val valueId =
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
                    .valueId

            assertTrue(model.saveField(id, valueId, FieldValueInput("sys.phone", value = "5550199")))
            assertEquals(
                "5550199",
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
                    .text
                    ?.reveal(),
            )

            assertTrue(model.deleteField(id, valueId))
            assertTrue(model.profile!!.isEmpty)
        }

    @Test
    fun aCompositeValueIsStoredByPartsAndShownAsParts() =
        runBlocking {
            val id = add("A")
            val input =
                FieldValueInput(
                    "sys.account",
                    parts = mapOf("service" to "Mail", "password" to "hunter2"),
                    isPrimary = true,
                )
            assertTrue(model.saveField(id, null, input))
            val row =
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
            assertNull(row.text)
            assertEquals(listOf("service", "password"), row.parts.map { it.key })
            assertTrue(row.isProtected)
        }

    @Test
    fun makingAValuePrimaryKeepsItsContentAndMovesThePrimaryMark() =
        runBlocking {
            val id = add("A")
            model.saveField(id, null, FieldValueInput("sys.phone", value = "5550100", isPrimary = true))
            model.saveField(id, null, FieldValueInput("sys.phone", value = "5550101"))
            val second =
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .first { !it.isPrimary }

            assertTrue(model.makePrimary(id, second))
            val stored = fields.get(second.valueId)!!
            assertTrue(stored.isPrimary)
            assertEquals("5550101", stored.value?.reveal())
        }

    @Test
    fun aTrashedPersonHasNoProfile() =
        runBlocking {
            val id = add("A")
            assertTrue(model.moveToTrash(id))
            model.loadProfile(id)
            assertNull(model.profile)
            assertEquals(Problem.NotFound, model.problem)
        }

    @Test
    fun renameAndFavoriteUpdateTheProfileAndTheList() =
        runBlocking {
            val id = add("A")
            model.loadProfile(id)
            assertTrue(model.rename(id, "Renamed"))
            model.setFavorite(id, true)
            assertEquals("Renamed", model.profile!!.entity.displayName)
            assertTrue(model.profile!!.entity.isFavorite)
            assertEquals(listOf("Renamed"), model.people.map { it.displayName })
        }

    @Test
    fun anEmptyNameCannotBeSaved() =
        runBlocking {
            val id = add("A")
            assertFalse(model.rename(id, "  "))
            assertEquals(Problem.InvalidInput, model.problem)
        }

    @Test
    fun trashRestoreAndPurgeFollowTheRules() =
        runBlocking {
            val id = add("A")
            assertFalse(model.purge(id), "an active person cannot be purged")
            assertEquals(Problem.NotAllowed, model.problem)

            assertTrue(model.moveToTrash(id))
            model.loadTrash()
            assertEquals(listOf(id), model.trashed.map { it.id })
            assertTrue(model.people.isEmpty())

            assertTrue(model.restore(id))
            assertTrue(model.trashed.isEmpty())
            assertEquals(listOf(id), model.people.map { it.id })

            model.moveToTrash(id)
            assertTrue(model.purge(id))
            assertTrue(entities.rows.isEmpty())
        }

    @Test
    fun thePersonalProfileExistsOnceAndCannotBeTrashed() =
        runBlocking {
            model.loadSelf()
            val self = model.selfId
            model.loadSelf()
            assertEquals(self, model.selfId)
            assertFalse(model.moveToTrash(self!!))
            assertEquals(Problem.NotAllowed, model.problem)
        }

    @Test
    fun aLockedVaultEndsInAStorageProblemInsteadOfACrash() =
        runBlocking {
            entities.throwOnRead = true
            model.loadPeople()
            assertEquals(Problem.Storage, model.problem)
            model.clearProblem()
            assertNull(model.problem)
        }

    @Test
    fun aCustomFieldCanBeCreatedUsedAndDeletedOnceEmpty() =
        runBlocking {
            assertTrue(
                model.createCustomField("Locker code", FieldDataType.Text, Sensitivity.Secret, allowsMultiple = false),
            )
            val field = model.definitions.single { it.isUserDefined }
            assertEquals("Locker code", field.label)

            val id = add("A")
            assertTrue(model.saveField(id, null, FieldValueInput(field.id, value = "4821")))
            assertTrue(
                model.profile!!
                    .sections
                    .single()
                    .section == FieldSection.Custom,
            )

            assertFalse(model.deleteCustomField(field.id))
            assertEquals(Problem.FieldInUse, model.problem)

            val valueId =
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
                    .valueId
            model.deleteField(id, valueId)
            assertTrue(model.deleteCustomField(field.id))
            assertTrue(model.definitions.none { it.isUserDefined })
        }

    @Test
    fun aDuplicateLabelIsReportedAsATakenLabel() =
        runBlocking {
            assertFalse(
                model.createCustomField("phone", FieldDataType.Text, Sensitivity.Personal, allowsMultiple = false),
            )
            assertEquals(Problem.LabelTaken, model.problem)
            assertFalse(model.renameField("sys.phone", "Email"))
            assertEquals(Problem.LabelTaken, model.problem)
        }

    @Test
    fun renamingAndHidingAFieldChangeTheProfileAndTheAddList() =
        runBlocking {
            val id = add("A")
            model.saveField(id, null, FieldValueInput("sys.phone", value = "5550100"))

            assertTrue(model.renameField("sys.phone", "Mobile"))
            model.loadProfile(id)
            assertEquals(
                "Mobile",
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
                    .definition
                    ?.label,
            )

            model.setFieldHidden("sys.birthday", true)
            model.loadProfile(id)
            assertFalse("birthday" in model.profile!!.addable.map { it.systemKey })
            assertEquals(
                1,
                model.profile!!
                    .sections
                    .flatMap { it.rows }
                    .size,
                "the saved value stays",
            )

            assertTrue(model.renameField("sys.phone", ""))
            model.loadProfile(id)
            assertEquals(
                "Phone",
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
                    .definition
                    ?.label,
            )
        }

    @Test
    fun aBuiltInFieldCannotBeDeleted() =
        runBlocking {
            assertFalse(model.deleteCustomField("sys.phone"))
            assertEquals(Problem.NotAllowed, model.problem)
        }

    @Test
    fun aValueCanBeRaisedToALevelThatRequiresReauthentication() =
        runBlocking {
            val id = add("A")
            model.saveField(id, null, FieldValueInput("sys.note", value = "private", sensitivity = Sensitivity.Secret))
            val row =
                model.profile!!
                    .sections
                    .single()
                    .rows
                    .single()
            assertEquals(Sensitivity.Secret, row.sensitivity)
        }
}
