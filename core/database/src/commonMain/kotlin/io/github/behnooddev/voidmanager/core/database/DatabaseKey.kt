package io.github.behnooddev.voidmanager.core.database

/**
 * The 256-bit key that encrypts the database. It is passed to SQLCipher as a raw key, so no password
 * derivation runs when the database is opened. The vault key hierarchy produces it (Phase 3).
 */
class DatabaseKey(
    private val bytes: ByteArray,
) {
    init {
        require(bytes.size == KEY_BYTES) { "Database key must be $KEY_BYTES bytes" }
    }

    /** The key in the raw-key syntax SQLCipher expects: x'<64 hex digits>'. */
    fun toSqlCipherRawKey(): String {
        val out = StringBuilder(KEY_BYTES * 2 + 3)
        out.append("x'")
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out.append(HEX[v shr 4]).append(HEX[v and 0x0F])
        }
        return out.append('\'').toString()
    }

    override fun toString(): String = "DatabaseKey([redacted])"

    private companion object {
        const val KEY_BYTES = 32
        const val HEX = "0123456789abcdef"
    }
}
