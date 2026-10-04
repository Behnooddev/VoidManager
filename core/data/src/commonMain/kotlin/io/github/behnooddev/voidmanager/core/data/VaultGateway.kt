package io.github.behnooddev.voidmanager.core.data

/**
 * What the application layer needs from a vault. The gateway hands out results and repositories,
 * never key material. Implementations live next to the cryptography.
 *
 * [create] and [unlock] derive keys with a memory-hard function and take noticeable time.
 * Call them off the main thread.
 */
interface VaultGateway {
    fun exists(): Boolean

    fun create(password: CharArray): OpenOutcome

    fun unlock(password: CharArray): OpenOutcome

    /** True when the key file holds a device-key slot. Says nothing about whether the platform still holds the key. */
    fun deviceUnlockEnabled(): Boolean

    /**
     * Opens the vault with the 32-byte key that the platform released after the user authenticated.
     * The caller overwrites [deviceKey] afterwards.
     */
    fun unlockWithDeviceKey(deviceKey: ByteArray): OpenOutcome

    /** Removes the device-key slot. The password keeps working. */
    fun disableDeviceUnlock()
}

/** An open vault. After [lock] the repositories must not be used again. */
interface OpenedVault {
    val repositories: VaultRepositories

    /**
     * Adds a device-key slot wrapped with [deviceKey], a 32-byte key the platform holds behind user
     * authentication. Returns false when the key file could not be written. The caller overwrites [deviceKey].
     */
    fun enableDeviceUnlock(deviceKey: ByteArray): Boolean

    /** Closes the database and wipes the keys held in memory. Calling it twice is harmless. */
    fun lock()
}

sealed interface OpenOutcome {
    data class Opened(
        val vault: OpenedVault,
    ) : OpenOutcome

    data object NoVault : OpenOutcome

    /** A vault is already present, so a new one was not created. */
    data object AlreadyExists : OpenOutcome

    /** The password is wrong, or the key file was modified. The two cannot be told apart. */
    data object WrongPasswordOrCorrupt : OpenOutcome

    /** The device key does not open the vault: it is not the key that was enrolled, or the key file was modified. */
    data object DeviceKeyRejected : OpenOutcome

    data object DeviceUnlockNotEnabled : OpenOutcome

    data object InvalidKeyFile : OpenOutcome

    data class UnsupportedVersion(
        val version: Int,
    ) : OpenOutcome

    /** The database was written by a newer version of the application and is left untouched. */
    data object NewerData : OpenOutcome

    /** The key file or the database could not be written or read. */
    data object StorageFailure : OpenOutcome
}
