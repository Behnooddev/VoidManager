package io.github.behnooddev.voidmanager.core.database

import app.cash.sqldelight.db.SqlDriver

/** Creates the encrypted driver for one vault database file. Implemented per platform. */
interface DriverFactory {
    fun create(key: DatabaseKey): SqlDriver

    /**
     * Whether the file on disk is encrypted. Only meaningful after the first write.
     * A driver that ignores its key parameter creates a plain database without any error, so this
     * is checked after every open.
     */
    fun isEncryptedOnDisk(): Boolean
}
