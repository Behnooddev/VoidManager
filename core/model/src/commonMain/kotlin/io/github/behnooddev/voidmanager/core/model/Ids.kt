package io.github.behnooddev.voidmanager.core.model

import kotlin.random.Random

fun interface IdGenerator {
    fun next(): String
}

/**
 * Generates UUIDv7 identifiers (RFC 9562): a 48-bit millisecond timestamp followed by random bits.
 * Identifiers sort by creation time at millisecond resolution. Ordering inside the same millisecond
 * is not guaranteed, and identifiers are not secrets.
 */
class UuidV7Generator(
    private val clock: () -> Long,
    private val random: Random = Random.Default,
) : IdGenerator {
    override fun next(): String {
        val bytes = random.nextBytes(UUID_BYTES)
        val timestamp = clock()
        for (i in 0 until TIMESTAMP_BYTES) {
            bytes[i] = (timestamp shr (8 * (TIMESTAMP_BYTES - 1 - i))).toByte()
        }
        bytes[VERSION_INDEX] = ((bytes[VERSION_INDEX].toInt() and 0x0F) or 0x70).toByte()
        bytes[VARIANT_INDEX] = ((bytes[VARIANT_INDEX].toInt() and 0x3F) or 0x80).toByte()
        return format(bytes)
    }

    private fun format(bytes: ByteArray): String {
        val hex = StringBuilder(UUID_BYTES * 2 + 4)
        for (i in bytes.indices) {
            if (i == 4 || i == 6 || i == 8 || i == 10) hex.append('-')
            val value = bytes[i].toInt() and 0xFF
            hex.append(HEX[value shr 4]).append(HEX[value and 0x0F])
        }
        return hex.toString()
    }

    private companion object {
        const val UUID_BYTES = 16
        const val TIMESTAMP_BYTES = 6
        const val VERSION_INDEX = 6
        const val VARIANT_INDEX = 8
        const val HEX = "0123456789abcdef"
    }
}
