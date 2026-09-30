package io.github.behnooddev.voidmanager.core.database

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Runs against a real encrypted SQLite file. All keys and data are synthetic. */
class EncryptedVaultTest {
    private fun tempFile(): File =
        File.createTempFile("vault-test", ".db").also {
            it.delete()
            it.deleteOnExit()
        }

    private val keyA = DatabaseKey(ByteArray(32) { (it + 1).toByte() })
    private val keyB = DatabaseKey(ByteArray(32) { (it + 101).toByte() })

    @Test
    fun createsAnEncryptedVaultAndReopensItWithTheSameKey() {
        val file = tempFile()
        val factory = JvmDriverFactory(file)

        val first = VaultDatabaseOpener().open(factory, keyA) as OpenResult.Opened
        first.database.miscQueries.putMeta("probe", "synthetic")
        first.driver.close()
        val second = VaultDatabaseOpener().open(factory, keyA) as OpenResult.Opened

        assertEquals(
            "synthetic",
            second.database.miscQueries
                .getMeta("probe")
                .executeAsOne(),
        )
        assertTrue(factory.isEncryptedOnDisk())
        second.driver.close()
    }

    @Test
    fun fileContentsAreNotPlainText() {
        val file = tempFile()
        val opened = VaultDatabaseOpener().open(JvmDriverFactory(file), keyA) as OpenResult.Opened
        opened.database.miscQueries.putMeta("probe", "findable-marker-text")
        opened.driver.close()

        val text = file.readBytes().toString(Charsets.ISO_8859_1)

        assertTrue(!text.contains("findable-marker-text"))
        assertTrue(!text.startsWith("SQLite format 3"))
    }

    @Test
    fun aWrongKeyCannotOpenTheVault() {
        val file = tempFile()
        (VaultDatabaseOpener().open(JvmDriverFactory(file), keyA) as OpenResult.Opened).driver.close()

        assertFailsWith<Exception> { VaultDatabaseOpener().open(JvmDriverFactory(file), keyB) }
    }

    @Test
    fun aNewerSchemaIsRefusedAndLeftAlone() {
        val file = tempFile()
        val factory = JvmDriverFactory(file)
        val opened = VaultDatabaseOpener().open(factory, keyA) as OpenResult.Opened
        opened.database.miscQueries.putMeta(VaultDatabaseOpener.SCHEMA_VERSION_KEY, "99")
        opened.driver.close()

        val result = VaultDatabaseOpener().open(factory, keyA)

        assertEquals(OpenResult.RefusedNewerSchema(stored = 99, supported = VoidManagerDatabase.Schema.version), result)
    }
}
