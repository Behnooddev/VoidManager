package io.github.behnooddev.voidmanager.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegistryTest {
    @Test
    fun systemKeysAreUnique() {
        val keys = FieldRegistry.definitions.map { it.systemKey }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun partKeysAreUniqueWithinADefinition() {
        for (definition in FieldRegistry.definitions) {
            val keys = definition.parts.map { it.key }
            assertEquals(keys.size, keys.toSet().size, definition.systemKey)
        }
    }

    @Test
    fun passwordsPinsAndCvvAreSecret() {
        val account = FieldRegistry.bySystemKey("account")
        val card = FieldRegistry.bySystemKey("card")

        assertNotNull(account)
        assertNotNull(card)
        assertEquals(Sensitivity.Secret, account.parts.single { it.key == "password" }.sensitivity)
        assertEquals(Sensitivity.Secret, account.parts.single { it.key == "recovery" }.sensitivity)
        assertEquals(Sensitivity.Secret, card.parts.single { it.key == "cvv2" }.sensitivity)
        assertEquals(Sensitivity.Secret, card.parts.single { it.key == "pin" }.sensitivity)
    }

    @Test
    fun financialAndNationalIdAreAtLeastSensitive() {
        assertTrue(FieldRegistry.bySystemKey("national_id")!!.defaultSensitivity.isStoredEncrypted)
        val card = FieldRegistry.bySystemKey("card")!!
        assertTrue(card.parts.all { it.sensitivity.isStoredEncrypted })
    }

    @Test
    fun definitionIdsAreDerivedFromSystemKeys() {
        assertEquals("sys.phone", FieldRegistry.bySystemKey("phone")!!.id)
    }

    @Test
    fun contactFieldsAllowMultipleValues() {
        assertTrue(FieldRegistry.bySystemKey("phone")!!.allowsMultiple)
        assertTrue(FieldRegistry.bySystemKey("email")!!.allowsMultiple)
        assertFalse(FieldRegistry.bySystemKey("name")!!.allowsMultiple)
    }

    @Test
    fun everyReciprocalTypeExists() {
        for (type in RelationshipTypes.all) {
            assertNotNull(RelationshipTypes.reciprocalOf(type.key), type.key)
        }
    }

    @Test
    fun symmetricTypesAreTheirOwnReciprocal() {
        for (key in listOf("sibling", "spouse", "partner", "friend", "colleague", "cousin", "relative")) {
            assertEquals(key, RelationshipTypes.reciprocalOf(key)!!.key)
        }
    }

    @Test
    fun parentAndChildAreReciprocal() {
        assertEquals("child", RelationshipTypes.reciprocalOf("parent")!!.key)
        assertEquals("parent", RelationshipTypes.reciprocalOf("child")!!.key)
        assertEquals("manager", RelationshipTypes.reciprocalOf("report")!!.key)
    }

    @Test
    fun genderedTypesFallBackToNeutralReciprocals() {
        assertEquals("sibling", RelationshipTypes.reciprocalOf("brother")!!.key)
        assertEquals("sibling", RelationshipTypes.reciprocalOf("sister")!!.key)
        assertEquals("child", RelationshipTypes.reciprocalOf("father")!!.key)
        assertEquals("parent", RelationshipTypes.reciprocalOf("daughter")!!.key)
    }

    @Test
    fun relationshipKeysAreUnique() {
        val keys = RelationshipTypes.all.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun protectedTextNeverPrintsItsValue() {
        val text = FieldText("hunter2", isProtected = true)

        assertEquals("[redacted]", text.toString())
        assertEquals("pw=[redacted]", "pw=$text")
        assertEquals("hunter2", text.reveal())
    }

    @Test
    fun unprotectedTextPrintsNormally() {
        assertEquals("Ali", FieldText("Ali", isProtected = false).toString())
    }

    @Test
    fun sensitivityLevelsDriveStorageAndSearch() {
        assertFalse(Sensitivity.Personal.isStoredEncrypted)
        assertTrue(Sensitivity.Sensitive.isStoredEncrypted)
        assertTrue(Sensitivity.Secret.isStoredEncrypted)
        assertTrue(Sensitivity.Personal.isSearchable)
        assertFalse(Sensitivity.Sensitive.isSearchable)
        assertEquals(Sensitivity.Secret, Sensitivity.fromLevel(3))
    }
}
