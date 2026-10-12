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
import io.github.behnooddev.voidmanager.shared.secrets.SecretClipboard
import io.github.behnooddev.voidmanager.shared.settings.AppSettingsHolder
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

    lateinit var settings: AppSettingsHolder
        private set

    lateinit var clipboard: SecretClipboard
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
        settings = AppSettingsHolder(SharedPreferencesSettingsStore(this))
        clipboard = AndroidSecretClipboard(this)
        // The idle time comes from the settings, read each time so a change applies at once.
        val autoLock =
            AutoLockController(
                policy = { AutoLockPolicy(idleTimeoutMillis = settings.current.idleLockMillis) },
                monotonicMillis = clock,
            )
        controller = VaultController(VaultFlow(gateway, autoLock, AttemptThrottle(clock)))
    }

    private companion object {
        const val KEY_FILE_NAME = "vault.keys"
        const val DATABASE_NAME = "vault.db"
    }
}
