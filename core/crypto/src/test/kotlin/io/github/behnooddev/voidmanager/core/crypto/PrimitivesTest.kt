package io.github.behnooddev.voidmanager.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

private fun hex(text: String): ByteArray =
    text
        .replace(" ", "")
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

class PrimitivesTest {
    @Test
    fun argon2idMatchesTheRfc9106TestVector() {
        val tag =
            Argon2idKdf.deriveUnchecked(
                password = ByteArray(32) { 1 },
                salt = ByteArray(16) { 2 },
                memoryKiB = 32,
                iterations = 3,
                parallelism = 4,
                secret = ByteArray(8) { 3 },
                additional = ByteArray(12) { 4 },
                outputBytes = 32,
            )

        assertEquals("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659", tag.toHex())
    }

    @Test
    fun argon2idIsDeterministicAndDependsOnPasswordAndSalt() {
        val salt = ByteArray(16) { it.toByte() }
        val params = KdfParams.FLOOR

        val a = Argon2idKdf.derive("one".encodeToByteArray(), salt, params)
        val b = Argon2idKdf.derive("one".encodeToByteArray(), salt, params)
        val otherPassword = Argon2idKdf.derive("two".encodeToByteArray(), salt, params)
        val otherSalt = Argon2idKdf.derive("one".encodeToByteArray(), ByteArray(16) { 9 }, params)

        assertContentEquals(a, b)
        assertNotEquals(a.toHex(), otherPassword.toHex())
        assertNotEquals(a.toHex(), otherSalt.toHex())
        assertEquals(32, a.size)
    }

    @Test
    fun kdfParametersBelowTheFloorOrAboveTheCeilingAreRejected() {
        assertFailsWith<IllegalArgumentException> { KdfParams(memoryKiB = 1024, iterations = 3, parallelism = 1) }
        assertFailsWith<IllegalArgumentException> { KdfParams(memoryKiB = 65536, iterations = 1, parallelism = 1) }
        assertFailsWith<IllegalArgumentException> {
            KdfParams(
                memoryKiB = 4 * 1024 * 1024,
                iterations = 3,
                parallelism = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> { KdfParams(memoryKiB = 65536, iterations = 3, parallelism = 0) }
    }

    @Test
    fun hkdfMatchesRfc5869TestCase1() {
        val okm =
            Hkdf.derive(
                ikm = ByteArray(22) { 0x0b },
                salt = hex("000102030405060708090a0b0c"),
                info = hex("f0f1f2f3f4f5f6f7f8f9"),
                length = 42,
            )

        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
            okm.toHex(),
        )
    }

    @Test
    fun hkdfMatchesRfc5869TestCase3WithoutSaltOrInfo() {
        val okm = Hkdf.derive(ikm = ByteArray(22) { 0x0b }, salt = null, info = ByteArray(0), length = 42)

        assertEquals(
            "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8",
            okm.toHex(),
        )
    }

    @Test
    fun aesGcmMatchesPublishedTestCase13() {
        val sealed = AesGcm.encryptWithNonce(ByteArray(32), ByteArray(12), ByteArray(0), ByteArray(0))

        assertEquals("530f8afbc74536b9a963b4f1c4cb738b", sealed.toHex())
    }

    @Test
    fun aesGcmMatchesPublishedTestCase14() {
        val sealed = AesGcm.encryptWithNonce(ByteArray(32), ByteArray(12), ByteArray(0), ByteArray(16))

        assertEquals("cea7403d4d606b6e074ec5d3baf39d18" + "d0d1c8a799996bf0265b98b5d48ab919", sealed.toHex())
    }

    @Test
    fun aesGcmRoundTripsIncludingEmptyPlaintext() {
        val key = ByteArray(32) { it.toByte() }
        val aad = "ctx".encodeToByteArray()

        assertEquals(
            "synthetic text",
            AesGcm.decrypt(key, aad, AesGcm.encrypt(key, aad, "synthetic text".encodeToByteArray())).decodeToString(),
        )
        assertEquals(0, AesGcm.decrypt(key, aad, AesGcm.encrypt(key, aad, ByteArray(0))).size)
    }

    @Test
    fun aesGcmUsesAFreshNonceEveryTime() {
        val key = ByteArray(32) { 5 }
        val a = AesGcm.encrypt(key, ByteArray(0), "same".encodeToByteArray())
        val b = AesGcm.encrypt(key, ByteArray(0), "same".encodeToByteArray())

        assertNotEquals(a.toHex(), b.toHex())
        assertFalse(a.copyOfRange(0, 12).contentEquals(b.copyOfRange(0, 12)))
    }

    @Test
    fun aesGcmRejectsWrongKeyWrongContextAndTampering() {
        val key = ByteArray(32) { 5 }
        val aad = "table|row|slot".encodeToByteArray()
        val sealed = AesGcm.encrypt(key, aad, "secret".encodeToByteArray())

        assertFailsWith<AuthenticationFailedException> { AesGcm.decrypt(ByteArray(32) { 6 }, aad, sealed) }
        assertFailsWith<AuthenticationFailedException> {
            AesGcm.decrypt(
                key,
                "table|other|slot".encodeToByteArray(),
                sealed,
            )
        }
        val flipped = sealed.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFailsWith<AuthenticationFailedException> { AesGcm.decrypt(key, aad, flipped) }
        val nonceFlipped = sealed.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertFailsWith<AuthenticationFailedException> { AesGcm.decrypt(key, aad, nonceFlipped) }
    }

    @Test
    fun aesGcmRejectsTruncatedInput() {
        val key = ByteArray(32) { 5 }
        val sealed = AesGcm.encrypt(key, ByteArray(0), "secret".encodeToByteArray())

        assertFailsWith<AuthenticationFailedException> {
            AesGcm.decrypt(
                key,
                ByteArray(0),
                sealed.copyOf(sealed.size - 1),
            )
        }
        assertFailsWith<AuthenticationFailedException> { AesGcm.decrypt(key, ByteArray(0), ByteArray(10)) }
    }

    @Test
    fun aesGcmRequiresA256BitKey() {
        assertFailsWith<IllegalArgumentException> { AesGcm.encrypt(ByteArray(16), ByteArray(0), ByteArray(1)) }
    }

    @Test
    fun derivedKeysAreIndependentPerPurposeAndStablePerVaultKey() {
        val vaultKey = ByteArray(32) { (it * 7).toByte() }

        val db = VaultKeys.derive(vaultKey, KeyPurpose.Database)
        val field = VaultKeys.derive(vaultKey, KeyPurpose.Field)
        val media = VaultKeys.derive(vaultKey, KeyPurpose.Media)

        assertEquals(3, setOf(db.toHex(), field.toHex(), media.toHex()).size)
        assertContentEquals(db, VaultKeys.derive(vaultKey, KeyPurpose.Database))
        assertNotEquals(db.toHex(), VaultKeys.derive(ByteArray(32), KeyPurpose.Database).toHex())
    }
}
