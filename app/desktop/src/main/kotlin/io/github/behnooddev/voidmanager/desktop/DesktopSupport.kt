package io.github.behnooddev.voidmanager.desktop

import io.github.behnooddev.voidmanager.shared.secrets.SecretClipboard
import io.github.behnooddev.voidmanager.shared.settings.SettingsStore
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.util.Properties
import java.util.Timer
import kotlin.concurrent.schedule

/** Copies to the system clipboard and, for a sensitive copy, clears it later if it still holds the same text. */
class JvmSecretClipboard : SecretClipboard {
    private var timer: Timer? = null

    override fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterMillis: Long,
    ): Boolean {
        timer?.cancel()
        timer = null
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            clipboard.setContents(StringSelection(text), null)
        } catch (e: IllegalStateException) {
            return false
        }
        if (sensitive) {
            timer =
                Timer(true).also { scheduled ->
                    scheduled.schedule(clearAfterMillis) { clearIfSame(text) }
                }
        }
        return true
    }

    private fun clearIfSame(text: String) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val current = clipboard.getData(DataFlavor.stringFlavor) as? String
            if (current == text) clipboard.setContents(StringSelection(""), null)
        } catch (e: Exception) {
            // The clipboard held something else, or is not available; nothing is cleared.
        }
    }
}

/** Settings in a properties file next to the vault. They are not personal data. */
class PropertiesSettingsStore(
    private val file: File,
) : SettingsStore {
    override fun read(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val properties = Properties()
        try {
            file.inputStream().use { properties.load(it) }
        } catch (e: IOException) {
            return emptyMap()
        }
        return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
    }

    override fun write(values: Map<String, String>) {
        val properties = Properties()
        for ((key, value) in values) properties.setProperty(key, value)
        file.outputStream().use { properties.store(it, null) }
    }
}
