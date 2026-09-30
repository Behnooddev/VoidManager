package io.github.behnooddev.voidmanager.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

/**
 * Desktop driver. It relies on the SQLCipher-enabled SQLite JDBC fork, which reads the connection
 * properties below. A property the fork does not know is ignored without an error and produces a
 * plain database, which is why [isEncryptedOnDisk] exists.
 */
class JvmDriverFactory(
    private val file: File,
) : DriverFactory {
    override fun create(key: DatabaseKey): SqlDriver {
        val properties =
            Properties().apply {
                setProperty("cipher", "sqlcipher")
                setProperty("legacy", "4")
                setProperty("key", key.toSqlCipherRawKey())
                setProperty("foreign_keys", "true")
            }
        return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", properties)
    }

    override fun isEncryptedOnDisk(): Boolean {
        if (!file.exists()) return false
        val header = ByteArray(HEADER_BYTES)
        val read = file.inputStream().use { it.read(header) }
        return read == HEADER_BYTES && EncryptionCheck.looksEncrypted(header)
    }

    private companion object {
        const val HEADER_BYTES = 16
    }
}
