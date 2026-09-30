package io.github.behnooddev.voidmanager.core.database

/**
 * A database that was opened with a key parameter the driver ignores is silently created unencrypted.
 * After the first write, the file header is inspected: an encrypted database never starts with the
 * plain SQLite header.
 */
object EncryptionCheck {
    private val PLAIN_HEADER = "SQLite format 3".encodeToByteArray()

    fun looksEncrypted(header: ByteArray): Boolean {
        if (header.size < PLAIN_HEADER.size) return false
        for (i in PLAIN_HEADER.indices) {
            if (header[i] != PLAIN_HEADER[i]) return true
        }
        return false
    }
}
