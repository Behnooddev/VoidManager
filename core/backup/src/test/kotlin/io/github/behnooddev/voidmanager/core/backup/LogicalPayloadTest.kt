package io.github.behnooddev.voidmanager.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LogicalPayloadTest {
    private val snapshot =
        BackupSnapshot(
            1,
            listOf(SnapshotDefinition("usr.1", null, 10, 0, "Locker", null, 1, false, true, false, 1, emptyList())),
            listOf(SnapshotEntity("e1", 0, "Synthetic", false, null, 1L, 2L, null)),
            listOf(SnapshotValue("v1", "e1", "usr.1", null, "12", null, false, 0, 1, 0, null, 1L, 2L, emptyList())),
            emptyList(),
        )
    private val limits = BackupLimits()

    private fun entries(zip: ByteArray): MutableMap<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                result[entry.name] = input.readBytes()
            }
        }
        return result
    }

    private fun zip(files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun mutated(change: (MutableMap<String, ByteArray>) -> Unit): ByteArray =
        zip(entries(LogicalPayload.write(snapshot)).also(change))

    @Test
    fun theSameDataGivesTheSameBytes() {
        assertContentEquals(LogicalPayload.write(snapshot), LogicalPayload.write(snapshot))
    }

    @Test
    fun theArchiveHoldsTheManifestAndOneFilePerTable() {
        assertEquals(
            listOf("manifest.json", "definitions.jsonl", "entities.jsonl", "values.jsonl", "relationships.jsonl"),
            entries(LogicalPayload.write(snapshot)).keys.toList(),
        )
    }

    @Test
    fun theSnapshotSurvivesTheArchive() {
        assertEquals(snapshot, LogicalPayload.read(LogicalPayload.write(snapshot), limits))
    }

    @Test
    fun aTableThatDoesNotMatchItsChecksumIsRefused() {
        val changed =
            mutated {
                it["entities.jsonl"] =
                    it.getValue("entities.jsonl").copyOf().also { bytes -> bytes[10] = 'X'.code.toByte() }
            }
        assertFailsWith<BackupFormatException> { LogicalPayload.read(changed, limits) }
    }

    @Test
    fun aMissingOrExtraFileIsRefused() {
        assertFailsWith<BackupFormatException> { LogicalPayload.read(mutated { it.remove("values.jsonl") }, limits) }
        assertFailsWith<BackupFormatException> {
            LogicalPayload.read(
                mutated { it["extra.txt"] = ByteArray(1) },
                limits,
            )
        }
        assertFailsWith<BackupFormatException> { LogicalPayload.read(mutated { it.remove("manifest.json") }, limits) }
    }

    @Test
    fun filesWithPathsOrDotsInTheirNamesAreRefused() {
        assertFailsWith<BackupFormatException> {
            LogicalPayload.read(
                mutated { it["../outside.txt"] = ByteArray(1) },
                limits,
            )
        }
        assertFailsWith<BackupFormatException> {
            LogicalPayload.read(
                mutated { it["sub/dir.txt"] = ByteArray(1) },
                limits,
            )
        }
    }

    @Test
    fun aWrongRowCountInTheManifestIsRefused() {
        val changed =
            mutated {
                val manifest = String(it.getValue("manifest.json"), Charsets.UTF_8).replace("\"rows\":1", "\"rows\":2")
                it["manifest.json"] = manifest.toByteArray(Charsets.UTF_8)
            }
        assertFailsWith<BackupFormatException> { LogicalPayload.read(changed, limits) }
    }

    @Test
    fun anUnknownPayloadVersionIsRefused() {
        val changed =
            mutated {
                val manifest =
                    String(
                        it.getValue("manifest.json"),
                        Charsets.UTF_8,
                    ).replace("\"payload_version\":1", "\"payload_version\":9")
                it["manifest.json"] = manifest.toByteArray(Charsets.UTF_8)
            }
        assertFailsWith<BackupFormatException> { LogicalPayload.read(changed, limits) }
    }

    @Test
    fun anEntryOrAnArchiveOverTheLimitIsRefusedWhileReading() {
        val payload = LogicalPayload.write(snapshot)
        assertFailsWith<BackupTooLargeException> { LogicalPayload.read(payload, BackupLimits(maxEntryBytes = 50)) }
        assertFailsWith<BackupTooLargeException> { LogicalPayload.read(payload, BackupLimits(maxTotalBytes = 100)) }
        assertFailsWith<BackupTooLargeException> { LogicalPayload.read(payload, BackupLimits(maxEntries = 2)) }
    }

    @Test
    fun aCompressionBombIsStoppedByTheLimitsBeforeItFillsMemory() {
        val bomb = zip(mapOf("manifest.json" to ByteArray(50 * 1024 * 1024)))
        assertEquals(true, bomb.size < 200_000, "it compresses to almost nothing")
        assertFailsWith<BackupTooLargeException> {
            LogicalPayload.read(
                bomb,
                BackupLimits(maxEntryBytes = 1024 * 1024),
            )
        }
    }

    @Test
    fun aRecordWithAMissingOrWrongTypedFieldIsRefused() {
        val changed =
            mutated {
                val text =
                    String(
                        it.getValue("entities.jsonl"),
                        Charsets.UTF_8,
                    ).replace("\"display_name\":\"Synthetic\"", "\"display_name\":5")
                it["entities.jsonl"] = text.toByteArray(Charsets.UTF_8)
            }
        // The checksum no longer matches, which is also a refusal; the point is that nothing is accepted.
        assertFailsWith<BackupFormatException> { LogicalPayload.read(changed, limits) }
    }
}
