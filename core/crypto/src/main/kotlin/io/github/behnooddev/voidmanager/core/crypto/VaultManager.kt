package io.github.behnooddev.voidmanager.core.crypto

import java.nio.CharBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer

sealed interface UnlockResult {
    data class Unlocked(
        val session: VaultSession,
    ) : UnlockResult

    data object NoVault : UnlockResult

    /** The password is wrong, or the key file was modified. The two cannot be told apart. */
    data object WrongPasswordOrCorrupt : UnlockResult

    data object InvalidKeyFile : UnlockResult

    data class UnsupportedVersion(
        val version: Int,
    ) : UnlockResult

    /** Device unlock was requested but has not been enabled. */
    data object DeviceUnlockNotEnabled : UnlockResult
}

sealed interface ChangePasswordResult {
    data object Changed : ChangePasswordResult

    data object WrongCurrentPassword : ChangePasswordResult

    data object NoVault : ChangePasswordResult
}

/**
 * Creates, unlocks and re-keys a vault. All password-based work happens here; callers never see
 * the vault key, only a [VaultSession].
 *
 * Methods that derive keys take noticeable time (memory-hard hashing). Call them off the main thread.
 */
class VaultManager(
    private val store: KeyEnvelopeStore,
    private val kdfParams: KdfParams = KdfParams.DEFAULT,
    private val random: SecureRandom = SecureRandom(),
) {
    fun exists(): Boolean = store.read() != null

    fun create(password: CharArray): VaultSession {
        check(!exists()) { "A vault already exists" }
        require(password.isNotEmpty()) { "The password is empty" }
        val vaultKey = ByteArray(VaultKeys.KEY_BYTES).also { random.nextBytes(it) }
        val envelope = wrapWithPassword(vaultKey, password, deviceWrapped = null)
        store.write(envelope.toBytes())
        return VaultSession(vaultKey)
    }

    fun unlock(password: CharArray): UnlockResult {
        val envelope =
            when (val loaded = load()) {
                is Loaded.Ok -> loaded.envelope
                is Loaded.Failed -> return loaded.result
            }
        val vaultKey = unwrapWithPassword(envelope, password) ?: return UnlockResult.WrongPasswordOrCorrupt
        return UnlockResult.Unlocked(VaultSession(vaultKey))
    }

    /**
     * Replaces the password. The vault key does not change, so no stored data is re-encrypted.
     * The current password is checked again even though the vault is unlocked.
     */
    fun changePassword(
        session: VaultSession,
        current: CharArray,
        new: CharArray,
    ): ChangePasswordResult {
        require(new.isNotEmpty()) { "The new password is empty" }
        val envelope =
            when (val loaded = load()) {
                is Loaded.Ok -> loaded.envelope
                is Loaded.Failed -> return ChangePasswordResult.NoVault
            }
        val unwrapped = unwrapWithPassword(envelope, current) ?: return ChangePasswordResult.WrongCurrentPassword
        val sessionKey = session.vaultKeyCopy()
        try {
            require(MessageDigest.isEqual(unwrapped, sessionKey)) { "The session does not belong to this vault" }
            store.write(wrapWithPassword(unwrapped, new, envelope.deviceWrapped).toBytes())
        } finally {
            unwrapped.fill(0)
            sessionKey.fill(0)
        }
        return ChangePasswordResult.Changed
    }

    /** Stores a second wrapping of the vault key under [deviceKey], a 32-byte key held by the operating system. */
    fun enableDeviceUnlock(
        session: VaultSession,
        deviceKey: ByteArray,
    ) {
        val envelope = (load() as? Loaded.Ok)?.envelope ?: error("No vault")
        val vaultKey = session.vaultKeyCopy()
        try {
            val wrapped = AesGcm.encrypt(deviceKey, KeyEnvelope.deviceAad(), vaultKey)
            store.write(envelope.withDeviceWrap(wrapped).toBytes())
        } finally {
            vaultKey.fill(0)
        }
    }

    fun disableDeviceUnlock() {
        val envelope = (load() as? Loaded.Ok)?.envelope ?: return
        store.write(envelope.withDeviceWrap(null).toBytes())
    }

    fun unlockWithDeviceKey(deviceKey: ByteArray): UnlockResult {
        val envelope =
            when (val loaded = load()) {
                is Loaded.Ok -> loaded.envelope
                is Loaded.Failed -> return loaded.result
            }
        val wrapped = envelope.deviceWrapped ?: return UnlockResult.DeviceUnlockNotEnabled
        val vaultKey =
            try {
                AesGcm.decrypt(deviceKey, KeyEnvelope.deviceAad(), wrapped)
            } catch (e: AuthenticationFailedException) {
                return UnlockResult.WrongPasswordOrCorrupt
            }
        return UnlockResult.Unlocked(VaultSession(vaultKey))
    }

    private sealed interface Loaded {
        data class Ok(
            val envelope: KeyEnvelope,
        ) : Loaded

        data class Failed(
            val result: UnlockResult,
        ) : Loaded
    }

    private fun load(): Loaded {
        val bytes = store.read() ?: return Loaded.Failed(UnlockResult.NoVault)
        return try {
            Loaded.Ok(KeyEnvelope.parse(bytes))
        } catch (e: UnsupportedEnvelopeVersionException) {
            Loaded.Failed(UnlockResult.UnsupportedVersion(e.version))
        } catch (e: InvalidEnvelopeException) {
            Loaded.Failed(UnlockResult.InvalidKeyFile)
        }
    }

    private fun wrapWithPassword(
        vaultKey: ByteArray,
        password: CharArray,
        deviceWrapped: ByteArray?,
    ): KeyEnvelope {
        val salt = ByteArray(Argon2idKdf.SALT_BYTES).also { random.nextBytes(it) }
        val shell = KeyEnvelope(KeyEnvelope.CURRENT_VERSION, kdfParams, salt, ByteArray(0), deviceWrapped)
        val passwordBytes = passwordToBytes(password)
        val kek =
            try {
                Argon2idKdf.derive(passwordBytes, salt, kdfParams)
            } finally {
                passwordBytes.fill(0)
            }
        try {
            val wrapped = AesGcm.encrypt(kek, shell.passwordAad(), vaultKey)
            return shell.withPasswordWrap(kdfParams, salt, wrapped)
        } finally {
            kek.fill(0)
        }
    }

    private fun unwrapWithPassword(
        envelope: KeyEnvelope,
        password: CharArray,
    ): ByteArray? {
        val passwordBytes = passwordToBytes(password)
        val kek =
            try {
                Argon2idKdf.derive(passwordBytes, envelope.salt, envelope.kdf)
            } finally {
                passwordBytes.fill(0)
            }
        try {
            return AesGcm.decrypt(kek, envelope.passwordAad(), envelope.passwordWrapped)
        } catch (e: AuthenticationFailedException) {
            return null
        } finally {
            kek.fill(0)
        }
    }

    internal companion object {
        /**
         * NFKC normalization, so the same typed password derives the same key whichever Unicode
         * form the keyboard produced. The normalized text is a temporary String, which cannot be overwritten.
         */
        fun passwordToBytes(password: CharArray): ByteArray {
            val normalized = Normalizer.normalize(CharBuffer.wrap(password), Normalizer.Form.NFKC)
            val buffer = Charsets.UTF_8.encode(normalized)
            val out = ByteArray(buffer.remaining())
            buffer.get(out)
            if (buffer.hasArray()) buffer.array().fill(0)
            return out
        }
    }
}

private fun passwordToBytes(password: CharArray): ByteArray = VaultManager.passwordToBytes(password)
