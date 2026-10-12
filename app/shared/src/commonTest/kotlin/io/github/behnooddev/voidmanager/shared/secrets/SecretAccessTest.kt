package io.github.behnooddev.voidmanager.shared.secrets

import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.shared.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretAccessTest {
    private var now = 0L
    private var settings = AppSettings(revealSeconds = 20, reauthWindowSeconds = 60, clipboardClearSeconds = 30)
    private val access = SecretAccess({ settings }, { now })

    @Test
    fun levelsBelowThreeNeedNoReauthentication() {
        for (level in listOf(Sensitivity.Public, Sensitivity.Personal, Sensitivity.Sensitive)) {
            assertEquals(Access.Granted, access.accessFor(level), level.name)
        }
    }

    @Test
    fun levelThreeNeedsAFreshAuthentication() {
        assertEquals(Access.NeedsReauth, access.accessFor(Sensitivity.Secret))
        access.recordReauthentication()
        assertEquals(Access.Granted, access.accessFor(Sensitivity.Secret))
        now += 59_999
        assertEquals(Access.Granted, access.accessFor(Sensitivity.Secret))
        now += 1
        assertEquals(Access.NeedsReauth, access.accessFor(Sensitivity.Secret))
    }

    @Test
    fun aRevealedValueHidesAfterTheRevealTimeAndOnlyOneIsShownAtATime() {
        access.reveal("a")
        access.reveal("b")
        assertFalse(access.isRevealed("a"))
        assertTrue(access.isRevealed("b"))
        now += 19_999
        assertTrue(access.isRevealed("b"))
        now += 1
        assertFalse(access.isRevealed("b"))
    }

    @Test
    fun clearForgetsTheAuthenticationAndWhatWasRevealed() {
        access.recordReauthentication()
        access.reveal("a")
        access.clear()
        assertFalse(access.isRevealed("a"))
        assertEquals(Access.NeedsReauth, access.accessFor(Sensitivity.Secret))
    }

    @Test
    fun aSettingsChangeAppliesAtOnce() {
        access.reveal("a")
        settings = settings.copy(revealSeconds = 60)
        access.reveal("a")
        now += 30_000
        assertTrue(access.isRevealed("a"), "the longer time applies after the change")
        assertEquals(30, access.clipboardClearSeconds)
        settings = settings.copy(clipboardClearSeconds = 120)
        assertEquals(120_000L, access.clipboardClearMillis)
    }
}
