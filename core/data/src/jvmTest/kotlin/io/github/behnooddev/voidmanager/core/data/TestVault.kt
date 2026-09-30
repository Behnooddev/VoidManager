package io.github.behnooddev.voidmanager.core.data

import app.cash.sqldelight.db.SqlDriver
import io.github.behnooddev.voidmanager.core.database.DatabaseKey
import io.github.behnooddev.voidmanager.core.database.JvmDriverFactory
import io.github.behnooddev.voidmanager.core.database.OpenResult
import io.github.behnooddev.voidmanager.core.database.VaultDatabaseOpener
import io.github.behnooddev.voidmanager.core.database.VoidManagerDatabase
import io.github.behnooddev.voidmanager.core.model.UuidV7Generator
import java.io.File

/**
 * Test double for [ValueCipher]. It is not encryption. It obscures the bytes and, like the real
 * implementation must, refuses to decrypt a value under a context other than the one it was
 * encrypted for.
 */
class FakeValueCipher : ValueCipher {
    var locked = false

    override fun encrypt(
        context: CipherContext,
        plaintext: String,
    ): ByteArray {
        if (locked) throw VaultLockedException()
        val tag = context.toAssociatedData()
        val body = plaintext.encodeToByteArray().map { (it.toInt() xor MASK).toByte() }.toByteArray()
        return byteArrayOf(tag.size.toByte()) + tag + body
    }

    override fun decrypt(
        context: CipherContext,
        ciphertext: ByteArray,
    ): String {
        if (locked) throw VaultLockedException()
        val tagSize = ciphertext[0].toInt()
        val tag = ciphertext.copyOfRange(1, 1 + tagSize)
        check(tag.contentEquals(context.toAssociatedData())) { "authentication failed" }
        val body = ciphertext.copyOfRange(1 + tagSize, ciphertext.size)
        return body.map { (it.toInt() xor MASK).toByte() }.toByteArray().decodeToString()
    }

    private companion object {
        const val MASK = 0x5A
    }
}

/** A real encrypted vault in a temporary file, with synthetic keys. */
class TestVault {
    private val file =
        File.createTempFile("vault-data-test", ".db").also {
            it.delete()
            it.deleteOnExit()
        }
    private var now = 1_000L

    val cipher = FakeValueCipher()
    val database: VoidManagerDatabase
    private val driver: SqlDriver
    val repositories: VaultRepositories

    init {
        val opened =
            VaultDatabaseOpener().open(
                JvmDriverFactory(file),
                DatabaseKey(ByteArray(32) { (it * 3 + 1).toByte() }),
            ) as OpenResult.Opened
        database = opened.database
        driver = opened.driver
        val ids = UuidV7Generator(clock = { now })
        repositories = VaultRepositories(database, cipher, ids) { ++now }
    }

    /** Simulates an attacker copying ciphertext between rows of the database file. */
    fun overwriteCiphertext(
        valueId: String,
        cipherBytes: ByteArray,
    ) {
        driver.execute(null, "UPDATE field_value SET value_cipher = ? WHERE id = ?", 2) {
            bindBytes(0, cipherBytes)
            bindString(1, valueId)
        }
    }

    val entities get() = repositories.entities
    val fields get() = repositories.fields
    val relationships get() = repositories.relationships
    val search get() = repositories.search
}
