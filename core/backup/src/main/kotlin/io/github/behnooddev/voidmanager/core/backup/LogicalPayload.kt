package io.github.behnooddev.voidmanager.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Size limits applied while a backup is read, so a hostile file cannot exhaust memory. */
data class BackupLimits(
    val maxFileBytes: Long = 256L * MIB,
    val maxEntryBytes: Long = 128L * MIB,
    val maxTotalBytes: Long = 256L * MIB,
    val maxEntries: Int = 16,
) {
    private companion object {
        const val MIB = 1024L * 1024L
    }
}

/**
 * The `logical-v1` payload: a ZIP archive with `manifest.json` and one JSON Lines file per table.
 * It is a logical export, not a copy of the database file, so it can be validated and migrated.
 * The manifest lists every table with its row count and SHA-256, and the reader accepts no other file.
 */
internal object LogicalPayload {
    const val PAYLOAD_VERSION = 1
    private const val MANIFEST = "manifest.json"
    private const val BUFFER = 8192
    private val FIXED_TIME = LocalDateTime.of(1980, 1, 1, 0, 0)

    private class Table(
        val name: String,
        val file: String,
    )

    private val TABLES =
        listOf(
            Table("definitions", "definitions.jsonl"),
            Table("entities", "entities.jsonl"),
            Table("values", "values.jsonl"),
            Table("relationships", "relationships.jsonl"),
        )

    fun write(snapshot: BackupSnapshot): ByteArray {
        val files =
            mapOf(
                "definitions" to snapshot.definitions.map(::definitionJson),
                "entities" to snapshot.entities.map(::entityJson),
                "values" to snapshot.values.map(::valueJson),
                "relationships" to snapshot.relationships.map(::relationshipJson),
            ).mapValues { (_, rows) ->
                rows.joinToString(separator = "") { MiniJson.write(it) + "\n" }.toByteArray(Charsets.UTF_8)
            }

        val manifest =
            mapOf(
                "payload_version" to PAYLOAD_VERSION,
                "schema_version" to snapshot.schemaVersion,
                "tables" to
                    TABLES.map { table ->
                        val bytes = files.getValue(table.name)
                        mapOf(
                            "name" to table.name,
                            "file" to table.file,
                            "rows" to bytes.count { it == '\n'.code.toByte() },
                            "sha256" to sha256Hex(bytes),
                        )
                    },
            )

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            addEntry(zip, MANIFEST, MiniJson.write(manifest).toByteArray(Charsets.UTF_8))
            for (table in TABLES) addEntry(zip, table.file, files.getValue(table.name))
        }
        return out.toByteArray()
    }

    /**
     * Throws [BackupFormatException] for anything that does not match the manifest,
     * and [BackupTooLargeException] past the limits.
     */
    fun read(
        zipBytes: ByteArray,
        limits: BackupLimits,
    ): BackupSnapshot {
        val entries = readEntries(zipBytes, limits)
        val manifest =
            parseObject(entries[MANIFEST] ?: throw BackupFormatException("The backup has no manifest"), "manifest")
        if (manifest.long("payload_version") !=
            PAYLOAD_VERSION.toLong()
        ) {
            throw BackupFormatException("Unknown payload version")
        }
        val schemaVersion = manifest.long("schema_version").toInt()

        val listed = manifest.list("tables")
        if (listed.size != TABLES.size) throw BackupFormatException("The manifest lists the wrong tables")
        val parsed = HashMap<String, List<Map<String, Any?>>>()
        for ((index, table) in TABLES.withIndex()) {
            val row = listed[index] as? Map<*, *> ?: throw BackupFormatException("Bad manifest entry")
            val entry = row.entries.associate { (key, value) -> key as String to value }
            if (entry.str("name") != table.name ||
                entry.str("file") != table.file
            ) {
                throw BackupFormatException("The manifest lists the wrong tables")
            }
            val bytes = entries[table.file] ?: throw BackupFormatException("A table file is missing")
            if (sha256Hex(bytes) !=
                entry.str("sha256")
            ) {
                throw BackupFormatException("A table does not match its checksum")
            }
            val lines = splitLines(bytes, table.name)
            if (lines.size.toLong() !=
                entry.long("rows")
            ) {
                throw BackupFormatException("A table has the wrong number of rows")
            }
            parsed[table.name] =
                lines.mapIndexed { number, line -> parseObject(line, "${table.name} row ${number + 1}") }
        }
        if (entries.keys !=
            (TABLES.map { it.file } + MANIFEST).toSet()
        ) {
            throw BackupFormatException("The backup contains unexpected files")
        }

        return BackupSnapshot(
            schemaVersion = schemaVersion,
            definitions = parsed.getValue("definitions").map(::definitionFrom),
            entities = parsed.getValue("entities").map(::entityFrom),
            values = parsed.getValue("values").map(::valueFrom),
            relationships = parsed.getValue("relationships").map(::relationshipFrom),
        )
    }

    private fun addEntry(
        zip: ZipOutputStream,
        name: String,
        bytes: ByteArray,
    ) {
        val entry = ZipEntry(name)
        entry.setTimeLocal(FIXED_TIME)
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun readEntries(
        zipBytes: ByteArray,
        limits: BackupLimits,
    ): Map<String, ByteArray> {
        val result = LinkedHashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (result.size >= limits.maxEntries) throw BackupTooLargeException("The backup has too many files")
                val name = entry.name
                if (entry.isDirectory || name.contains('/') || name.contains('\\') || name.contains("..")) {
                    throw BackupFormatException("The backup contains an unexpected file name")
                }
                if (result.containsKey(name)) throw BackupFormatException("The backup repeats a file")
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER)
                var entrySize = 0L
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    entrySize += read
                    total += read
                    if (entrySize > limits.maxEntryBytes ||
                        total > limits.maxTotalBytes
                    ) {
                        throw BackupTooLargeException("The backup is larger than allowed")
                    }
                    out.write(buffer, 0, read)
                }
                result[name] = out.toByteArray()
            }
        }
        return result
    }

    private fun splitLines(
        bytes: ByteArray,
        table: String,
    ): List<String> {
        if (bytes.isEmpty()) return emptyList()
        val text = String(bytes, Charsets.UTF_8)
        if (!text.endsWith("\n")) throw BackupFormatException("A table does not end with a line break ($table)")
        return text.removeSuffix("\n").split("\n")
    }

    private fun parseObject(
        bytes: ByteArray,
        what: String,
    ): Map<String, Any?> = parseObject(String(bytes, Charsets.UTF_8), what)

    private fun parseObject(
        text: String,
        what: String,
    ): Map<String, Any?> {
        val value =
            try {
                MiniJson.parse(text)
            } catch (e: BackupFormatException) {
                throw BackupFormatException("Bad $what: ${e.message}")
            }
        @Suppress("UNCHECKED_CAST")
        return value as? Map<String, Any?> ?: throw BackupFormatException("Bad $what: not an object")
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }

    // ---- records to JSON ----

    private fun definitionJson(d: SnapshotDefinition): Map<String, Any?> =
        mapOf(
            "id" to d.id,
            "system_key" to d.systemKey,
            "section" to d.section,
            "data_type" to d.dataType,
            "default_label" to d.defaultLabel,
            "label_override" to d.labelOverride,
            "default_sensitivity" to d.defaultSensitivity,
            "allows_multiple" to d.allowsMultiple,
            "is_user_defined" to d.isUserDefined,
            "is_hidden" to d.isHidden,
            "sort_order" to d.sortOrder,
            "parts" to d.parts.map { mapOf("key" to it.key, "sensitivity" to it.sensitivity) },
        )

    private fun entityJson(e: SnapshotEntity): Map<String, Any?> =
        mapOf(
            "id" to e.id,
            "kind" to e.kind,
            "display_name" to e.displayName,
            "is_favorite" to e.isFavorite,
            "primary_photo_id" to e.primaryPhotoId,
            "created_at" to e.createdAt,
            "updated_at" to e.updatedAt,
            "deleted_at" to e.deletedAt,
        )

    private fun valueJson(v: SnapshotValue): Map<String, Any?> =
        mapOf(
            "id" to v.id,
            "entity_id" to v.entityId,
            "definition_id" to v.definitionId,
            "label" to v.label,
            "value" to v.value,
            "note" to v.note,
            "is_primary" to v.isPrimary,
            "sort_order" to v.sortOrder,
            "sensitivity" to v.sensitivity,
            "sharing_policy" to v.sharingPolicy,
            "metadata" to v.metadata,
            "created_at" to v.createdAt,
            "updated_at" to v.updatedAt,
            "parts" to v.parts.map { mapOf("key" to it.key, "value" to it.value, "sensitivity" to it.sensitivity) },
        )

    private fun relationshipJson(r: SnapshotRelationship): Map<String, Any?> =
        mapOf(
            "id" to r.id,
            "pair_id" to r.pairId,
            "from_entity_id" to r.fromEntityId,
            "to_entity_id" to r.toEntityId,
            "type_id" to r.typeId,
            "note" to r.note,
            "created_at" to r.createdAt,
            "updated_at" to r.updatedAt,
        )

    // ---- JSON to records, with validation ----

    private fun definitionFrom(m: Map<String, Any?>) =
        SnapshotDefinition(
            id = m.str("id"),
            systemKey = m.strOrNull("system_key"),
            section = m.int("section"),
            dataType = m.int("data_type"),
            defaultLabel = m.str("default_label"),
            labelOverride = m.strOrNull("label_override"),
            defaultSensitivity = m.level("default_sensitivity"),
            allowsMultiple = m.bool("allows_multiple"),
            isUserDefined = m.bool("is_user_defined"),
            isHidden = m.bool("is_hidden"),
            sortOrder = m.int("sort_order"),
            parts =
                m
                    .list(
                        "parts",
                    ).map { SnapshotDefinitionPart(it.asMap().str("key"), it.asMap().level("sensitivity")) },
        )

    private fun entityFrom(m: Map<String, Any?>) =
        SnapshotEntity(
            id = m.str("id"),
            kind = m.int("kind").also { if (it !in 0..1) throw BackupFormatException("Bad entity kind") },
            displayName = m.str("display_name"),
            isFavorite = m.bool("is_favorite"),
            primaryPhotoId = m.strOrNull("primary_photo_id"),
            createdAt = m.long("created_at"),
            updatedAt = m.long("updated_at"),
            deletedAt = m.longOrNull("deleted_at"),
        )

    private fun valueFrom(m: Map<String, Any?>): SnapshotValue {
        val parts =
            m.list("parts").map {
                SnapshotPart(it.asMap().str("key"), it.asMap().strOrNull("value"), it.asMap().level("sensitivity"))
            }
        if (parts.map { it.key }.toSet().size != parts.size) throw BackupFormatException("A value repeats a part")
        return SnapshotValue(
            id = m.str("id"),
            entityId = m.str("entity_id"),
            definitionId = m.str("definition_id"),
            label = m.strOrNull("label"),
            value = m.strOrNull("value"),
            note = m.strOrNull("note"),
            isPrimary = m.bool("is_primary"),
            sortOrder = m.int("sort_order"),
            sensitivity = m.level("sensitivity"),
            sharingPolicy =
                m.int("sharing_policy").also {
                    if (it !in
                        0..3
                    ) {
                        throw BackupFormatException("Bad sharing policy")
                    }
                },
            metadata = m.strOrNull("metadata"),
            createdAt = m.long("created_at"),
            updatedAt = m.long("updated_at"),
            parts = parts,
        )
    }

    private fun relationshipFrom(m: Map<String, Any?>) =
        SnapshotRelationship(
            id = m.str("id"),
            pairId = m.str("pair_id"),
            fromEntityId = m.str("from_entity_id"),
            toEntityId = m.str("to_entity_id"),
            typeId = m.str("type_id"),
            note = m.strOrNull("note"),
            createdAt = m.long("created_at"),
            updatedAt = m.long("updated_at"),
        )

    private fun Any?.asMap(): Map<String, Any?> {
        val map = this as? Map<*, *> ?: throw BackupFormatException("Expected an object")
        return map.entries.associate { (key, value) -> key as String to value }
    }

    private fun Map<String, Any?>.str(key: String): String =
        this[key] as? String ?: throw BackupFormatException("Missing text field '$key'")

    private fun Map<String, Any?>.strOrNull(key: String): String? =
        if (!containsKey(key)) {
            throw BackupFormatException("Missing field '$key'")
        } else {
            this[key]?.let {
                it as? String
                    ?: throw BackupFormatException("Bad field '$key'")
            }
        }

    private fun Map<String, Any?>.long(key: String): Long =
        this[key] as? Long ?: throw BackupFormatException("Missing number field '$key'")

    private fun Map<String, Any?>.longOrNull(key: String): Long? =
        if (!containsKey(key)) {
            throw BackupFormatException("Missing field '$key'")
        } else {
            this[key]?.let {
                it as? Long
                    ?: throw BackupFormatException("Bad field '$key'")
            }
        }

    private fun Map<String, Any?>.int(key: String): Int {
        val value = long(key)
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) throw BackupFormatException("Number out of range in '$key'")
        return value.toInt()
    }

    private fun Map<String, Any?>.level(key: String): Int =
        int(key).also {
            if (it !in
                0..3
            ) {
                throw BackupFormatException("Bad protection level in '$key'")
            }
        }

    private fun Map<String, Any?>.bool(key: String): Boolean =
        this[key] as? Boolean ?: throw BackupFormatException("Missing true/false field '$key'")

    private fun Map<String, Any?>.list(key: String): List<Any?> =
        this[key] as? List<*> ?: throw BackupFormatException("Missing list field '$key'")
}
