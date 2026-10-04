package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.data.OpenOutcome
import io.github.behnooddev.voidmanager.core.data.OpenedVault
import io.github.behnooddev.voidmanager.core.database.JvmDriverFactory
import io.github.behnooddev.voidmanager.core.model.Outcome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The vault service on a real encrypted database file. All data is synthetic. */
class VaultServiceTest {
    private val dir =
        File.createTempFile("vault-service", "").also {
            it.delete()
            it.mkdirs()
            it.deleteOnExit()
        }
    private val password = "synthetic service password"

    private fun service(): VaultService =
        VaultService(
            manager = VaultManager(FileKeyEnvelopeStore(File(dir, "vault.keys")), kdfParams = KdfParams.FLOOR),
            driverFactory = JvmDriverFactory(File(dir, "vault.db")),
        )

    private fun opened(outcome: OpenOutcome): OpenedVault = assertIs<OpenOutcome.Opened>(outcome).vault

    @Test
    fun createThenUnlockKeepsTheStoredData() {
        val service = service()
        assertFalse(service.exists())

        val vault = opened(service.create(password.toCharArray()))
        val person = (vault.repositories.entities.createPerson("Synthetic Person") as Outcome.Success).value
        vault.lock()

        assertTrue(service.exists())
        val again = opened(service.unlock(password.toCharArray()))
        assertEquals(
            listOf(person.id),
            again.repositories.entities
                .listActive()
                .map { it.id },
        )
        again.lock()
    }

    @Test
    fun aWrongPasswordOpensNothing() {
        val service = service()
        opened(service.create(password.toCharArray())).lock()
        assertEquals(OpenOutcome.WrongPasswordOrCorrupt, service.unlock("another password".toCharArray()))
    }

    @Test
    fun unlockWithoutAVaultReportsNoVault() {
        assertEquals(OpenOutcome.NoVault, service().unlock(password.toCharArray()))
    }

    @Test
    fun createRefusesToReplaceAnExistingVault() {
        val service = service()
        opened(service.create(password.toCharArray())).lock()
        assertEquals(OpenOutcome.AlreadyExists, service.create("a different password".toCharArray()))
        assertIs<OpenOutcome.Opened>(service.unlock(password.toCharArray())).vault.lock()
    }

    @Test
    fun lockingTwiceIsHarmlessAndTheKeyFileSurvives() {
        val service = service()
        val vault = opened(service.create(password.toCharArray()))
        vault.lock()
        vault.lock()
        assertTrue(File(dir, "vault.keys").exists())
    }

    @Test
    fun aDamagedKeyFileIsReportedAsSuch() {
        val service = service()
        opened(service.create(password.toCharArray())).lock()
        File(dir, "vault.keys").writeBytes(ByteArray(40) { 7 })
        assertEquals(OpenOutcome.InvalidKeyFile, service.unlock(password.toCharArray()))
    }
}
