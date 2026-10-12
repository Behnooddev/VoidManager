package io.github.behnooddev.voidmanager.core.backup

import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** HKDF with SHA-256 (RFC 5869). */
internal object Hkdf {
    private const val HASH_BYTES = 32
    private const val MAX_BLOCKS = 255

    fun derive(
        ikm: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        require(length in 1..(MAX_BLOCKS * HASH_BYTES)) { "HKDF length out of range" }
        val pseudoRandomKey = hmac(salt, ikm)
        val out = ByteArray(length)
        var block = ByteArray(0)
        var written = 0
        var counter = 1
        while (written < length) {
            block = hmac(pseudoRandomKey, block + info + byteArrayOf(counter.toByte()))
            val take = minOf(block.size, length - written)
            block.copyInto(out, written, 0, take)
            written += take
            counter++
        }
        pseudoRandomKey.fill(0)
        return out
    }

    private fun hmac(
        key: ByteArray,
        data: ByteArray,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (key.isEmpty()) ByteArray(HASH_BYTES) else key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}

/**
 * Chunked authenticated encryption (the STREAM construction) with AES-256-GCM. Each chunk has its
 * own nonce made of a fixed prefix, a counter and a flag that marks the last chunk, and the whole
 * header is authenticated with every chunk. Dropping, reordering, repeating or adding chunks, and
 * cutting the end off, all make decryption fail.
 */
internal object StreamCipher {
    const val TAG_BYTES = 16
    const val NONCE_PREFIX_BYTES = 7
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val MAX_SEGMENTS = 1L shl 31

    fun encrypt(
        key: ByteArray,
        noncePrefix: ByteArray,
        aad: ByteArray,
        plain: ByteArray,
        chunkSize: Int,
    ): ByteArray {
        require(noncePrefix.size == NONCE_PREFIX_BYTES) { "Bad nonce prefix" }
        val segments = if (plain.isEmpty()) 1 else (plain.size + chunkSize - 1) / chunkSize
        require(segments.toLong() < MAX_SEGMENTS) { "Payload too large" }
        val out = ByteArray(plain.size + segments * TAG_BYTES)
        var written = 0
        for (index in 0 until segments) {
            val from = index * chunkSize
            val to = minOf(from + chunkSize, plain.size)
            val cipher =
                newCipher(Cipher.ENCRYPT_MODE, key, nonce(noncePrefix, index, last = index == segments - 1), aad)
            written += cipher.doFinal(plain, from, to - from, out, written)
        }
        return out
    }

    /** Returns null when any chunk fails authentication or the chunk layout is not one this writer produces. */
    fun decrypt(
        key: ByteArray,
        noncePrefix: ByteArray,
        aad: ByteArray,
        data: ByteArray,
        chunkSize: Int,
    ): ByteArray? {
        val segmentSize = chunkSize + TAG_BYTES
        if (data.size < TAG_BYTES) return null
        val segments = (data.size + segmentSize - 1) / segmentSize
        if (segments.toLong() >= MAX_SEGMENTS) return null
        val lastSize = data.size - (segments - 1) * segmentSize
        // The writer never ends with an empty chunk unless the whole payload is empty.
        if (lastSize == TAG_BYTES && segments > 1) return null
        val out = ByteArray(data.size - segments * TAG_BYTES)
        var written = 0
        try {
            for (index in 0 until segments) {
                val from = index * segmentSize
                val length = minOf(segmentSize, data.size - from)
                val cipher =
                    newCipher(Cipher.DECRYPT_MODE, key, nonce(noncePrefix, index, last = index == segments - 1), aad)
                written += cipher.doFinal(data, from, length, out, written)
            }
        } catch (e: AEADBadTagException) {
            return null
        } catch (e: GeneralSecurityException) {
            return null
        }
        return out
    }

    /** One AES-GCM message with a fixed all-zero nonce. Only for a key that is used exactly once. */
    fun sealOnce(
        key: ByteArray,
        aad: ByteArray,
        plain: ByteArray,
    ): ByteArray = newCipher(Cipher.ENCRYPT_MODE, key, ByteArray(NONCE_BYTES), aad).doFinal(plain)

    fun openOnce(
        key: ByteArray,
        aad: ByteArray,
        sealed: ByteArray,
    ): ByteArray? =
        try {
            newCipher(Cipher.DECRYPT_MODE, key, ByteArray(NONCE_BYTES), aad).doFinal(sealed)
        } catch (e: GeneralSecurityException) {
            null
        }

    private fun nonce(
        prefix: ByteArray,
        index: Int,
        last: Boolean,
    ): ByteArray {
        val nonce = ByteArray(NONCE_BYTES)
        prefix.copyInto(nonce)
        nonce[NONCE_PREFIX_BYTES] = (index ushr 24).toByte()
        nonce[NONCE_PREFIX_BYTES + 1] = (index ushr 16).toByte()
        nonce[NONCE_PREFIX_BYTES + 2] = (index ushr 8).toByte()
        nonce[NONCE_PREFIX_BYTES + 3] = index.toByte()
        nonce[NONCE_BYTES - 1] = if (last) 1 else 0
        return nonce
    }

    private fun newCipher(
        mode: Int,
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
    ): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher
    }
}
