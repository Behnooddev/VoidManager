package io.github.behnooddev.voidmanager.core.crypto

import app.cash.sqldelight.db.SqlDriver
import io.github.behnooddev.voidmanager.core.data.OpenOutcome
import io.github.behnooddev.voidmanager.core.data.OpenedVault
import io.github.behnooddev.voidmanager.core.data.VaultGateway
import io.github.behnooddev.voidmanager.core.data.VaultRepositories
import io.github.behnooddev.voidmanager.core.database.DriverFactory
import io.github.behnooddev.voidmanager.core.database.OpenResult
import io.github.behnooddev.voidmanager.core.database.VaultDatabaseOpener
import io.github.behnooddev.voidmanager.core.model.IdGenerator
import io.github.behnooddev.voidmanager.core.model.UuidV7Generator
import java.io.IOException

/**
 * Joins the key hierarchy to the encrypted database: a correct password produces an open vault,
 * anything else produces a result that names the reason. The vault key never leaves this class
 * except inside the [VaultSession] it hands to the opened vault.
 */
class VaultService(
    private val manager: VaultManager,
    private val driverFactory: DriverFactory,
    private val ids: IdGenerator = UuidV7Generator(clock = { System.currentTimeMillis() }),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val opener: VaultDatabaseOpener = VaultDatabaseOpener(),
    /** Picks the key derivation cost for a new vault. Null uses the manager's configured cost. */
    private val calibrate: (() -> KdfParams)? = null,
) : VaultGateway {
    override fun exists(): Boolean = manager.exists()

    override fun create(password: CharArray): OpenOutcome {
        if (manager.exists()) return OpenOutcome.AlreadyExists
        val session =
            try {
                if (calibrate == null) manager.create(password) else manager.create(password, calibrate.invoke())
            } catch (e: IOException) {
                return OpenOutcome.StorageFailure
            }
        return open(session)
    }

    override fun unlock(password: CharArray): OpenOutcome =
        when (val result = manager.unlock(password)) {
            is UnlockResult.Unlocked -> open(result.session)
            UnlockResult.NoVault -> OpenOutcome.NoVault
            UnlockResult.WrongPasswordOrCorrupt -> OpenOutcome.WrongPasswordOrCorrupt
            UnlockResult.InvalidKeyFile -> OpenOutcome.InvalidKeyFile
            is UnlockResult.UnsupportedVersion -> OpenOutcome.UnsupportedVersion(result.version)
            // A password unlock never asks for the device slot; if it is reported anyway, nothing was opened.
            UnlockResult.DeviceUnlockNotEnabled -> OpenOutcome.WrongPasswordOrCorrupt
        }

    override fun deviceUnlockEnabled(): Boolean = manager.deviceUnlockEnabled()

    override fun unlockWithDeviceKey(deviceKey: ByteArray): OpenOutcome =
        when (val result = manager.unlockWithDeviceKey(deviceKey)) {
            is UnlockResult.Unlocked -> open(result.session)
            UnlockResult.NoVault -> OpenOutcome.NoVault
            UnlockResult.WrongPasswordOrCorrupt -> OpenOutcome.DeviceKeyRejected
            UnlockResult.InvalidKeyFile -> OpenOutcome.InvalidKeyFile
            is UnlockResult.UnsupportedVersion -> OpenOutcome.UnsupportedVersion(result.version)
            UnlockResult.DeviceUnlockNotEnabled -> OpenOutcome.DeviceUnlockNotEnabled
        }

    override fun disableDeviceUnlock() {
        try {
            manager.disableDeviceUnlock()
        } catch (e: IOException) {
            // The slot stays; the platform key is deleted by the caller, so the slot cannot be opened again.
        }
    }

    private fun open(session: VaultSession): OpenOutcome {
        val opened =
            try {
                opener.open(driverFactory, session.databaseKey())
            } catch (e: Exception) {
                session.lock()
                return OpenOutcome.StorageFailure
            }
        return when (opened) {
            is OpenResult.RefusedNewerSchema -> {
                session.lock()
                OpenOutcome.NewerData
            }
            is OpenResult.Opened -> assemble(session, opened)
        }
    }

    private fun assemble(
        session: VaultSession,
        opened: OpenResult.Opened,
    ): OpenOutcome =
        try {
            val repositories = VaultRepositories(opened.database, session.valueCipher(), ids, clock)
            OpenOutcome.Opened(SqlOpenedVault(session, opened.driver, manager, repositories))
        } catch (e: Exception) {
            closeQuietly(opened.driver)
            session.lock()
            OpenOutcome.StorageFailure
        }
}

private fun closeQuietly(driver: SqlDriver) {
    try {
        driver.close()
    } catch (e: Exception) {
        // The key is wiped by the caller whether or not the driver closes cleanly.
    }
}

private class SqlOpenedVault(
    private val session: VaultSession,
    private val driver: SqlDriver,
    private val manager: VaultManager,
    override val repositories: VaultRepositories,
) : OpenedVault {
    private var closed = false

    override fun enableDeviceUnlock(deviceKey: ByteArray): Boolean =
        try {
            manager.enableDeviceUnlock(session, deviceKey)
            true
        } catch (e: IOException) {
            false
        }

    override fun lock() {
        synchronized(this) {
            if (closed) return
            closed = true
        }
        try {
            closeQuietly(driver)
        } finally {
            session.lock()
        }
    }
}
