package io.github.behnooddev.voidmanager.shared.settings

/** Keeps the settings for this run only. Used when a platform provides no store. */
class MemorySettingsStore : SettingsStore {
    private var values: Map<String, String> = emptyMap()

    override fun read(): Map<String, String> = values

    override fun write(values: Map<String, String>) {
        this.values = values
    }
}
