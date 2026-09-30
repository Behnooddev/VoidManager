package io.github.behnooddev.voidmanager.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Android driver on SQLCipher. The open helper never creates or migrates tables; that is done by
 * [VaultDatabaseOpener], which keeps its own schema version, so the helper version stays fixed.
 */
class AndroidDriverFactory(
    private val context: Context,
    private val name: String,
) : DriverFactory {
    override fun create(key: DatabaseKey): SqlDriver {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(key.toSqlCipherRawKey().encodeToByteArray())
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(name)
                .callback(NoSchemaCallback)
                .build()
        return AndroidSqliteDriver(factory.create(configuration))
    }

    override fun isEncryptedOnDisk(): Boolean {
        val file: File = context.getDatabasePath(name)
        if (!file.exists()) return false
        val header = ByteArray(HEADER_BYTES)
        val read = file.inputStream().use { it.read(header) }
        return read == HEADER_BYTES && EncryptionCheck.looksEncrypted(header)
    }

    private object NoSchemaCallback : SupportSQLiteOpenHelper.Callback(HELPER_VERSION) {
        override fun onConfigure(db: SupportSQLiteDatabase) {
            db.setForeignKeyConstraintsEnabled(true)
        }

        override fun onCreate(db: SupportSQLiteDatabase) = Unit

        override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ) = Unit
    }

    private companion object {
        const val HELPER_VERSION = 1
        const val HEADER_BYTES = 16
    }
}
