package io.github.behnooddev.voidmanager.android

import android.app.Application
import android.os.SystemClock
import io.github.behnooddev.voidmanager.core.crypto.FileKeyEnvelopeStore
import io.github.behnooddev.voidmanager.core.crypto.KdfCalibrator
import io.github.behnooddev.voidmanager.core.crypto.VaultManager
import io.github.behnooddev.voidmanager.core.crypto.VaultService
import io.github.behnooddev.voidmanager.core.database.AndroidDriverFactory
import io.github.behnooddev.voidmanager.core.security.AutoLockController
import io.github.behnooddev.voidmanager.core.security.AutoLockPolicy
import io.github.behnooddev.voidmanager.shared.vault.AttemptThrottle
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import io.github.behnooddev.voidmanager.shared.vault.VaultFlow
import java.io.File

/**
 * Owns the vault controller so that it outlives the activity: rotating the screen recreates the
 * activity but must not lock the vault. The process ending, for any reason, does.
 */
class VoidManagerApp : Application() {
    lateinit var controller: VaultController
        private set

    override fun onCreate() {
        super.onCreate()
        val manager = VaultManager(FileKeyEnvelopeStore(File(filesDir, KEY_FILE_NAME)))
        val gateway =
            VaultService(
                manager = manager,
                driverFactory = AndroidDriverFactory(this, DATABASE_NAME),
                // Times one real derivation when a vault is created and fits the iteration count to this device.
                calibrate = { KdfCalibrator.calibrate(KdfCalibrator::measureArgon2) },
            )
        // elapsedRealtime keeps counting while the device sleeps, so a long sleep cannot extend a session.
        val clock = { SystemClock.elapsedRealtime() }
        val autoLock = AutoLockController(policy = { AutoLockPolicy() }, monotonicMillis = clock)
        controller = VaultController(VaultFlow(gateway, autoLock, AttemptThrottle(clock)))
    }

    private companion object {
        const val KEY_FILE_NAME = "vault.keys"
        const val DATABASE_NAME = "vault.db"
    }
}
