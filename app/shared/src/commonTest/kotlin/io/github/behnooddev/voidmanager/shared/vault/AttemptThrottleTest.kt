package io.github.behnooddev.voidmanager.shared.vault

import kotlin.test.Test
import kotlin.test.assertEquals

class AttemptThrottleTest {
    private var now = 0L
    private val throttle = AttemptThrottle { now }

    @Test
    fun theFirstFourWrongPasswordsAreNotDelayed() {
        repeat(4) {
            throttle.recordFailure()
            assertEquals(0L, throttle.remainingMillis())
        }
    }

    @Test
    fun theFifthWrongPasswordWaitsFiveSeconds() {
        repeat(5) { throttle.recordFailure() }
        assertEquals(5_000L, throttle.remainingMillis())
        now += 2_000
        assertEquals(3_000L, throttle.remainingMillis())
        now += 3_000
        assertEquals(0L, throttle.remainingMillis())
    }

    @Test
    fun theWaitDoublesAndIsCapped() {
        assertEquals(5_000L, AttemptThrottle.delayAfter(5))
        assertEquals(10_000L, AttemptThrottle.delayAfter(6))
        assertEquals(20_000L, AttemptThrottle.delayAfter(7))
        assertEquals(300_000L, AttemptThrottle.delayAfter(12))
        assertEquals(300_000L, AttemptThrottle.delayAfter(10_000))
    }

    @Test
    fun aSuccessfulUnlockClearsTheCounter() {
        repeat(6) { throttle.recordFailure() }
        throttle.recordSuccess()
        assertEquals(0L, throttle.remainingMillis())
        repeat(4) { throttle.recordFailure() }
        assertEquals(0L, throttle.remainingMillis())
    }
}
