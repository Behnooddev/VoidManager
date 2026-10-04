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
}

/** An open vault. After [lock] the repositories must not be used again. */
interface OpenedVault {
    val repositories: VaultRepositories

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

    data object InvalidKeyFile : OpenOutcome

    data class UnsupportedVersion(
        val version: Int,
    ) : OpenOutcome

    /** The database was written by a newer version of the application and is left untouched. */
    data object NewerData : OpenOutcome

    /** The key file or the database could not be written or read. */
    data object StorageFailure : OpenOutcome
}
