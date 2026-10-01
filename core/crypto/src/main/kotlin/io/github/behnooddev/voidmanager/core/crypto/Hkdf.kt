package io.github.behnooddev.voidmanager.core.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

/** HKDF with SHA-256 (RFC 5869), from Bouncy Castle. */
object Hkdf {
    private const val MAX_LENGTH = 255 * 32

    fun derive(
        ikm: ByteArray,
        salt: ByteArray?,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        require(length in 1..MAX_LENGTH) { "Invalid output length" }
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(HKDFParameters(ikm, salt, info))
        val out = ByteArray(length)
        generator.generateBytes(out, 0, length)
        return out
    }
}
