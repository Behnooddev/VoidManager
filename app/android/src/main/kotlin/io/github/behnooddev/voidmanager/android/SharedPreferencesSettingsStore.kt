package io.github.behnooddev.voidmanager.android

import android.content.Context
import io.github.behnooddev.voidmanager.shared.settings.SettingsStore

/** App settings in a private preferences file. They are not personal data and are readable before unlock. */
class SharedPreferencesSettingsStore(
    context: Context,
) : SettingsStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(): Map<String, String> =
        preferences.all.entries
            .mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
            .toMap()

    override fun write(values: Map<String, String>) {
        val editor = preferences.edit()
        for ((key, value) in values) editor.putString(key, value)
        editor.apply()
    }

    private companion object {
        const val FILE_NAME = "voidmanager_settings"
    }
}
