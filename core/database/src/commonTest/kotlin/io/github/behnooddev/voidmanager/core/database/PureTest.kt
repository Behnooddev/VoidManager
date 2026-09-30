package io.github.behnooddev.voidmanager.core.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PureTest {
    @Test
    fun missingSchemaMeansCreate() {
        assertEquals(OpenPlan.Create, MigrationPlanner.plan(null, 1))
    }

    @Test
    fun equalVersionsAreUpToDate() {
        assertEquals(OpenPlan.UpToDate, MigrationPlanner.plan(3, 3))
    }

    @Test
    fun olderStoredSchemaIsMigrated() {
        assertEquals(OpenPlan.Migrate(1, 3), MigrationPlanner.plan(1, 3))
    }

    @Test
    fun newerStoredSchemaIsRefused() {
        assertEquals(OpenPlan.RefuseNewer(5, 3), MigrationPlanner.plan(5, 3))
    }

    @Test
    fun keyIsFormattedAsSqlCipherRawKey() {
        val key = DatabaseKey(ByteArray(32) { it.toByte() })

        assertEquals("x'000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f'", key.toSqlCipherRawKey())
    }

    @Test
    fun keyMustBe32Bytes() {
        assertFailsWith<IllegalArgumentException> { DatabaseKey(ByteArray(16)) }
    }

    @Test
    fun keyNeverPrintsItself() {
        val key = DatabaseKey(ByteArray(32) { 0x7F })

        assertFalse(key.toString().contains("7f"))
        assertEquals("DatabaseKey([redacted])", key.toString())
    }

    @Test
    fun plainSqliteHeaderIsNotEncrypted() {
        val plain = "SQLite format 3".encodeToByteArray() + byteArrayOf(0)

        assertFalse(EncryptionCheck.looksEncrypted(plain))
    }

    @Test
    fun randomHeaderCountsAsEncrypted() {
        assertTrue(EncryptionCheck.looksEncrypted(ByteArray(16) { (it * 37 + 11).toByte() }))
    }

    @Test
    fun truncatedHeaderIsNotTrusted() {
        assertFalse(EncryptionCheck.looksEncrypted(ByteArray(4)))
    }
}
