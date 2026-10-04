package io.github.behnooddev.voidmanager.shared.vault

/** What a [DeviceKeyProvider] returns after asking the user to authenticate. */
sealed interface DeviceKeyResult {
    /** The 32-byte key. The receiver overwrites [bytes] once it has used them. */
    class Key(
        val bytes: ByteArray,
    ) : DeviceKeyResult

    /** The user dismissed the prompt. Not an error. */
    data object Cancelled : DeviceKeyResult

    /** The platform key can no longer be used, for example because biometrics changed. Device unlock must be turned off. */
    data object Invalidated : DeviceKeyResult

    /** No stored key, no enrolled biometrics, or no screen to show the prompt on. */
    data object Unavailable : DeviceKeyResult

    data object Failed : DeviceKeyResult
}

/**
 * A key that the operating system releases only after the user authenticates (biometrics on
 * Android). The vault wraps its key under it, so unlocking does not need the password.
 * [createKey] and [releaseKey] show a system prompt and must be called from the main thread.
 */
interface DeviceKeyProvider {
    /** True when the device can authenticate with strong biometrics right now. */
    fun isAvailable(): Boolean

    /** True when a key from an earlier [createKey] is still stored. */
    fun hasKey(): Boolean

    /** Authenticates the user, then makes a new random key, stores it protected, and returns a copy. */
    suspend fun createKey(): DeviceKeyResult

    /** Authenticates the user and returns the stored key. */
    suspend fun releaseKey(): DeviceKeyResult

    /** Removes the stored key. Harmless when there is none. */
    fun deleteKey()
}

enum class DeviceUnlockChange { Enabled, Cancelled, Failed }
