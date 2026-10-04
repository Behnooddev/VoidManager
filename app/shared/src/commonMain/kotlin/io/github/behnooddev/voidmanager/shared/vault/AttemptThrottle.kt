package io.github.behnooddev.voidmanager.shared.vault

/**
 * Slows repeated wrong passwords down after a few free attempts: 5 seconds after the fifth
 * failure, doubling with each further failure, capped at five minutes.
 *
 * This is a usability measure, not a security boundary. The counter lives in memory and restarts
 * with the process, and an attacker holding a copy of the files attacks the key file offline, where
 * only the cost of the password hash and the strength of the password matter.
 */
class AttemptThrottle(
    private val monotonicMillis: () -> Long,
) {
    private var failures = 0
    private var blockedUntil = 0L

    fun remainingMillis(): Long = maxOf(0L, blockedUntil - monotonicMillis())

    fun recordFailure() {
        failures++
        blockedUntil = monotonicMillis() + delayAfter(failures)
    }

    fun recordSuccess() {
        failures = 0
        blockedUntil = 0L
    }

    companion object {
        const val FREE_ATTEMPTS = 4
        const val BASE_DELAY_MILLIS = 5_000L
        const val MAX_DELAY_MILLIS = 300_000L
        private const val MAX_DOUBLINGS = 16

        /** The wait imposed after [failures] consecutive wrong passwords. */
        fun delayAfter(failures: Int): Long {
            if (failures <= FREE_ATTEMPTS) return 0L
            val doublings = minOf(failures - FREE_ATTEMPTS - 1, MAX_DOUBLINGS)
            return minOf(BASE_DELAY_MILLIS shl doublings, MAX_DELAY_MILLIS)
        }
    }
}
