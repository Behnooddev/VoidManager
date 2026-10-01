package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.data.VaultRepositories
import io.github.behnooddev.voidmanager.core.database.JvmDriverFactory
import io.github.behnooddev.voidmanager.core.database.OpenResult
import io.github.behnooddev.voidmanager.core.database.VaultDatabaseOpener
import io.github.behnooddev.voidmanager.core.model.FieldValueInput
import io.github.behnooddev.voidmanager.core.model.Outcome
import io.github.behnooddev.voidmanager.core.model.UuidV7Generator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The key hierarchy, the encrypted database and the repositories working together. All data is synthetic. */
class VaultIntegrationTest {
    private val dir =
        File.createTempFile("vault-integration", "").also {
            it.delete()
            it.mkdirs()
            it.deleteOnExit()
        }
    private val manager = VaultManager(FileKeyEnvelopeStore(File(dir, "vault.keys")), kdfParams = KdfParams.FLOOR)
    private val password = "synthetic integration password".toCharArray()
    private val databaseFile = File(dir, "vault.db")

    private fun open(session: VaultSession): Pair<VaultRepositories, OpenResult.Opened> {
        val opened =
            VaultDatabaseOpener().open(
                JvmDriverFactory(databaseFile),
                session.databaseKey(),
            ) as OpenResult.Opened
        val repositories =
            VaultRepositories(
                opened.database,
                session.valueCipher(),
                UuidV7Generator(clock = { System.currentTimeMillis() }),
                clock = { System.currentTimeMillis() },
            )
        return repositories to opened
    }

    @Test
    fun dataSurvivesLockAndUnlockAndSecretsAreEncryptedOnDisk() {
        val session = manager.create(password)
        val (repositories, opened) = open(session)
        val person = (repositories.entities.createPerson("Synthetic Person") as Outcome.Success).value
        repositories.fields.add(person.id, FieldValueInput("sys.national_id", value = "synthetic-id-0012345678"))
        repositories.fields.add(person.id, FieldValueInput("sys.phone", value = "0912 000 0000"))
        opened.driver.close()
        session.lock()

        val onDisk = databaseFile.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(onDisk.contains("Synthetic Person"))
        assertFalse(onDisk.contains("synthetic-id-0012345678"))

        val again = (manager.unlock(password) as UnlockResult.Unlocked).session
        val (reopened, reopenedDriver) = open(again)
        val fields = reopened.fields.listForEntity(person.id)

        assertEquals(2, fields.size)
        assertTrue(fields.any { it.value?.reveal() == "synthetic-id-0012345678" })
        reopenedDriver.driver.close()
    }

    @Test
    fun aLockedSessionCannotOpenTheDatabase() {
        val session = manager.create(password)
        val (_, opened) = open(session)
        opened.driver.close()
        session.lock()

        assertIs<Exception>(runCatching { session.databaseKey() }.exceptionOrNull())
    }

    @Test
    fun theDatabaseDoesNotOpenWithAKeyFromAnotherVault() {
        val session = manager.create(password)
        val (_, opened) = open(session)
        opened.driver.close()

        val other =
            VaultManager(
                FileKeyEnvelopeStore(File(dir, "other.keys")),
                kdfParams = KdfParams.FLOOR,
            ).create(password)

        assertIs<Exception>(
            runCatching {
                VaultDatabaseOpener().open(
                    JvmDriverFactory(databaseFile),
                    other.databaseKey(),
                )
            }.exceptionOrNull(),
        )
    }
}
