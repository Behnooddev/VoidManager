package io.github.behnooddev.voidmanager.core.data

/** Identifies the slot a ciphertext belongs to, so it cannot be copied to another row or slot unnoticed. */
data class CipherContext(
    val table: String,
    val rowId: String,
    /** `value`, `note`, or the part key for composite values. */
    val slot: String,
) {
    /** Bytes bound into the authentication tag as associated data. */
    fun toAssociatedData(): ByteArray = "$FORMAT|$table|$rowId|$slot".encodeToByteArray()

    companion object {
        const val FORMAT = "vm1"
    }
}

/**
 * Encrypts and decrypts individual stored values. The real implementation comes from the key
 * management work in Phase 3; repositories depend only on this interface.
 */
interface ValueCipher {
    /** Throws [VaultLockedException] when the vault is locked. */
    fun encrypt(
        context: CipherContext,
        plaintext: String,
    ): ByteArray

    /** Throws [VaultLockedException] when the vault is locked, and an exception when authentication fails. */
    fun decrypt(
        context: CipherContext,
        ciphertext: ByteArray,
    ): String
}

class VaultLockedException : IllegalStateException("The vault is locked")
