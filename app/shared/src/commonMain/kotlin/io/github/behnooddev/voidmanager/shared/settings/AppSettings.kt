package io.github.behnooddev.voidmanager.shared.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Settings that are not personal data. They are stored outside the vault, so they can be read
 * before unlock, and every value must be one of a fixed set of choices.
 */
data class AppSettings(
    val idleLockSeconds: Int = DEFAULT_IDLE_LOCK,
    val revealSeconds: Int = DEFAULT_REVEAL,
    val clipboardClearSeconds: Int = DEFAULT_CLIPBOARD_CLEAR,
    val reauthWindowSeconds: Int = DEFAULT_REAUTH_WINDOW,
) {
    val idleLockMillis: Long get() = idleLockSeconds * MILLIS_PER_SECOND
    val revealMillis: Long get() = revealSeconds * MILLIS_PER_SECOND
    val clipboardClearMillis: Long get() = clipboardClearSeconds * MILLIS_PER_SECOND
    val reauthWindowMillis: Long get() = reauthWindowSeconds * MILLIS_PER_SECOND

    fun toMap(): Map<String, String> =
        mapOf(
            KEY_IDLE_LOCK to idleLockSeconds.toString(),
            KEY_REVEAL to revealSeconds.toString(),
            KEY_CLIPBOARD_CLEAR to clipboardClearSeconds.toString(),
            KEY_REAUTH_WINDOW to reauthWindowSeconds.toString(),
        )

    companion object {
        const val DEFAULT_IDLE_LOCK = 60
        const val DEFAULT_REVEAL = 20
        const val DEFAULT_CLIPBOARD_CLEAR = 30
        const val DEFAULT_REAUTH_WINDOW = 60
        private const val MILLIS_PER_SECOND = 1_000L

        val IDLE_LOCK_CHOICES = listOf(30, 60, 120, 300)
        val REVEAL_CHOICES = listOf(10, 20, 30, 60)
        val CLIPBOARD_CLEAR_CHOICES = listOf(15, 30, 60, 120)
        val REAUTH_WINDOW_CHOICES = listOf(30, 60, 300)

        private const val KEY_IDLE_LOCK = "idle_lock_seconds"
        private const val KEY_REVEAL = "reveal_seconds"
        private const val KEY_CLIPBOARD_CLEAR = "clipboard_clear_seconds"
        private const val KEY_REAUTH_WINDOW = "reauth_window_seconds"

        /** A missing, unreadable or unlisted value falls back to its default, so a damaged file cannot disable a protection. */
        fun fromMap(values: Map<String, String>): AppSettings =
            AppSettings(
                idleLockSeconds = pick(values[KEY_IDLE_LOCK], IDLE_LOCK_CHOICES, DEFAULT_IDLE_LOCK),
                revealSeconds = pick(values[KEY_REVEAL], REVEAL_CHOICES, DEFAULT_REVEAL),
                clipboardClearSeconds =
                    pick(
                        values[KEY_CLIPBOARD_CLEAR],
                        CLIPBOARD_CLEAR_CHOICES,
                        DEFAULT_CLIPBOARD_CLEAR,
                    ),
                reauthWindowSeconds = pick(values[KEY_REAUTH_WINDOW], REAUTH_WINDOW_CHOICES, DEFAULT_REAUTH_WINDOW),
            )

        private fun pick(
            text: String?,
            choices: List<Int>,
            default: Int,
        ): Int = text?.toIntOrNull()?.takeIf { it in choices } ?: default
    }
}

/** Where the settings live. The platform decides how; the values are plain text. */
interface SettingsStore {
    fun read(): Map<String, String>

    fun write(values: Map<String, String>)
}

/** The settings in use, observable by the screens. A change is saved at once. */
class AppSettingsHolder(
    private val store: SettingsStore,
) {
    var current: AppSettings by mutableStateOf(AppSettings.fromMap(readQuietly()))
        private set

    fun update(change: (AppSettings) -> AppSettings) {
        // Going through the map drops any value that is not one of the choices.
        val next = AppSettings.fromMap(change(current).toMap())
        current = next
        try {
            store.write(next.toMap())
        } catch (e: Exception) {
            // The new value stays in effect for this run; it is saved again with the next change.
        }
    }

    private fun readQuietly(): Map<String, String> =
        try {
            store.read()
        } catch (e: Exception) {
            emptyMap()
        }
}
