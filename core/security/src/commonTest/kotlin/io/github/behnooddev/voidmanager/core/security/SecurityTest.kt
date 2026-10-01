package io.github.behnooddev.voidmanager.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityTest {
    private var now = 0L
    private val clock = { now }

    @Test
    fun shortPasswordsAreRejected() {
        val result = PasswordPolicy().assess("Short1!")

        assertTrue(PasswordIssue.TooShort in result.issues)
        assertFalse(result.isAcceptable)
    }

    @Test
    fun commonPasswordsAreRejectedRegardlessOfCase() {
        val result = PasswordPolicy(minLength = 8).assess("PassWord123")

        assertTrue(PasswordIssue.CommonPassword in result.issues)
        assertEquals(PasswordStrength.Weak, result.strength)
    }

    @Test
    fun repeatedCharacterAndSequencesAreFlagged() {
        val policy = PasswordPolicy()

        assertTrue(PasswordIssue.SingleRepeatedCharacter in policy.assess("aaaaaaaaaaaaaaaa").issues)
        assertTrue(PasswordIssue.SimpleSequence in policy.assess("abcdefghijklmnop").issues)
        assertTrue(
            PasswordIssue.SimpleSequence in policy.assess("9876543210").issues,
        )
    }

    @Test
    fun aLongMixedPasswordIsAcceptedAndStrong() {
        val result = PasswordPolicy().assess("blue-Harbor-17-lantern-Quilt!")

        assertTrue(result.isAcceptable)
        assertTrue(result.strength == PasswordStrength.Strong || result.strength == PasswordStrength.Good)
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        val policy = PasswordPolicy(minLength = 8)

        val short = policy.assess("abcdefgh1").estimatedBits
        val longer = policy.assess("abcdefgh1XyzQrst").estimatedBits

        assertTrue(longer > short)
    }

    @Test
    fun nonAsciiLettersCountAsAnotherClass() {
        val policy = PasswordPolicy()

        val latin = policy.assess("kalemekhoobebede").estimatedBits
        val mixed = policy.assess("kalemekhoobebe\u062F\u06CC").estimatedBits

        assertTrue(mixed > latin)
    }

    @Test
    fun policyCannotBeConfiguredBelowTheAbsoluteMinimum() {
        assertFailsWith<IllegalArgumentException> { PasswordPolicy(minLength = 4) }
    }

    @Test
    fun idleTimeoutLocksAfterTheConfiguredTime() {
        val controller = AutoLockController({ AutoLockPolicy(idleTimeoutMillis = 30_000) }, clock)

        now = 29_999
        assertFalse(controller.shouldLock())
        now = 30_000
        assertTrue(controller.shouldLock())
    }

    @Test
    fun interactionRestartsTheIdleTimer() {
        val controller = AutoLockController({ AutoLockPolicy(idleTimeoutMillis = 30_000) }, clock)

        now = 20_000
        controller.onInteraction()
        now = 45_000

        assertFalse(controller.shouldLock())
    }

    @Test
    fun disabledIdleTimerNeverLocksOnItsOwn() {
        val controller = AutoLockController({ AutoLockPolicy(idleTimeoutMillis = null) }, clock)

        now = 10_000_000

        assertFalse(controller.shouldLock())
    }

    @Test
    fun backgroundLocksImmediatelyWithNoGracePeriod() {
        val controller =
            AutoLockController({ AutoLockPolicy(lockOnBackground = true, backgroundGraceMillis = 0) }, clock)

        controller.onBackgrounded()

        assertTrue(controller.shouldLock())
    }

    @Test
    fun backgroundGracePeriodIsHonored() {
        val controller =
            AutoLockController(
                { AutoLockPolicy(idleTimeoutMillis = null, lockOnBackground = true, backgroundGraceMillis = 10_000) },
                clock,
            )

        controller.onBackgrounded()
        now = 5_000
        assertFalse(controller.onForegrounded())

        controller.onBackgrounded()
        now = 20_000
        assertTrue(controller.onForegrounded())
    }

    @Test
    fun backgroundLockCanBeSwitchedOff() {
        val controller =
            AutoLockController(
                { AutoLockPolicy(idleTimeoutMillis = null, lockOnBackground = false) },
                clock,
            )

        controller.onBackgrounded()
        now = 999_999

        assertFalse(controller.onForegrounded())
    }

    @Test
    fun resetStartsFreshTimers() {
        val controller = AutoLockController({ AutoLockPolicy(idleTimeoutMillis = 1_000) }, clock)
        now = 5_000
        assertTrue(controller.shouldLock())

        controller.reset()

        assertFalse(controller.shouldLock())
    }

    @Test
    fun nonPositiveTimeoutsAreRejected() {
        assertFailsWith<IllegalArgumentException> { AutoLockPolicy(idleTimeoutMillis = 0) }
        assertFailsWith<IllegalArgumentException> { AutoLockPolicy(backgroundGraceMillis = -1) }
    }

    @Test
    fun reAuthenticationExpires() {
        val window = ReAuthWindow(windowMillis = 60_000, monotonicMillis = clock)
        assertFalse(window.isFresh())

        window.recordAuthentication()
        now = 59_999
        assertTrue(window.isFresh())
        now = 60_000
        assertFalse(window.isFresh())
    }

    @Test
    fun clearingTheReAuthWindowRequiresANewAuthentication() {
        val window = ReAuthWindow(windowMillis = 60_000, monotonicMillis = clock)
        window.recordAuthentication()

        window.clear()

        assertFalse(window.isFresh())
    }

    @Test
    fun aRevealedSecretHidesItselfAfterTheTimeout() {
        val controller = RevealController(hideAfterMillis = 15_000, monotonicMillis = clock)

        controller.reveal("acc-1")
        now = 14_999
        assertTrue(controller.isRevealed("acc-1"))
        now = 15_000
        assertFalse(controller.isRevealed("acc-1"))
    }

    @Test
    fun revealingAnotherSecretHidesThePreviousOne() {
        val controller = RevealController(hideAfterMillis = 15_000, monotonicMillis = clock)

        controller.reveal("a")
        controller.reveal("b")

        assertFalse(controller.isRevealed("a"))
        assertTrue(controller.isRevealed("b"))
    }

    @Test
    fun hideAllClearsTheRevealedSecret() {
        val controller = RevealController(hideAfterMillis = 15_000, monotonicMillis = clock)
        controller.reveal("a")

        controller.hideAll()

        assertFalse(controller.isRevealed("a"))
    }
}
