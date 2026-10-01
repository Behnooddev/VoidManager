package io.github.behnooddev.voidmanager.core.crypto

import io.github.behnooddev.voidmanager.core.data.CipherContext
import io.github.behnooddev.voidmanager.core.data.ValueCipher
import io.github.behnooddev.voidmanager.core.data.VaultLockedException
import io.github.behnooddev.voidmanager.core.database.DatabaseKey

/** What a derived key is for. The purpose is part of the derivation, so the keys are independent. */
enum class KeyPurpose(
    val info: String,
) {
    Database("vm1/db"),
    Field("vm1/field"),
    Media("vm1/media"),
}

object VaultKeys {
    const val KEY_BYTES = 32
    private val SALT = "VoidManager-vm1-keys".encodeToByteArray()

    fun derive(
        vaultKey: ByteArray,
        purpose: KeyPurpose,
    ): ByteArray = Hkdf.derive(vaultKey, SALT, purpose.info.encodeToByteArray(), KEY_BYTES)
}

/**
 * The unlocked state of a vault. It holds the vault key in memory until [lock] is called, which
 * overwrites the key and everything derived from it. After that, every key request throws
 * [VaultLockedException].
 *
 * The JVM cannot guarantee that no other copy of key material exists in memory; overwriting
 * reduces the exposure but does not remove it.
 */
class VaultSession internal constructor(
    vaultKey: ByteArray,
) : AutoCloseable {
    private val guard = Any()
    private var vaultKey: ByteArray? = vaultKey
    private var fieldKey: ByteArray? = null

    val isUnlocked: Boolean get() = synchronized(guard) { vaultKey != null }

    /** The key for the encrypted database. The caller owns the returned object. */
    fun databaseKey(): DatabaseKey = DatabaseKey(derive(KeyPurpose.Database))

    /** The key for photo and attachment encryption. The caller owns the returned array and must wipe it. */
    fun mediaKey(): ByteArray = derive(KeyPurpose.Media)

    fun valueCipher(): ValueCipher = SessionValueCipher(this)

    internal fun fieldKey(): ByteArray =
        synchronized(guard) {
            val existing = fieldKey
            if (existing != null) return existing
            val created = VaultKeys.derive(requireKey(), KeyPurpose.Field)
            fieldKey = created
            created
        }

    internal fun vaultKeyCopy(): ByteArray = synchronized(guard) { requireKey().copyOf() }

    fun lock() {
        synchronized(guard) {
            vaultKey?.fill(0)
            fieldKey?.fill(0)
            vaultKey = null
            fieldKey = null
        }
    }

    override fun close() = lock()

    private fun derive(purpose: KeyPurpose): ByteArray =
        synchronized(guard) {
            VaultKeys.derive(requireKey(), purpose)
        }

    private fun requireKey(): ByteArray = vaultKey ?: throw VaultLockedException()
}

internal class SessionValueCipher(
    private val session: VaultSession,
) : ValueCipher {
    override fun encrypt(
        context: CipherContext,
        plaintext: String,
    ): ByteArray = AesGcm.encrypt(session.fieldKey(), context.toAssociatedData(), plaintext.encodeToByteArray())

    override fun decrypt(
        context: CipherContext,
        ciphertext: ByteArray,
    ): String = AesGcm.decrypt(session.fieldKey(), context.toAssociatedData(), ciphertext).decodeToString()
}
