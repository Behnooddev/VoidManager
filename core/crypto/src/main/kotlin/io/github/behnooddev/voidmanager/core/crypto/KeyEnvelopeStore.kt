package io.github.behnooddev.voidmanager.core.crypto

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface KeyEnvelopeStore {
    fun read(): ByteArray?

    fun write(bytes: ByteArray)
}

/** Stores the key file next to the database. A write either completes or leaves the old file intact. */
class FileKeyEnvelopeStore(
    private val file: File,
) : KeyEnvelopeStore {
    override fun read(): ByteArray? = if (file.exists()) file.readBytes() else null

    override fun write(bytes: ByteArray) {
        val parent = file.absoluteFile.parentFile
        parent?.mkdirs()
        val temp = File(parent, file.name + ".tmp")
        FileOutputStream(temp).use {
            it.write(bytes)
            it.fd.sync()
        }
        Files.move(
            temp.toPath(),
            file.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
