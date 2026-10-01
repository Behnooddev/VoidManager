package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.data.CipherContext
import io.github.behnooddev.voidmanager.core.data.VaultLockedException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class MemoryStore : KeyEnvelopeStore {
    var bytes: ByteArray? = null

    override fun read(): ByteArray? = bytes

    override fun write(bytes: ByteArray) {
        this.bytes = bytes.copyOf()
    }
}

class VaultManagerTest {
    private val store = MemoryStore()
    private val manager = VaultManager(store, kdfParams = KdfParams.FLOOR)
    private val password = "correct horse battery staple".toCharArray()

    private fun unlocked(result: UnlockResult): VaultSession = (result as UnlockResult.Unlocked).session

    @Test
    fun aNewVaultUnlocksWithItsPassword() {
        manager.create(password).lock()

        val session = unlocked(manager.unlock(password))

        assertTrue(session.isUnlocked)
    }

    @Test
    fun theSameKeysComeBackOnEveryUnlock() {
        val first = manager.create(password)
        val keyA = first.mediaKey()
        first.lock()

        val second = unlocked(manager.unlock(password))

        assertContentEquals(keyA, second.mediaKey())
    }

    @Test
    fun aWrongPasswordDoesNotUnlock() {
        manager.create(password).lock()

        assertEquals(UnlockResult.WrongPasswordOrCorrupt, manager.unlock("not the password".toCharArray()))
    }

    @Test
    fun unlockingWithoutAVaultReportsNoVault() {
        assertEquals(UnlockResult.NoVault, manager.unlock(password))
    }

    @Test
    fun aVaultCannotBeCreatedTwice() {
        manager.create(password).lock()

        assertFailsWith<IllegalStateException> { manager.create(password) }
    }

    @Test
    fun emptyPasswordsAreRejected() {
        assertFailsWith<IllegalArgumentException> { manager.create(CharArray(0)) }
    }

    @Test
    fun theKeyFileHoldsNoPlaintextKeyMaterial() {
        val session = manager.create(password)
        val mediaKey = session.mediaKey()
        val file = store.bytes!!.toList()

        assertFalse(file.windowed(8).any { window -> mediaKey.toList().windowed(8).any { it == window } })
    }

    @Test
    fun lockingOverwritesKeysAndRefusesFurtherUse() {
        val session = manager.create(password)
        val cipher = session.valueCipher()
        val context = CipherContext("field_value", "row-1", "value")
        val sealed = cipher.encrypt(context, "synthetic")

        session.lock()

        assertFalse(session.isUnlocked)
        assertFailsWith<VaultLockedException> { session.databaseKey() }
        assertFailsWith<VaultLockedException> { session.mediaKey() }
        assertFailsWith<VaultLockedException> { cipher.encrypt(context, "x") }
        assertFailsWith<VaultLockedException> { cipher.decrypt(context, sealed) }
    }

    @Test
    fun relockingAfterUnlockWorksAgain() {
        manager.create(password).lock()
        val session = unlocked(manager.unlock(password))
        val cipher = session.valueCipher()
        val context = CipherContext("field_value", "row-1", "value")

        assertEquals("synthetic", cipher.decrypt(context, cipher.encrypt(context, "synthetic")))
    }

    @Test
    fun valuesDecryptOnlyInTheContextTheyWereEncryptedFor() {
        val cipher = manager.create(password).valueCipher()
        val sealed = cipher.encrypt(CipherContext("field_value", "row-1", "value"), "synthetic")

        assertFailsWith<AuthenticationFailedException> {
            cipher.decrypt(CipherContext("field_value", "row-2", "value"), sealed)
        }
        assertFailsWith<AuthenticationFailedException> {
            cipher.decrypt(CipherContext("field_value", "row-1", "note"), sealed)
        }
        assertFailsWith<AuthenticationFailedException> {
            cipher.decrypt(CipherContext("field_part", "row-1", "value"), sealed)
        }
    }

    @Test
    fun valuesFromAnotherVaultCannotBeRead() {
        val other = VaultManager(MemoryStore(), kdfParams = KdfParams.FLOOR)
        val context = CipherContext("field_value", "row-1", "value")
        val sealed = other.create(password).valueCipher().encrypt(context, "synthetic")

        assertFailsWith<AuthenticationFailedException> {
            manager
                .create(
                    password,
                ).valueCipher()
                .decrypt(context, sealed)
        }
    }

    @Test
    fun changingThePasswordKeepsTheKeysAndReplacesThePassword() {
        val session = manager.create(password)
        val keyBefore = session.mediaKey()
        val fileBefore = store.bytes!!.copyOf()
        val newPassword = "a completely different phrase".toCharArray()

        val result = manager.changePassword(session, password, newPassword)

        assertEquals(ChangePasswordResult.Changed, result)
        assertFalse(fileBefore.contentEquals(store.bytes!!))
        assertEquals(UnlockResult.WrongPasswordOrCorrupt, manager.unlock(password))
        assertContentEquals(keyBefore, unlocked(manager.unlock(newPassword)).mediaKey())
    }

    @Test
    fun changingThePasswordNeedsTheCurrentPassword() {
        val session = manager.create(password)
        val fileBefore = store.bytes!!.copyOf()

        val result = manager.changePassword(session, "wrong".toCharArray(), "new password here".toCharArray())

        assertEquals(ChangePasswordResult.WrongCurrentPassword, result)
        assertContentEquals(fileBefore, store.bytes)
    }

    @Test
    fun aSessionFromAnotherVaultCannotChangeThePassword() {
        val stranger = VaultManager(MemoryStore(), kdfParams = KdfParams.FLOOR).create(password)
        manager.create(password)

        assertFailsWith<IllegalArgumentException> {
            manager.changePassword(stranger, password, "new password here".toCharArray())
        }
    }

    @Test
    fun differentUnicodeFormsOfThePasswordUnlockTheSameVault() {
        val composed = "caf\u00E9 password".toCharArray()
        val decomposed = "cafe\u0301 password".toCharArray()
        manager.create(composed).lock()

        assertIs<UnlockResult.Unlocked>(manager.unlock(decomposed))
    }

    @Test
    fun aTamperedCostParameterFailsInsteadOfUnlocking() {
        manager.create(password).lock()
        val bytes = store.bytes!!
        // The iteration count is the second 32-bit field after the 6-byte magic and version.
        bytes[6 + 4 + 3] = (bytes[6 + 4 + 3].toInt() + 1).toByte()

        assertEquals(UnlockResult.WrongPasswordOrCorrupt, manager.unlock(password))
    }

    @Test
    fun aTamperedWrappedKeyFails() {
        manager.create(password).lock()
        val bytes = store.bytes!!
        bytes[bytes.size - 10] = (bytes[bytes.size - 10].toInt() xor 1).toByte()

        assertEquals(UnlockResult.WrongPasswordOrCorrupt, manager.unlock(password))
    }

    @Test
    fun malformedKeyFilesAreReportedNotThrown() {
        manager.create(password).lock()
        val good = store.bytes!!

        store.bytes = good.copyOf(good.size - 3)
        assertEquals(UnlockResult.InvalidKeyFile, manager.unlock(password))

        store.bytes = "garbage-bytes-not-a-key-file".encodeToByteArray()
        assertEquals(UnlockResult.InvalidKeyFile, manager.unlock(password))

        store.bytes = good + byteArrayOf(0)
        assertEquals(UnlockResult.InvalidKeyFile, manager.unlock(password))
    }

    @Test
    fun aNewerKeyFileVersionIsReportedAsUnsupported() {
        manager.create(password).lock()
        val bytes = store.bytes!!
        bytes[5] = 9

        assertEquals(UnlockResult.UnsupportedVersion(9), manager.unlock(password))
    }

    @Test
    fun aKeyFileAskingForHugeMemoryIsRefusedBeforeAnyWork() {
        manager.create(password).lock()
        val bytes = store.bytes!!
        // Memory cost field: set the high byte so the value exceeds the ceiling.
        bytes[6] = 0x7F

        assertEquals(UnlockResult.InvalidKeyFile, manager.unlock(password))
    }

    @Test
    fun deviceUnlockWorksOnceEnabledAndNotBefore() {
        val deviceKey = ByteArray(32) { (it + 40).toByte() }
        val session = manager.create(password)
        assertEquals(UnlockResult.DeviceUnlockNotEnabled, manager.unlockWithDeviceKey(deviceKey))

        manager.enableDeviceUnlock(session, deviceKey)
        val viaDevice = unlocked(manager.unlockWithDeviceKey(deviceKey))

        assertContentEquals(session.mediaKey(), viaDevice.mediaKey())
    }

    @Test
    fun aWrongDeviceKeyDoesNotUnlock() {
        val session = manager.create(password)
        manager.enableDeviceUnlock(session, ByteArray(32) { 1 })

        assertEquals(UnlockResult.WrongPasswordOrCorrupt, manager.unlockWithDeviceKey(ByteArray(32) { 2 }))
    }

    @Test
    fun deviceUnlockSurvivesAPasswordChangeAndCanBeDisabled() {
        val deviceKey = ByteArray(32) { 3 }
        val session = manager.create(password)
        manager.enableDeviceUnlock(session, deviceKey)

        manager.changePassword(session, password, "another long phrase".toCharArray())
        assertIs<UnlockResult.Unlocked>(manager.unlockWithDeviceKey(deviceKey))

        manager.disableDeviceUnlock()
        assertEquals(UnlockResult.DeviceUnlockNotEnabled, manager.unlockWithDeviceKey(deviceKey))
        assertIs<UnlockResult.Unlocked>(manager.unlock("another long phrase".toCharArray()))
    }

    @Test
    fun envelopeSurvivesSerialization() {
        manager.create(password).lock()
        val bytes = store.bytes!!

        assertContentEquals(bytes, KeyEnvelope.parse(bytes).toBytes())
    }

    @Test
    fun fileStoreWritesAtomicallyAndReadsBack() {
        val dir =
            File.createTempFile("vault-dir", "").also {
                it.delete()
                it.mkdirs()
                it.deleteOnExit()
            }
        val file = File(dir, "vault.keys")
        val fileStore = FileKeyEnvelopeStore(file)
        assertEquals(null, fileStore.read())

        fileStore.write(byteArrayOf(1, 2, 3))
        fileStore.write(byteArrayOf(4, 5))

        assertContentEquals(byteArrayOf(4, 5), fileStore.read())
        assertFalse(File(dir, "vault.keys.tmp").exists())
        file.delete()
    }

    @Test
    fun aVaultOnDiskUnlocksAfterReopening() {
        val dir =
            File.createTempFile("vault-dir2", "").also {
                it.delete()
                it.mkdirs()
                it.deleteOnExit()
            }
        val fileStore = FileKeyEnvelopeStore(File(dir, "vault.keys"))
        VaultManager(fileStore, kdfParams = KdfParams.FLOOR).create(password).lock()

        val reopened = VaultManager(FileKeyEnvelopeStore(File(dir, "vault.keys")), kdfParams = KdfParams.FLOOR)

        assertIs<UnlockResult.Unlocked>(reopened.unlock(password))
    }
}
