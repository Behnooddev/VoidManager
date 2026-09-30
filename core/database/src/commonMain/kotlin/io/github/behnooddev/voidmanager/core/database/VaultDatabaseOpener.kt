package io.github.behnooddev.voidmanager.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

sealed interface OpenResult {
    data class Opened(
        val database: VoidManagerDatabase,
        val driver: SqlDriver,
    ) : OpenResult

    /** The vault was written by a newer version of the application and is left untouched. */
    data class RefusedNewerSchema(
        val stored: Long,
        val supported: Long,
    ) : OpenResult
}

/** Raised when the database file is not encrypted although a key was supplied. */
class UnencryptedDatabaseException : IllegalStateException("The database file is not encrypted")

/**
 * Opens a vault: creates the schema, migrates an older one, or refuses a newer one.
 * The schema version is stored in `vault_meta`, not in `PRAGMA user_version`, so the same logic
 * works on every platform driver.
 */
class VaultDatabaseOpener(
    private val schema: SqlSchema<QueryResult.Value<Unit>> = VoidManagerDatabase.Schema,
    /** Called before an older schema is migrated. Platform code copies the database file here. */
    private val beforeMigrate: (from: Long, to: Long) -> Unit = { _, _ -> },
) {
    fun open(
        factory: DriverFactory,
        key: DatabaseKey,
    ): OpenResult {
        val driver = factory.create(key)
        try {
            val result = prepare(driver)
            if (result is OpenResult.RefusedNewerSchema) {
                driver.close()
                return result
            }
            if (!factory.isEncryptedOnDisk()) throw UnencryptedDatabaseException()
            return result
        } catch (e: Throwable) {
            driver.close()
            throw e
        }
    }

    private fun prepare(driver: SqlDriver): OpenResult {
        val database = VoidManagerDatabase(driver)
        val stored = readStoredVersion(driver, database)
        when (val plan = MigrationPlanner.plan(stored, schema.version)) {
            OpenPlan.Create ->
                database.transaction {
                    schema.create(driver).value
                    database.miscQueries.putMeta(SCHEMA_VERSION_KEY, schema.version.toString())
                }
            OpenPlan.UpToDate -> Unit
            is OpenPlan.Migrate -> {
                beforeMigrate(plan.from, plan.to)
                database.transaction {
                    schema.migrate(driver, plan.from, plan.to).value
                    database.miscQueries.putMeta(SCHEMA_VERSION_KEY, plan.to.toString())
                }
            }
            is OpenPlan.RefuseNewer -> return OpenResult.RefusedNewerSchema(plan.stored, plan.supported)
        }
        return OpenResult.Opened(database, driver)
    }

    private fun readStoredVersion(
        driver: SqlDriver,
        database: VoidManagerDatabase,
    ): Long? {
        val hasMeta =
            driver
                .executeQuery(
                    identifier = null,
                    sql = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'vault_meta'",
                    mapper = { cursor -> QueryResult.Value(cursor.next().value) },
                    parameters = 0,
                ).value
        if (!hasMeta) return null
        return database.miscQueries
            .getMeta(SCHEMA_VERSION_KEY)
            .executeAsOneOrNull()
            ?.toLongOrNull()
    }

    companion object {
        const val SCHEMA_VERSION_KEY = "schema_version"
    }
}
