package io.github.behnooddev.voidmanager.core.crypto

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AuthenticationFailedException(
    message: String,
    cause: Throwable? = null,
) : GeneralSecurityException(message, cause)

/**
 * AES-256-GCM through the platform provider.
 *
 * Sealed output layout: 12-byte random nonce, then ciphertext, then the 16-byte tag.
 * Nonces are random, so one key must not protect more than about 2^32 messages. The keys used
 * here are per vault and protect far fewer values than that.
 */
object AesGcm {
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 12
    const val TAG_BYTES = 16
    private const val TAG_BITS = TAG_BYTES * 8
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val secureRandom = SecureRandom()

    fun encrypt(
        key: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        val nonce = ByteArray(NONCE_BYTES)
        secureRandom.nextBytes(nonce)
        return nonce + encryptWithNonce(key, nonce, aad, plaintext)
    }

    fun decrypt(
        key: ByteArray,
        aad: ByteArray,
        sealed: ByteArray,
    ): ByteArray {
        if (sealed.size < NONCE_BYTES + TAG_BYTES) throw AuthenticationFailedException("The data is too short")
        val nonce = sealed.copyOfRange(0, NONCE_BYTES)
        val body = sealed.copyOfRange(NONCE_BYTES, sealed.size)
        return decryptWithNonce(key, nonce, aad, body)
    }

    /** Exposed for known-answer tests. Application code must use [encrypt], which picks a fresh nonce. */
    internal fun encryptWithNonce(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec(key), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext)
    }

    internal fun decryptWithNonce(
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
        body: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec(key), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        try {
            return cipher.doFinal(body)
        } catch (e: AEADBadTagException) {
            throw AuthenticationFailedException("Authentication failed", e)
        }
    }

    private fun keySpec(key: ByteArray): SecretKeySpec {
        require(key.size == KEY_BYTES) { "AES-256 needs a $KEY_BYTES-byte key" }
        return SecretKeySpec(key, "AES")
    }
}
