package io.github.behnooddev.voidmanager.shared.vault

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Connects [VaultFlow] to the screens: it publishes the flow's state as Compose state and runs the
 * slow key derivation off the main thread. Create and unlock keep running when the screen that
 * started them leaves, so a vault that opens is always either handed to the state or closed.
 */
class VaultController(
    private val flow: VaultFlow,
    private val worker: CoroutineContext = Dispatchers.Default,
) {
    var stage: Stage by mutableStateOf(flow.stage)
        private set

    var failure: Failure? by mutableStateOf(flow.failure)
        private set

    /** True while a key derivation is running. */
    var busy: Boolean by mutableStateOf(false)
        private set

    /** Creates the vault and opens it. [password] is overwritten before this returns. */
    suspend fun create(password: CharArray) {
        run(password) { flow.create(it) }
    }

    /** Opens the vault. [password] is overwritten before this returns. */
    suspend fun unlock(password: CharArray) {
        run(password) { flow.unlock(it) }
    }

    /** True when the unlock screen should offer biometric unlock. */
    fun deviceUnlockReady(keys: DeviceKeyProvider): Boolean =
        keys.isAvailable() && keys.hasKey() && flow.deviceUnlockEnabled()

    /** Asks the user to authenticate, then opens the vault with the released key. */
    suspend fun unlockWithDevice(keys: DeviceKeyProvider) {
        if (busy || stage != Stage.Locked) return
        busy = true
        try {
            when (val result = keys.releaseKey()) {
                is DeviceKeyResult.Key ->
                    try {
                        withContext(worker + NonCancellable) { flow.unlockWithDeviceKey(result.bytes) }
                    } finally {
                        result.bytes.fill(0)
                    }
                DeviceKeyResult.Cancelled -> Unit
                DeviceKeyResult.Invalidated -> {
                    flow.deviceKeyInvalidated()
                    keys.deleteKey()
                }
                DeviceKeyResult.Unavailable, DeviceKeyResult.Failed -> flow.deviceUnlockFailed()
            }
        } finally {
            busy = false
            publish()
        }
    }

    /** Turns biometric unlock on. The vault must be unlocked. */
    suspend fun enableDeviceUnlock(keys: DeviceKeyProvider): DeviceUnlockChange {
        if (busy || stage !is Stage.Unlocked) return DeviceUnlockChange.Failed
        busy = true
        try {
            return when (val result = keys.createKey()) {
                is DeviceKeyResult.Key -> {
                    val enabled =
                        try {
                            withContext(worker + NonCancellable) { flow.enableDeviceUnlock(result.bytes) }
                        } finally {
                            result.bytes.fill(0)
                        }
                    if (enabled) {
                        DeviceUnlockChange.Enabled
                    } else {
                        keys.deleteKey()
                        DeviceUnlockChange.Failed
                    }
                }
                DeviceKeyResult.Cancelled -> DeviceUnlockChange.Cancelled
                else -> DeviceUnlockChange.Failed
            }
        } finally {
            busy = false
            publish()
        }
    }

    fun disableDeviceUnlock(keys: DeviceKeyProvider) {
        flow.disableDeviceUnlock()
        keys.deleteKey()
    }

    fun lock() {
        flow.lock()
        publish()
    }

    fun throttleRemainingMillis(): Long = flow.throttleRemainingMillis()

    fun onInteraction() = flow.onInteraction()

    fun onBackgrounded() {
        flow.onBackgrounded()
        publish()
    }

    fun onForegrounded() {
        flow.onForegrounded()
        publish()
    }

    fun checkAutoLock() {
        flow.checkAutoLock()
        publish()
    }

    private suspend fun run(
        password: CharArray,
        action: (CharArray) -> Unit,
    ) {
        if (busy) {
            password.fill(NUL)
            return
        }
        busy = true
        try {
            withContext(worker + NonCancellable) { action(password) }
        } finally {
            password.fill(NUL)
            busy = false
            publish()
        }
    }

    private fun publish() {
        stage = flow.stage
        failure = flow.failure
    }

    private companion object {
        const val NUL = '\u0000'
    }
}
