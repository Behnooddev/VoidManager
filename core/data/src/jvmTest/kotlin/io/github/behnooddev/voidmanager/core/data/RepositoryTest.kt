package io.github.behnooddev.voidmanager.core.data

import io.github.behnooddev.voidmanager.core.model.EntityKind
import io.github.behnooddev.voidmanager.core.model.FailureReason
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.Sensitivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    private val vault = TestVault()

    private fun <T> Outcome<T>.value(): T = (this as Outcome.Success).value

    private fun person(name: String) = vault.entities.createPerson(name).value()

    private fun failureOf(outcome: Outcome<*>): FailureReason = (outcome as Outcome.Failure).reason

    @Test
    fun quickAddCreatesAPersonWithFields() {
        val ali = person("Ali Rezaei")
        vault.fields.add(ali.id, FieldValueInput(definitionId = "sys.phone", value = "0912 345 6789")).value()

        val fields = vault.fields.listForEntity(ali.id)

        assertEquals(1, fields.size)
        assertEquals("0912 345 6789", fields.single().value!!.reveal())
        assertEquals(listOf(ali.id), vault.entities.listActive().map { it.id })
    }

    @Test
    fun blankNamesAreRejected() {
        assertEquals(FailureReason.InvalidInput, failureOf(vault.entities.createPerson("   ")))
    }

    @Test
    fun personalProfileIsCreatedOnceAndKeptOutOfPeopleLists() {
        val first = vault.entities.ensureSelf("Me")
        val second = vault.entities.ensureSelf("Someone else")

        assertEquals(first.id, second.id)
        assertEquals(EntityKind.Self, first.kind)
        assertTrue(vault.entities.listActive().isEmpty())
    }

    @Test
    fun personalProfileCannotBeTrashed() {
        val self = vault.entities.ensureSelf("Me")

        assertEquals(FailureReason.NotAllowed, failureOf(vault.entities.moveToTrash(self.id)))
    }

    @Test
    fun aPersonCanHaveSeveralPhoneNumbers() {
        val ali = person("Ali")
        vault.fields.add(ali.id, FieldValueInput("sys.phone", value = "0912 000 0001", isPrimary = true)).value()
        vault.fields.add(ali.id, FieldValueInput("sys.phone", value = "0912 000 0002")).value()

        assertEquals(2, vault.fields.listForEntity(ali.id).size)
    }

    @Test
    fun singleValueFieldsRejectASecondValue() {
        val ali = person("Ali")
        vault.fields.add(ali.id, FieldValueInput("sys.birthday", value = "1990-01-01")).value()

        val second = vault.fields.add(ali.id, FieldValueInput("sys.birthday", value = "1991-01-01"))

        assertEquals(FailureReason.Conflict, failureOf(second))
    }

    @Test
    fun onlyOneValueIsPrimaryPerField() {
        val ali = person("Ali")
        val first =
            vault.fields
                .add(
                    ali.id,
                    FieldValueInput("sys.phone", value = "111 111 1111", isPrimary = true),
                ).value()
        val second =
            vault.fields
                .add(
                    ali.id,
                    FieldValueInput("sys.phone", value = "222 222 2222", isPrimary = true),
                ).value()

        assertFalse(vault.fields.get(first.id)!!.isPrimary)
        assertTrue(vault.fields.get(second.id)!!.isPrimary)
    }

    @Test
    fun sensitiveValuesAreStoredOnlyAsCiphertext() {
        val ali = person("Ali")
        val added = vault.fields.add(ali.id, FieldValueInput("sys.national_id", value = "0012345678")).value()

        val stored =
            vault.database.fieldValueQueries
                .selectValueById(
                    added.id,
                ) { _, _, _, _, plain, cipher, _, _, normalized, _, _, _, _, _, _, _ ->
                    Triple(plain, cipher, normalized)
                }.executeAsOne()

        assertNull(stored.first)
        assertNotNull(stored.second)
        assertNull(stored.third)
        assertEquals(
            "0012345678",
            vault.fields
                .get(added.id)!!
                .value!!
                .reveal(),
        )
        assertEquals(
            "[redacted]",
            vault.fields
                .get(added.id)!!
                .value
                .toString(),
        )
    }

    @Test
    fun ciphertextMovedToAnotherRowFailsToDecrypt() {
        val ali = person("Ali")
        val reza = person("Reza")
        val a = vault.fields.add(ali.id, FieldValueInput("sys.national_id", value = "0012345678")).value()
        val b = vault.fields.add(reza.id, FieldValueInput("sys.national_id", value = "0087654321")).value()
        val stolen =
            vault.database.fieldValueQueries
                .selectValueById(
                    a.id,
                ) { _, _, _, _, _, cipher, _, _, _, _, _, _, _, _, _, _ -> cipher }
                .executeAsOne()!!
        vault.overwriteCiphertext(valueId = b.id, cipherBytes = stolen)

        assertFailsWith<IllegalStateException> { vault.fields.get(b.id) }
    }

    @Test
    fun aLockedVaultRefusesToReadOrWriteSensitiveValues() {
        val ali = person("Ali")
        val added = vault.fields.add(ali.id, FieldValueInput("sys.national_id", value = "0012345678")).value()
        vault.cipher.locked = true

        assertFailsWith<VaultLockedException> { vault.fields.get(added.id) }
        assertFailsWith<VaultLockedException> {
            vault.fields.add(ali.id, FieldValueInput("sys.bank_info", value = "IBAN synthetic"))
        }
    }

    @Test
    fun accountPasswordIsEncryptedAndUsernameIsNot() {
        val ali = person("Ali")
        val account =
            vault.fields
                .add(
                    ali.id,
                    FieldValueInput(
                        definitionId = "sys.account",
                        parts =
                            mapOf(
                                "service" to "Example Mail",
                                "username" to "ali.example",
                                "password" to "synthetic-pass-1",
                            ),
                    ),
                ).value()

        val parts = account.parts.associateBy { it.key }

        assertEquals(Sensitivity.Personal, parts.getValue("username").sensitivity)
        assertEquals(Sensitivity.Secret, parts.getValue("password").sensitivity)
        assertEquals("[redacted]", parts.getValue("password").value.toString())
        assertEquals("synthetic-pass-1", parts.getValue("password").value!!.reveal())
        assertEquals("ali.example", parts.getValue("username").value.toString())
    }

    @Test
    fun unknownPartsAreRejected() {
        val ali = person("Ali")

        val result = vault.fields.add(ali.id, FieldValueInput("sys.account", parts = mapOf("nope" to "x")))

        assertEquals(FailureReason.InvalidInput, failureOf(result))
    }

    @Test
    fun sensitivityCannotBeLoweredBelowTheFieldDefault() {
        val ali = person("Ali")

        val result =
            vault.fields.add(
                ali.id,
                FieldValueInput("sys.national_id", value = "1", sensitivity = Sensitivity.Personal),
            )

        assertEquals(FailureReason.SensitivityLowered, failureOf(result))
    }

    @Test
    fun sensitivityCanBeRaisedAboveTheFieldDefault() {
        val ali = person("Ali")
        val added =
            vault.fields
                .add(
                    ali.id,
                    FieldValueInput("sys.phone", value = "0912 345 6789", sensitivity = Sensitivity.Sensitive),
                ).value()

        assertEquals(Sensitivity.Sensitive, added.sensitivity)
        assertEquals("[redacted]", added.value.toString())
    }

    @Test
    fun replacingAValueUpdatesItAndItsParts() {
        val ali = person("Ali")
        val added =
            vault.fields
                .add(
                    ali.id,
                    FieldValueInput("sys.account", parts = mapOf("service" to "Old", "password" to "one")),
                ).value()

        val replaced =
            vault.fields
                .replace(
                    added.id,
                    FieldValueInput("sys.account", parts = mapOf("service" to "New", "password" to "two")),
                ).value()

        val parts = replaced.parts.associateBy { it.key }
        assertEquals("New", parts.getValue("service").value!!.reveal())
        assertEquals("two", parts.getValue("password").value!!.reveal())
    }

    @Test
    fun searchFindsByNameIgnoringCaseAndAccents() {
        person("Jos\u00E9 Garc\u00EDa")

        assertEquals(1, vault.search.search("JOSE garcia").size)
    }

    @Test
    fun searchFindsByPhoneRegardlessOfFormatting() {
        val ali = person("Ali")
        vault.fields.add(ali.id, FieldValueInput("sys.phone", value = "+98 912 345 6789")).value()

        assertEquals(listOf(ali.id), vault.search.search("0912-345-6789").map { it.id })
        assertEquals(listOf(ali.id), vault.search.search("912 345").map { it.id })
    }

    @Test
    fun searchNeverFindsSecretsOrSensitiveValues() {
        val ali = person("Ali")
        vault.fields
            .add(
                ali.id,
                FieldValueInput("sys.account", parts = mapOf("password" to "hunter2-synthetic")),
            ).value()
        vault.fields.add(ali.id, FieldValueInput("sys.national_id", value = "0012345678")).value()

        assertTrue(vault.search.search("hunter2").isEmpty())
        assertTrue(vault.search.search("0012345678").isEmpty())
    }

    @Test
    fun percentSignsInSearchAreLiteral() {
        person("Ali")

        assertTrue(vault.search.search("%").isEmpty())
    }

    @Test
    fun trashHidesRestoreReturnsAndPurgeDeletesEverything() {
        val ali = person("Ali")
        vault.fields.add(ali.id, FieldValueInput("sys.phone", value = "0912 345 6789")).value()

        vault.entities.moveToTrash(ali.id).value()
        assertTrue(vault.entities.listActive().isEmpty())
        assertTrue(vault.search.search("Ali").isEmpty())
        assertEquals(listOf(ali.id), vault.entities.listTrashed().map { it.id })

        vault.entities.restore(ali.id).value()
        assertEquals(1, vault.search.search("Ali").size)

        assertEquals(FailureReason.NotAllowed, failureOf(vault.entities.purge(ali.id)))
        vault.entities.moveToTrash(ali.id).value()
        vault.entities.purge(ali.id).value()
        assertNull(vault.entities.get(ali.id))
        assertTrue(vault.fields.listForEntity(ali.id).isEmpty())
    }

    @Test
    fun relationshipsCreateBothDirections() {
        val ali = person("Ali")
        val reza = person("Reza")

        vault.relationships.create(ali.id, reza.id, typeKey = "brother").value()

        assertEquals(listOf("sys.brother"), vault.relationships.listFor(ali.id).map { it.typeId })
        assertEquals(listOf("sys.sibling"), vault.relationships.listFor(reza.id).map { it.typeId })
    }

    @Test
    fun theReciprocalTypeCanBeChosen() {
        val ali = person("Ali")
        val sara = person("Sara")

        vault.relationships.create(ali.id, sara.id, typeKey = "brother", reciprocalTypeKey = "sister").value()

        assertEquals(listOf("sys.sister"), vault.relationships.listFor(sara.id).map { it.typeId })
    }

    @Test
    fun duplicateAndSelfRelationshipsAreRejected() {
        val ali = person("Ali")
        val reza = person("Reza")
        vault.relationships.create(ali.id, reza.id, typeKey = "friend").value()

        assertEquals(FailureReason.Conflict, failureOf(vault.relationships.create(ali.id, reza.id, "friend")))
        assertEquals(FailureReason.InvalidInput, failureOf(vault.relationships.create(ali.id, ali.id, "friend")))
        assertEquals(FailureReason.InvalidInput, failureOf(vault.relationships.create(ali.id, reza.id, "unknown")))
    }

    @Test
    fun deletingARelationshipRemovesBothDirections() {
        val ali = person("Ali")
        val reza = person("Reza")
        val created = vault.relationships.create(ali.id, reza.id, typeKey = "parent").value()

        vault.relationships.delete(created.pairId).value()

        assertTrue(vault.relationships.listFor(ali.id).isEmpty())
        assertTrue(vault.relationships.listFor(reza.id).isEmpty())
    }

    @Test
    fun seedingIsRepeatable() {
        VaultSeeder.seed(vault.database)
        VaultSeeder.seed(vault.database)

        val count =
            vault.database.fieldDefinitionQueries
                .selectAll { id, _, _, _, _, _, _, _, _, _, _, _, _ ->
                    id
                }.executeAsList()
                .size

        assertEquals(io.github.behnooddev.voidmanager.core.model.FieldRegistry.definitions.size, count)
    }
}
