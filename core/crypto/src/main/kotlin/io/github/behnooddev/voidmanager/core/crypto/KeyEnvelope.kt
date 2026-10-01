package io.github.behnooddev.voidmanager.core.crypto

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

class InvalidEnvelopeException(
    message: String,
) : Exception(message)

class UnsupportedEnvelopeVersionException(
    val version: Int,
) : Exception("Unsupported key file version $version")

/**
 * The key file: the vault key wrapped under the password-derived key, and optionally under a
 * device-bound key. It is needed before the database can be opened, so it lives outside the
 * database. It contains no secret in the clear.
 *
 * Layout: magic, version, Argon2id cost, salt, password-wrapped key, device-wrapped key (length 0 when absent).
 * The header (magic to salt) is bound into the password wrap as associated data, so changing the
 * cost parameters or the salt makes unwrapping fail.
 */
class KeyEnvelope(
    val formatVersion: Int,
    val kdf: KdfParams,
    val salt: ByteArray,
    val passwordWrapped: ByteArray,
    val deviceWrapped: ByteArray?,
) {
    fun headerBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            write(MAGIC)
            writeShort(formatVersion)
            writeInt(kdf.memoryKiB)
            writeInt(kdf.iterations)
            writeInt(kdf.parallelism)
            writeByte(salt.size)
            write(salt)
        }
        return out.toByteArray()
    }

    fun passwordAad(): ByteArray = headerBytes() + PASSWORD_LABEL

    fun toBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).apply {
            write(headerBytes())
            writeShort(passwordWrapped.size)
            write(passwordWrapped)
            val device = deviceWrapped
            writeShort(device?.size ?: 0)
            if (device != null) write(device)
        }
        return out.toByteArray()
    }

    fun withPasswordWrap(
        kdf: KdfParams,
        salt: ByteArray,
        wrapped: ByteArray,
    ) = KeyEnvelope(formatVersion, kdf, salt, wrapped, deviceWrapped)

    fun withDeviceWrap(wrapped: ByteArray?) = KeyEnvelope(formatVersion, kdf, salt, passwordWrapped, wrapped)

    companion object {
        const val CURRENT_VERSION = 1
        private val MAGIC = "VMKF".encodeToByteArray()
        private val PASSWORD_LABEL = "|pw".encodeToByteArray()
        private val DEVICE_LABEL = "|dev".encodeToByteArray()
        private const val MAX_WRAPPED_BYTES = 256

        /** Associated data for the device wrap. It does not depend on the password parameters. */
        fun deviceAad(): ByteArray = MAGIC + DEVICE_LABEL

        fun parse(bytes: ByteArray): KeyEnvelope {
            try {
                val input = DataInputStream(ByteArrayInputStream(bytes))
                val magic = ByteArray(MAGIC.size).also { input.readFully(it) }
                if (!magic.contentEquals(MAGIC)) throw InvalidEnvelopeException("Not a key file")
                val version = input.readUnsignedShort()
                if (version != CURRENT_VERSION) throw UnsupportedEnvelopeVersionException(version)
                val kdf =
                    try {
                        KdfParams(input.readInt(), input.readInt(), input.readInt())
                    } catch (e: IllegalArgumentException) {
                        throw InvalidEnvelopeException("The key derivation parameters are out of range")
                    }
                val saltSize = input.readUnsignedByte()
                if (saltSize != Argon2idKdf.SALT_BYTES) throw InvalidEnvelopeException("Unexpected salt size")
                val salt = ByteArray(saltSize).also { input.readFully(it) }
                val passwordWrapped = readBlock(input) ?: throw InvalidEnvelopeException("The wrapped key is missing")
                val deviceWrapped = readBlock(input)
                if (input.available() != 0) throw InvalidEnvelopeException("Unexpected data after the key file")
                return KeyEnvelope(version, kdf, salt, passwordWrapped, deviceWrapped)
            } catch (e: EOFException) {
                throw InvalidEnvelopeException("The key file is truncated")
            }
        }

        private fun readBlock(input: DataInputStream): ByteArray? {
            val size = input.readUnsignedShort()
            if (size == 0) return null
            if (size > MAX_WRAPPED_BYTES) throw InvalidEnvelopeException("A wrapped key is too long")
            return ByteArray(size).also { input.readFully(it) }
        }
    }
}
