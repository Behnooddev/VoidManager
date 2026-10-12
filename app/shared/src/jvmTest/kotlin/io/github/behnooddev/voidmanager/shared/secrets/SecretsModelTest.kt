package io.github.behnooddev.voidmanager.shared.secrets

import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.shared.settings.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RecordingClipboard(
    var succeed: Boolean = true,
) : SecretClipboard {
    val copies = mutableListOf<Triple<String, Boolean, Long>>()

    override fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterMillis: Long,
    ): Boolean {
        copies += Triple(text, sensitive, clearAfterMillis)
        return succeed
    }
}

class SecretsModelTest {
    private var now = 0L
    private val clipboard = RecordingClipboard()
    private val model = SecretsModel(SecretAccess({ AppSettings() }, { now }), clipboard)

    @Test
    fun aLevelTwoValueIsRevealedAtOnceAndHidesByItself() {
        model.toggleReveal("v1", Sensitivity.Sensitive)
        assertTrue(model.isRevealed("v1"))
        assertFalse(model.needsReauth)

        now += 20_000
        model.tick()
        assertNull(model.revealedKey)
    }

    @Test
    fun pressingShowAgainHidesTheValue() {
        model.toggleReveal("v1", Sensitivity.Sensitive)
        model.toggleReveal("v1", Sensitivity.Sensitive)
        assertNull(model.revealedKey)
    }

    @Test
    fun aLevelThreeValueWaitsForReauthenticationAndThenGoesAhead() {
        model.toggleReveal("v1:password", Sensitivity.Secret)
        assertNull(model.revealedKey)
        assertTrue(model.needsReauth)

        model.reauthenticated()
        assertFalse(model.needsReauth)
        assertEquals("v1:password", model.revealedKey)

        model.toggleReveal("v1:password", Sensitivity.Secret)
        model.toggleReveal("v1:pin", Sensitivity.Secret)
        assertEquals("v1:pin", model.revealedKey, "the authentication is still fresh")
        assertFalse(model.needsReauth)
    }

    @Test
    fun cancellingTheReauthenticationDropsTheWaitingAction() {
        model.toggleReveal("v1:password", Sensitivity.Secret)
        model.cancelReauth()
        model.reauthenticated()
        assertNull(model.revealedKey, "nothing was waiting any more")
    }

    @Test
    fun copyingAProtectedValueMarksItSensitiveAndReportsWhenItIsCleared() {
        model.copy("5550100", Sensitivity.Sensitive, protected = true)
        assertEquals(listOf(Triple("5550100", true, 30_000L)), clipboard.copies)
        assertEquals(CopyNotice.Copied(clearAfterSeconds = 30), model.notice)
    }

    @Test
    fun copyingAnUnprotectedValueIsNotCleared() {
        model.copy("someone@example.org", Sensitivity.Personal, protected = false)
        assertEquals(CopyNotice.Copied(clearAfterSeconds = null), model.notice)
        assertEquals(false, clipboard.copies.single().second)
    }

    @Test
    fun copyingALevelThreeValueNeedsReauthenticationFirst() {
        model.copy("hunter2", Sensitivity.Secret, protected = true)
        assertTrue(clipboard.copies.isEmpty())
        assertTrue(model.needsReauth)

        model.reauthenticated()
        assertEquals("hunter2", clipboard.copies.single().first)
    }

    @Test
    fun aFailedCopyIsReported() {
        clipboard.succeed = false
        model.copy("x", Sensitivity.Personal, protected = false)
        assertIs<CopyNotice.Failed>(model.notice)
    }

    @Test
    fun clearForgetsRevealsAuthenticationAndNotices() {
        model.toggleReveal("v1:password", Sensitivity.Secret)
        model.reauthenticated()
        model.copy("x", Sensitivity.Personal, protected = false)
        model.clear()
        assertNull(model.revealedKey)
        assertNull(model.notice)
        model.toggleReveal("v1:password", Sensitivity.Secret)
        assertTrue(model.needsReauth, "the authentication was forgotten")
    }

    @Test
    fun withoutAClipboardNothingCanBeCopied() {
        val bare = SecretsModel(SecretAccess({ AppSettings() }, { now }), null)
        assertFalse(bare.canCopy)
        bare.copy("x", Sensitivity.Personal, protected = false)
        assertIs<CopyNotice.Failed>(bare.notice)
    }
}
