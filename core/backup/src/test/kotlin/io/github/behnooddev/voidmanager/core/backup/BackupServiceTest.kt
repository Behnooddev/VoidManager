package io.github.behnooddev.voidmanager.core.backup

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Fast stand-in for Argon2id so the tests run in milliseconds. The format code is the same. */
private class FastKdf : BackupKdf {
    var calls = 0

    override fun derive(
        password: ByteArray,
        salt: ByteArray,
        params: BackupKdfParams,
    ): ByteArray {
        calls++
        val spec = PBEKeySpec(String(password, Charsets.UTF_8).toCharArray(), salt, 1000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}

class BackupServiceTest {
    private val kdf = FastKdf()
    private val service = BackupService(kdf, clock = { 1_700_000_000_000L })
    private val params = BackupKdfParams.FLOOR
    private val password = "synthetic backup password".toCharArray()
    private val secretText = "hunter2-synthetic-secret"
    private val name = "Synthetic Person"

    private fun snapshot(extraNotes: Int = 0): BackupSnapshot {
        val random = SecureRandom()
        val bulky =
            List(extraNotes) { index ->
                SnapshotValue(
                    "bulk$index",
                    "e1",
                    "sys.note",
                    null,
                    ByteArray(300)
                        .also {
                            random.nextBytes(it)
                        }.joinToString("") { "%02x".format(it) },
                    null,
                    false,
                    index,
                    1,
                    0,
                    null,
                    1L,
                    1L,
                    emptyList(),
                )
            }
        return BackupSnapshot(
            schemaVersion = 1,
            definitions =
                listOf(
                    SnapshotDefinition(
                        "usr.1",
                        null,
                        10,
                        0,
                        "Locker code",
                        null,
                        3,
                        false,
                        true,
                        false,
                        99,
                        emptyList(),
                    ),
                    SnapshotDefinition(
                        "sys.account",
                        "account",
                        3,
                        11,
                        "Account",
                        "Logins",
                        2,
                        true,
                        false,
                        true,
                        5,
                        listOf(SnapshotDefinitionPart("password", 3), SnapshotDefinitionPart("service", 1)),
                    ),
                ),
            entities =
                listOf(
                    SnapshotEntity("e1", 0, name, true, null, 10L, 20L, null),
                    SnapshotEntity("e2", 0, "علی رضایی", false, "photo1", 11L, 21L, 30L),
                    SnapshotEntity("self", 1, "Me", false, null, 12L, 22L, null),
                ),
            values =
                listOf(
                    SnapshotValue(
                        "v1",
                        "e1",
                        "sys.phone",
                        "Mobile",
                        "+98 912 345 6789",
                        null,
                        true,
                        0,
                        1,
                        0,
                        null,
                        10L,
                        20L,
                        emptyList(),
                    ),
                    SnapshotValue(
                        "v2",
                        "e1",
                        "sys.account",
                        null,
                        null,
                        secretText,
                        false,
                        1,
                        3,
                        2,
                        "{\"k\":1}",
                        10L,
                        20L,
                        listOf(
                            SnapshotPart("password", secretText, 3),
                            SnapshotPart("service", "Mail", 1),
                            SnapshotPart("recovery", null, 3),
                        ),
                    ),
                ) + bulky,
            relationships = listOf(SnapshotRelationship("r1", "p1", "e1", "e2", "sys.rel.parent", "note", 1L, 2L)),
        )
    }

    private fun create(snapshot: BackupSnapshot = snapshot()): ByteArray =
        service.create(snapshot, password, params, "0.9.0")

    private fun open(
        bytes: ByteArray,
        pw: CharArray = password,
    ): OpenResult = service.open(bytes, pw)

    private fun contains(
        haystack: ByteArray,
        needle: String,
    ): Boolean =
        String(haystack, Charsets.ISO_8859_1).contains(String(needle.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1))

    /** Rewrites the JSON header and fixes the length field, keeping the encrypted payload as it is. */
    private fun withHeader(
        file: ByteArray,
        change: (Map<String, Any?>) -> Map<String, Any?>,
    ): ByteArray {
        val length =
            ((file[6].toInt() and 0xFF) shl 24) or ((file[7].toInt() and 0xFF) shl 16) or
                ((file[8].toInt() and 0xFF) shl 8) or
                (file[9].toInt() and 0xFF)

        @Suppress("UNCHECKED_CAST")
        val header = MiniJson.parse(String(file.copyOfRange(10, 10 + length), Charsets.UTF_8)) as Map<String, Any?>
        val newHeader = MiniJson.write(change(header)).toByteArray(Charsets.UTF_8)
        val rest = file.copyOfRange(10 + length, file.size)
        val prefix = file.copyOfRange(0, 6)
        return prefix +
            byteArrayOf(
                (newHeader.size ushr 24).toByte(),
                (newHeader.size ushr 16).toByte(),
                (newHeader.size ushr 8).toByte(),
                newHeader.size.toByte(),
            ) +
            newHeader +
            rest
    }

    @Test
    fun aBackupRoundTripsWithEverythingIncludingPersianTextAndSecrets() {
        val original = snapshot()
        val opened = assertIs<OpenResult.Opened>(open(create(original)))
        assertEquals(original, opened.snapshot)
        assertEquals(2, opened.summary.definitions)
        assertEquals(3, opened.summary.entities)
        assertEquals(2, opened.summary.values)
        assertEquals(1, opened.summary.relationships)
        assertEquals(BackupHeaderInfo(1, 1, "0.9.0", 1_700_000_000_000L), opened.summary.header)
    }

    @Test
    fun aWrongOrEmptyPasswordIsReportedAsSuchWithoutTouchingTheContent() {
        val file = create()
        assertEquals(OpenResult.WrongPasswordOrChanged, open(file, "another password".toCharArray()))
        assertEquals(OpenResult.WrongPasswordOrChanged, open(file, CharArray(0)))
    }

    @Test
    fun equivalentUnicodeFormsOfThePasswordOpenTheBackup() {
        val composed = "caf\u00e9 password".toCharArray()
        val decomposed = "cafe\u0301 password".toCharArray()
        val file = service.create(snapshot(), composed, params, "0.9.0")
        assertIs<OpenResult.Opened>(open(file, decomposed))
    }

    @Test
    fun theFileHoldsNoPlaintextAtAll() {
        val file = create()
        for (needle in listOf(
            secretText,
            name,
            "Synthetic",
            "Locker",
            "Mail",
            "912",
            "علی",
            "Mobile",
            "sys.phone",
        )) {
            assertFalse(contains(file, needle), needle)
        }
        assertTrue(contains(file, "VMBK"))
    }

    @Test
    fun theHeaderIsReadableWithoutAPasswordAndHoldsNoPersonalData() {
        val file = create()
        kdf.calls = 0
        val result = assertIs<InspectResult.Header>(service.inspect(file))
        assertEquals(BackupHeaderInfo(1, 1, "0.9.0", 1_700_000_000_000L), result.info)
        assertEquals(0, kdf.calls, "inspecting derives no key")
        val headerText = String(file, Charsets.ISO_8859_1).substringBefore("}}")
        assertFalse(headerText.contains("Synthetic"))
    }

    @Test
    fun twoBackupsOfTheSameDataDifferButBothOpen() {
        val first = create()
        val second = create()
        assertFalse(first.contentEquals(second))
        assertEquals(open(first), open(second).let { it })
    }

    @Test
    fun cuttingTheFileShortOrAddingToItMakesItCorruptNotWrongPassword() {
        val file = create(snapshot(extraNotes = 900))
        assertTrue(file.size > 3 * BackupService.CHUNK_SIZE, "the payload spans several chunks")
        assertIs<OpenResult.Corrupted>(open(file.copyOf(file.size - 1)))
        assertIs<OpenResult.Corrupted>(open(file.copyOf(file.size - (BackupService.CHUNK_SIZE + 16))))
        assertIs<OpenResult.Corrupted>(open(file + byteArrayOf(0)))
        assertIs<OpenResult.Corrupted>(open(file.copyOf(40)))
    }

    @Test
    fun changingAnyByteOfThePayloadMakesItCorrupt() {
        val file = create()
        val headerLength = ((file[8].toInt() and 0xFF) shl 8) or (file[9].toInt() and 0xFF)
        val start = 10 + headerLength
        for (index in listOf(start, start + 20, file.size / 2 + 5, file.size - 1)) {
            val changed = file.copyOf().also { it[index] = (it[index].toInt() xor 0x40).toByte() }
            assertIs<OpenResult.Corrupted>(open(changed), "byte $index")
        }
    }

    @Test
    fun changingAHeaderFieldBreaksTheFileEvenWhenThePasswordIsRight() {
        val file = create()
        val result = open(withHeader(file) { it + ("app_version" to "9.9.9") })
        assertIs<OpenResult.Corrupted>(result)
        val resalted =
            open(
                withHeader(file) { header ->
                    header +
                        ("kdf" to (header["kdf"] as Map<*, *> + ("salt" to "AAAAAAAAAAAAAAAAAAAAAA")))
                },
            )
        assertEquals(OpenResult.WrongPasswordOrChanged, resalted, "a different salt gives a different key")
    }

    @Test
    fun aKeyCostOutsideTheLimitsIsRefusedBeforeAnyDerivation() {
        val file = create()
        kdf.calls = 0
        val huge =
            withHeader(file) { header ->
                header +
                    ("kdf" to (header["kdf"] as Map<*, *> + ("memory_kib" to 64L * 1024 * 1024)))
            }
        assertIs<OpenResult.Corrupted>(open(huge))
        val tiny =
            withHeader(file) { header -> header + ("kdf" to (header["kdf"] as Map<*, *> + ("iterations" to 1L))) }
        assertIs<OpenResult.Corrupted>(open(tiny))
        assertEquals(0, kdf.calls, "no key was derived for a hostile cost")
    }

    @Test
    fun unknownPayloadKindOrAlgorithmIsNotSilentlyAccepted() {
        val file = create()
        assertIs<OpenResult.Corrupted>(open(withHeader(file) { it + ("payload_kind" to "logical-v2") }))
        assertIs<OpenResult.Corrupted>(
            open(
                withHeader(file) { header ->
                    header +
                        ("cipher" to (header["cipher"] as Map<*, *> + ("algorithm" to "rot13")))
                },
            ),
        )
    }

    @Test
    fun filesThatAreNotBackupsOrAreFromTheFutureAreSaidSo() {
        assertEquals(OpenResult.NotABackup, open(ByteArray(0)))
        assertEquals(OpenResult.NotABackup, open("PK\u0003\u0004 not a backup at all".toByteArray()))
        assertEquals(OpenResult.NotABackup, open("VMB".toByteArray()))
        val file = create()
        val future = file.copyOf().also { it[5] = 2 }
        assertEquals(OpenResult.UnsupportedVersion(2), open(future))
        assertEquals(InspectResult.UnsupportedVersion(2), service.inspect(future))
        assertEquals(InspectResult.NotABackup, service.inspect(ByteArray(3)))
    }

    @Test
    fun aHeaderLengthThatPointsPastTheFileIsDamage() {
        val file = create()
        val broken =
            file.copyOf().also {
                it[6] = 0x7f
            }
        assertIs<OpenResult.Corrupted>(open(broken))
        assertIs<InspectResult.Damaged>(service.inspect(broken))
        assertIs<OpenResult.Corrupted>(open(file.copyOf(12)))
    }

    @Test
    fun aFileOverTheSizeLimitIsRefusedUpFront() {
        val small = BackupService(kdf, limits = BackupLimits(maxFileBytes = 100))
        assertEquals(OpenResult.TooLarge, small.open(create(), password))
        assertEquals(InspectResult.TooLarge, small.inspect(create()))
    }

    @Test
    fun contentThatDecryptsButFailsValidationIsReportedAsInvalid() {
        val badKind =
            snapshot().let {
                it.copy(
                    entities =
                        it.entities + SnapshotEntity("e9", 7, "x", false, null, 1L, 1L, null),
                )
            }
        assertIs<OpenResult.InvalidContent>(open(create(badKind)))

        val repeatedPart =
            snapshot().let { s ->
                val v = s.values[1]
                s.copy(values = listOf(v.copy(parts = v.parts + SnapshotPart("password", "again", 3))))
            }
        assertIs<OpenResult.InvalidContent>(open(create(repeatedPart)))
    }

    @Test
    fun theSnapshotTypesPrintNoStoredText() {
        val text =
            snapshot().toString() + snapshot().values.toString() + snapshot().entities.toString() +
                snapshot().values[1].parts.toString()
        for (needle in listOf(secretText, name, "912", "Mail", "علی")) assertFalse(text.contains(needle), needle)
    }

    @Test
    fun anEmptyVaultCanBeBackedUpAndRestored() {
        val empty = BackupSnapshot(1, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(empty, (open(create(empty)) as OpenResult.Opened).snapshot)
    }

    @Test
    fun theBackupPasswordMustNotBeEmpty() {
        var failed = false
        try {
            service.create(snapshot(), CharArray(0), params, "0.9.0")
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
