package io.github.behnooddev.voidmanager.core.backup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreamCipherTest {
    private val key = ByteArray(32) { it.toByte() }
    private val prefix = ByteArray(7) { (it + 100).toByte() }
    private val aad = "header".toByteArray()
    private val chunk = 16

    private fun plain(size: Int) = ByteArray(size) { (it * 7 + 3).toByte() }

    private fun encrypt(size: Int) = StreamCipher.encrypt(key, prefix, aad, plain(size), chunk)

    private fun decrypt(
        data: ByteArray,
        key: ByteArray = this.key,
        prefix: ByteArray = this.prefix,
        aad: ByteArray = this.aad,
    ) = StreamCipher.decrypt(key, prefix, aad, data, chunk)

    @Test
    fun everySizeAroundTheChunkBoundariesRoundTrips() {
        for (size in listOf(0, 1, 15, 16, 17, 31, 32, 33, 47, 48, 49, 100, 1000)) {
            assertContentEquals(plain(size), decrypt(encrypt(size)), "size $size")
        }
    }

    @Test
    fun theCiphertextAddsOneTagPerChunk() {
        assertEquals(0 + 16, encrypt(0).size)
        assertEquals(16 + 16, encrypt(16).size)
        assertEquals(17 + 2 * 16, encrypt(17).size)
        assertEquals(48 + 3 * 16, encrypt(48).size)
    }

    @Test
    fun cuttingTheEndOffAtAChunkBoundaryIsDetected() {
        val data = encrypt(48)
        val segment = chunk + StreamCipher.TAG_BYTES
        assertNull(decrypt(data.copyOf(data.size - segment)), "the last chunk is missing")
        assertNull(decrypt(data.copyOf(segment)), "only the first chunk is left")
    }

    @Test
    fun cuttingInsideAChunkOrAddingBytesIsDetected() {
        val data = encrypt(40)
        assertNull(decrypt(data.copyOf(data.size - 1)))
        assertNull(decrypt(data + byteArrayOf(0)))
        assertNull(decrypt(data + ByteArray(32)))
        assertNull(decrypt(ByteArray(0)))
        assertNull(decrypt(ByteArray(15)))
    }

    @Test
    fun reorderingOrRepeatingChunksIsDetected() {
        val data = encrypt(48)
        val segment = chunk + StreamCipher.TAG_BYTES
        val first = data.copyOfRange(0, segment)
        val second = data.copyOfRange(segment, 2 * segment)
        val third = data.copyOfRange(2 * segment, 3 * segment)
        assertNull(decrypt(second + first + third))
        assertNull(decrypt(first + first + third))
        assertNull(decrypt(first + second + second))
    }

    @Test
    fun flippingAnyBitFailsAuthentication() {
        val data = encrypt(40)
        for (index in data.indices step 5) {
            val changed = data.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertNull(decrypt(changed), "byte $index")
        }
    }

    @Test
    fun aWrongKeyPrefixOrAssociatedDataFails() {
        val data = encrypt(40)
        assertNull(decrypt(data, key = ByteArray(32)))
        assertNull(decrypt(data, prefix = ByteArray(7)))
        assertNull(decrypt(data, aad = "other".toByteArray()))
    }

    @Test
    fun anEmptyTrailingChunkIsRefusedBecauseTheWriterNeverMakesOne() {
        val full = encrypt(32)
        val emptyFinal = StreamCipher.encrypt(key, prefix, aad, ByteArray(0), chunk)
        assertNull(decrypt(full + emptyFinal))
    }

    @Test
    fun theOneShotCheckSealsAndOpens() {
        val sealed = StreamCipher.sealOnce(key, aad, "ok".toByteArray())
        assertContentEquals("ok".toByteArray(), StreamCipher.openOnce(key, aad, sealed))
        assertNull(StreamCipher.openOnce(ByteArray(32), aad, sealed))
        assertNull(StreamCipher.openOnce(key, "x".toByteArray(), sealed))
    }

    @Test
    fun hkdfMatchesTheRfc5869TestVector() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = ByteArray(13) { it.toByte() }
        val info = ByteArray(10) { (0xf0 + it).toByte() }
        val okm = Hkdf.derive(ikm, salt, info, 42)
        val expected = "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"
        assertEquals(expected, okm.joinToString("") { "%02x".format(it) })
        assertTrue(okm.size == 42)
    }
}
