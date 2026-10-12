package io.github.behnooddev.voidmanager.core.backup

import java.nio.CharBuffer
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64

/** Argon2id cost for a backup key. The limits match the vault key file, and are checked before any derivation. */
data class BackupKdfParams(
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
) {
    init {
        require(memoryKiB in MIN_MEMORY_KIB..MAX_MEMORY_KIB) { "Memory cost out of range" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Iteration count out of range" }
        require(parallelism in 1..MAX_PARALLELISM) { "Parallelism out of range" }
    }

    companion object {
        const val MIN_MEMORY_KIB = 19 * 1024
        const val MAX_MEMORY_KIB = 512 * 1024
        const val MIN_ITERATIONS = 2
        const val MAX_ITERATIONS = 10
        const val MAX_PARALLELISM = 8

        val FLOOR = BackupKdfParams(MIN_MEMORY_KIB, MIN_ITERATIONS, 1)
        val DEFAULT = BackupKdfParams(memoryKiB = 64 * 1024, iterations = 3, parallelism = 1)
    }
}

/** The password hash. The app supplies Argon2id; tests supply a fast stand-in. Returns 32 bytes. */
fun interface BackupKdf {
    fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: BackupKdfParams,
    ): ByteArray
}

/** What the file says about itself without a password. No names, counts or other personal data. */
data class BackupHeaderInfo(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: Long,
)

sealed interface InspectResult {
    data class Header(
        val info: BackupHeaderInfo,
    ) : InspectResult

    data object NotABackup : InspectResult

    /** A newer format than this app reads. */
    data class UnsupportedVersion(
        val version: Int,
    ) : InspectResult

    data class Damaged(
        val reason: String,
    ) : InspectResult

    data object TooLarge : InspectResult
}

/** What a backup holds, for the summary shown before anything is restored. */
data class BackupSummary(
    val header: BackupHeaderInfo,
    val definitions: Int,
    val entities: Int,
    val values: Int,
    val relationships: Int,
)

sealed interface OpenResult {
    data class Opened(
        val summary: BackupSummary,
        val snapshot: BackupSnapshot,
    ) : OpenResult

    /** The password is wrong, or the header was changed so that the key does not match. */
    data object WrongPasswordOrChanged : OpenResult

    /** The password is right but the file is damaged, cut short or was changed. */
    data class Corrupted(
        val reason: String,
    ) : OpenResult

    data class UnsupportedVersion(
        val version: Int,
    ) : OpenResult

    data object NotABackup : OpenResult

    data object TooLarge : OpenResult

    /** Decrypted fine, but the content does not pass validation. Nothing has been restored. */
    data class InvalidContent(
        val reason: String,
    ) : OpenResult
}

/**
 * Writes and reads encrypted backup files (`docs/backup-format.md`).
 *
 * Layout: `VMBK`, format version (2 bytes), header length (4 bytes), canonical JSON header, then the
 * payload as chunked AES-256-GCM. The header is authenticated with every chunk. The key comes from
 * Argon2id over the backup password; a small sealed check value tells a wrong password from a
 * damaged file. The password is normalized (NFKC) like the master password.
 */
class BackupService(
    private val kdf: BackupKdf,
    private val random: SecureRandom = SecureRandom(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val limits: BackupLimits = BackupLimits(),
) {
    fun create(
        snapshot: BackupSnapshot,
        password: CharArray,
        params: BackupKdfParams,
        appVersion: String,
    ): ByteArray {
        require(password.isNotEmpty()) { "The backup password is empty" }
        val payload = LogicalPayload.write(snapshot)
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val streamSalt = ByteArray(STREAM_SALT_BYTES).also { random.nextBytes(it) }

        val backupKey = deriveKey(password, salt, params)
        val checkKey = Hkdf.derive(backupKey, streamSalt, CHECK_INFO, KEY_BYTES)
        val streamKey = Hkdf.derive(backupKey, streamSalt, STREAM_INFO, KEY_BYTES)
        val noncePrefix = Hkdf.derive(backupKey, streamSalt, NONCE_INFO, StreamCipher.NONCE_PREFIX_BYTES)
        backupKey.fill(0)
        try {
            val check = StreamCipher.sealOnce(checkKey, CHECK_AAD, CHECK_PLAIN)
            val header =
                MiniJson
                    .write(
                        mapOf(
                            "format_version" to FORMAT_VERSION,
                            "schema_version" to snapshot.schemaVersion,
                            "app_version" to appVersion,
                            "created_at" to clock(),
                            "payload_kind" to PAYLOAD_KIND,
                            "check" to base64(check),
                            "kdf" to
                                mapOf(
                                    "algorithm" to "argon2id",
                                    "memory_kib" to params.memoryKiB,
                                    "iterations" to params.iterations,
                                    "parallelism" to params.parallelism,
                                    "salt" to base64(salt),
                                ),
                            "cipher" to
                                mapOf(
                                    "algorithm" to CIPHER_NAME,
                                    "chunk_size" to CHUNK_SIZE,
                                    "stream_salt" to base64(streamSalt),
                                ),
                        ),
                    ).toByteArray(Charsets.UTF_8)
            val encrypted = StreamCipher.encrypt(streamKey, noncePrefix, header, payload, CHUNK_SIZE)
            return MAGIC + u16(FORMAT_VERSION) + u32(header.size) + header + encrypted
        } finally {
            checkKey.fill(0)
            streamKey.fill(0)
            noncePrefix.fill(0)
        }
    }

    /** Reads only the header. Needs no password and derives no key. */
    fun inspect(bytes: ByteArray): InspectResult =
        when (val parsed = parseContainer(bytes)) {
            is Container.Ok -> InspectResult.Header(parsed.info)
            Container.NotABackup -> InspectResult.NotABackup
            is Container.Unsupported -> InspectResult.UnsupportedVersion(parsed.version)
            is Container.Bad -> InspectResult.Damaged(parsed.reason)
            Container.TooLarge -> InspectResult.TooLarge
        }

    /** Decrypts and validates a backup in memory. Nothing is written anywhere. The caller wipes [password]. */
    fun open(
        bytes: ByteArray,
        password: CharArray,
    ): OpenResult {
        val container =
            when (val parsed = parseContainer(bytes)) {
                is Container.Ok -> parsed
                Container.NotABackup -> return OpenResult.NotABackup
                is Container.Unsupported -> return OpenResult.UnsupportedVersion(parsed.version)
                is Container.Bad -> return OpenResult.Corrupted(parsed.reason)
                Container.TooLarge -> return OpenResult.TooLarge
            }
        if (password.isEmpty()) return OpenResult.WrongPasswordOrChanged

        val backupKey = deriveKey(password, container.salt, container.params)
        val checkKey = Hkdf.derive(backupKey, container.streamSalt, CHECK_INFO, KEY_BYTES)
        val streamKey = Hkdf.derive(backupKey, container.streamSalt, STREAM_INFO, KEY_BYTES)
        val noncePrefix = Hkdf.derive(backupKey, container.streamSalt, NONCE_INFO, StreamCipher.NONCE_PREFIX_BYTES)
        backupKey.fill(0)
        try {
            val check = StreamCipher.openOnce(checkKey, CHECK_AAD, container.check)
            if (check == null || !check.contentEquals(CHECK_PLAIN)) return OpenResult.WrongPasswordOrChanged

            val payload =
                StreamCipher.decrypt(
                    streamKey,
                    noncePrefix,
                    container.headerBytes,
                    container.encrypted,
                    container.chunkSize,
                )
                    ?: return OpenResult.Corrupted("The file is damaged, incomplete or has been changed")
            val snapshot =
                try {
                    LogicalPayload.read(payload, limits)
                } catch (e: BackupTooLargeException) {
                    return OpenResult.TooLarge
                } catch (e: BackupFormatException) {
                    return OpenResult.InvalidContent(e.message ?: "The content is not valid")
                }
            val summary =
                BackupSummary(
                    container.info,
                    snapshot.definitions.size,
                    snapshot.entities.size,
                    snapshot.values.size,
                    snapshot.relationships.size,
                )
            return OpenResult.Opened(summary, snapshot)
        } finally {
            checkKey.fill(0)
            streamKey.fill(0)
            noncePrefix.fill(0)
        }
    }

    // ---- container parsing ----

    private sealed interface Container {
        class Ok(
            val info: BackupHeaderInfo,
            val headerBytes: ByteArray,
            val params: BackupKdfParams,
            val salt: ByteArray,
            val streamSalt: ByteArray,
            val check: ByteArray,
            val chunkSize: Int,
            val encrypted: ByteArray,
        ) : Container

        data object NotABackup : Container

        class Unsupported(
            val version: Int,
        ) : Container

        class Bad(
            val reason: String,
        ) : Container

        data object TooLarge : Container
    }

    private fun parseContainer(bytes: ByteArray): Container {
        if (bytes.size.toLong() > limits.maxFileBytes) return Container.TooLarge
        if (bytes.size < PREFIX_BYTES ||
            !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
        ) {
            return Container.NotABackup
        }
        val version = ((bytes[4].toInt() and BYTE_MASK) shl 8) or (bytes[5].toInt() and BYTE_MASK)
        if (version != FORMAT_VERSION) return Container.Unsupported(version)
        val headerLength = readU32(bytes, HEADER_LENGTH_OFFSET)
        if (headerLength <= 0 || headerLength > MAX_HEADER_BYTES || PREFIX_BYTES + headerLength > bytes.size) {
            return Container.Bad("The header is damaged")
        }
        val headerBytes = bytes.copyOfRange(PREFIX_BYTES, PREFIX_BYTES + headerLength.toInt())
        val encrypted = bytes.copyOfRange(PREFIX_BYTES + headerLength.toInt(), bytes.size)

        return try {
            val header = MiniJson.parse(String(headerBytes, Charsets.UTF_8)).asObject()
            if (header.integer("format_version") !=
                version.toLong()
            ) {
                return Container.Bad("The header does not match the file")
            }
            if (header.string("payload_kind") !=
                PAYLOAD_KIND
            ) {
                return Container.Bad("This kind of payload is not supported")
            }
            val kdfSection = header.obj("kdf")
            val cipherSection = header.obj("cipher")
            if (kdfSection.string("algorithm") != "argon2id" || cipherSection.string("algorithm") != CIPHER_NAME) {
                return Container.Bad("This kind of encryption is not supported")
            }
            val chunkSize = cipherSection.integer("chunk_size")
            if (chunkSize < MIN_CHUNK || chunkSize > MAX_CHUNK) return Container.Bad("The chunk size is out of range")
            val params = paramsFrom(kdfSection) ?: return Container.Bad("The key cost is out of range")
            val salt = base64Decode(kdfSection.string("salt"))
            val streamSalt = base64Decode(cipherSection.string("stream_salt"))
            val check = base64Decode(header.string("check"))
            if (salt.size != SALT_BYTES ||
                streamSalt.size != STREAM_SALT_BYTES ||
                check.size != CHECK_PLAIN.size + StreamCipher.TAG_BYTES
            ) {
                return Container.Bad("The header is damaged")
            }
            val info =
                BackupHeaderInfo(
                    formatVersion = version,
                    schemaVersion = header.integer("schema_version").toInt(),
                    appVersion = header.string("app_version"),
                    createdAt = header.integer("created_at"),
                )
            Container.Ok(info, headerBytes, params, salt, streamSalt, check, chunkSize.toInt(), encrypted)
        } catch (e: BackupFormatException) {
            Container.Bad("The header is damaged")
        } catch (e: IllegalArgumentException) {
            Container.Bad("The header is damaged")
        }
    }

    private fun paramsFrom(kdfSection: Map<String, Any?>): BackupKdfParams? {
        val memory = kdfSection.integer("memory_kib")
        val iterations = kdfSection.integer("iterations")
        val parallelism = kdfSection.integer("parallelism")
        val inRange =
            memory in BackupKdfParams.MIN_MEMORY_KIB.toLong()..BackupKdfParams.MAX_MEMORY_KIB.toLong() &&
                iterations in BackupKdfParams.MIN_ITERATIONS.toLong()..BackupKdfParams.MAX_ITERATIONS.toLong() &&
                parallelism in 1L..BackupKdfParams.MAX_PARALLELISM.toLong()
        return if (inRange) BackupKdfParams(memory.toInt(), iterations.toInt(), parallelism.toInt()) else null
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        params: BackupKdfParams,
    ): ByteArray {
        val normalized = Normalizer.normalize(CharBuffer.wrap(password), Normalizer.Form.NFKC)
        val buffer = Charsets.UTF_8.encode(normalized)
        val passwordBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        if (buffer.hasArray()) buffer.array().fill(0)
        try {
            val key = kdf.derive(passwordBytes, salt, params)
            check(key.size == KEY_BYTES) { "The key derivation must return $KEY_BYTES bytes" }
            return key
        } finally {
            passwordBytes.fill(0)
        }
    }

    private fun Any?.asObject(): Map<String, Any?> {
        val map = this as? Map<*, *> ?: throw BackupFormatException("Not an object")
        return map.entries.associate { (key, value) -> key as String to value }
    }

    private fun Map<String, Any?>.obj(key: String): Map<String, Any?> = this[key].asObject()

    private fun Map<String, Any?>.string(key: String): String =
        this[key] as? String ?: throw BackupFormatException("Missing '$key'")

    private fun Map<String, Any?>.integer(key: String): Long =
        this[key] as? Long ?: throw BackupFormatException("Missing '$key'")

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

    private fun base64Decode(text: String): ByteArray =
        try {
            Base64.getDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("Bad encoding")
        }

    private fun readU32(
        bytes: ByteArray,
        offset: Int,
    ): Long {
        var value = 0L
        for (index in 0 until U32_BYTES) {
            value =
                (value shl BITS_PER_BYTE) or (bytes[offset + index].toLong() and BYTE_MASK.toLong())
        }
        return value
    }

    private fun u16(value: Int): ByteArray = byteArrayOf((value ushr 8).toByte(), value.toByte())

    private fun u32(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (
                value ushr
                    8
            ).toByte(),
            value.toByte(),
        )

    companion object {
        const val FORMAT_VERSION = 1
        const val CHUNK_SIZE = 64 * 1024
        private const val PAYLOAD_KIND = "logical-v1"
        private const val CIPHER_NAME = "aes-256-gcm-hkdf-streaming"
        private const val KEY_BYTES = 32
        private const val SALT_BYTES = 16
        private const val STREAM_SALT_BYTES = 32
        private const val PREFIX_BYTES = 10
        private const val BYTE_MASK = 0xFF
        private const val BITS_PER_BYTE = 8
        private const val U32_BYTES = 4
        private const val HEADER_LENGTH_OFFSET = 6
        private const val MAX_HEADER_BYTES = 16L * 1024L
        private const val MIN_CHUNK = 4096L
        private const val MAX_CHUNK = 1024L * 1024L
        private val MAGIC = "VMBK".toByteArray(Charsets.US_ASCII)
        private val CHECK_PLAIN = "VMBK-OK".toByteArray(Charsets.US_ASCII)
        private val CHECK_AAD = "VMBK check v1".toByteArray(Charsets.US_ASCII)
        private val CHECK_INFO = "VMBK check key v1".toByteArray(Charsets.US_ASCII)
        private val STREAM_INFO = "VMBK stream key v1".toByteArray(Charsets.US_ASCII)
        private val NONCE_INFO = "VMBK nonce prefix v1".toByteArray(Charsets.US_ASCII)
    }
}
