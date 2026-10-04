package io.github.behnooddev.voidmanager.shared.vault

import io.github.behnooddev.voidmanager.core.data.OpenOutcome
import io.github.behnooddev.voidmanager.core.data.OpenedVault
import io.github.behnooddev.voidmanager.core.data.VaultGateway
import io.github.behnooddev.voidmanager.core.security.AutoLockController

/** Where the application is in the vault lifecycle. */
sealed interface Stage {
    /** No vault exists on this device yet. */
    data object NeedsSetup : Stage

    data object Locked : Stage

    data class Unlocked(
        val vault: OpenedVault,
    ) : Stage
}

/** Why the last create or unlock attempt did not open a vault. */
enum class Failure {
    WrongPassword,
    InvalidKeyFile,
    UnsupportedVersion,
    NewerData,
    StorageFailure,
}

/**
 * The lock state machine, without any UI or threading. [create] and [unlock] block while keys are
 * derived, so the caller runs them off the main thread; every other member is cheap.
 * Not thread safe: all calls come from one thread at a time.
 */
class VaultFlow(
    private val gateway: VaultGateway,
    private val autoLock: AutoLockController,
    private val throttle: AttemptThrottle,
) {
    var stage: Stage = if (gateway.exists()) Stage.Locked else Stage.NeedsSetup
        private set

    var failure: Failure? = null
        private set

    fun throttleRemainingMillis(): Long = throttle.remainingMillis()

    /** Does nothing unless the stage is [Stage.NeedsSetup]. The caller wipes [password]. */
    fun create(password: CharArray) {
        if (stage != Stage.NeedsSetup) return
        failure = null
        handle(gateway.create(password))
    }

    /** Does nothing unless the stage is [Stage.Locked] and no throttle wait is running. The caller wipes [password]. */
    fun unlock(password: CharArray) {
        if (stage != Stage.Locked) return
        if (throttle.remainingMillis() > 0) return
        failure = null
        handle(gateway.unlock(password))
    }

    fun lock() {
        val current = stage as? Stage.Unlocked ?: return
        stage = Stage.Locked
        autoLock.reset()
        current.vault.lock()
    }

    fun onInteraction() {
        if (stage is Stage.Unlocked) autoLock.onInteraction()
    }

    /** Locks at once when the policy allows no time in the background. */
    fun onBackgrounded() {
        if (stage !is Stage.Unlocked) return
        autoLock.onBackgrounded()
        if (autoLock.shouldLock()) lock()
    }

    fun onForegrounded() {
        if (stage is Stage.Unlocked && autoLock.onForegrounded()) lock()
    }

    /** Called on a timer while the application is in the foreground. */
    fun checkAutoLock() {
        if (stage is Stage.Unlocked && autoLock.shouldLock()) lock()
    }

    private fun handle(outcome: OpenOutcome) {
        when (outcome) {
            is OpenOutcome.Opened -> {
                stage = Stage.Unlocked(outcome.vault)
                throttle.recordSuccess()
                autoLock.reset()
            }
            OpenOutcome.NoVault -> stage = Stage.NeedsSetup
            OpenOutcome.AlreadyExists -> stage = Stage.Locked
            OpenOutcome.WrongPasswordOrCorrupt -> {
                failure = Failure.WrongPassword
                throttle.recordFailure()
            }
            OpenOutcome.InvalidKeyFile -> failure = Failure.InvalidKeyFile
            is OpenOutcome.UnsupportedVersion -> failure = Failure.UnsupportedVersion
            OpenOutcome.NewerData -> failure = Failure.NewerData
            OpenOutcome.StorageFailure -> failure = Failure.StorageFailure
        }
    }
}
