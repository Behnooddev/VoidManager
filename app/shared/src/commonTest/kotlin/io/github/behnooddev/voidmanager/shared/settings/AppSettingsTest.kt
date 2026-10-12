package io.github.behnooddev.voidmanager.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun theDefaultsMatchTheDocumentedValues() {
        val settings = AppSettings()
        assertEquals(60_000L, settings.idleLockMillis)
        assertEquals(20_000L, settings.revealMillis)
        assertEquals(30_000L, settings.clipboardClearMillis)
        assertEquals(60_000L, settings.reauthWindowMillis)
    }

    @Test
    fun everyDefaultIsOneOfItsChoices() {
        assertTrue(AppSettings.DEFAULT_IDLE_LOCK in AppSettings.IDLE_LOCK_CHOICES)
        assertTrue(AppSettings.DEFAULT_REVEAL in AppSettings.REVEAL_CHOICES)
        assertTrue(AppSettings.DEFAULT_CLIPBOARD_CLEAR in AppSettings.CLIPBOARD_CLEAR_CHOICES)
        assertTrue(AppSettings.DEFAULT_REAUTH_WINDOW in AppSettings.REAUTH_WINDOW_CHOICES)
    }

    @Test
    fun settingsSurviveAWriteAndRead() {
        val settings =
            AppSettings(
                idleLockSeconds = 300,
                revealSeconds = 10,
                clipboardClearSeconds = 120,
                reauthWindowSeconds = 30,
            )
        assertEquals(settings, AppSettings.fromMap(settings.toMap()))
    }

    @Test
    fun missingUnreadableOrUnlistedValuesFallBackToTheDefaults() {
        assertEquals(AppSettings(), AppSettings.fromMap(emptyMap()))
        val damaged =
            mapOf(
                "idle_lock_seconds" to "banana",
                "reveal_seconds" to "0",
                "clipboard_clear_seconds" to "86400",
                "reauth_window_seconds" to "-5",
            )
        assertEquals(AppSettings(), AppSettings.fromMap(damaged))
    }

    @Test
    fun aValidValueIsKeptWhenAnotherIsDamaged() {
        val settings = AppSettings.fromMap(mapOf("reveal_seconds" to "60", "idle_lock_seconds" to "7"))
        assertEquals(60, settings.revealSeconds)
        assertEquals(AppSettings.DEFAULT_IDLE_LOCK, settings.idleLockSeconds)
    }
}
