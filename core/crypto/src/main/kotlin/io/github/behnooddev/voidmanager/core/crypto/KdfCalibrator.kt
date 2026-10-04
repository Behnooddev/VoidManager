package io.github.behnooddev.voidmanager.core.crypto

import java.security.SecureRandom
import kotlin.math.roundToInt

/**
 * Chooses the Argon2id cost for a new vault from one timing measurement on this device.
 *
 * Argon2id time grows linearly with the iteration count, so a single measurement of the default
 * cost predicts the others. Memory stays at the default so that low-end devices keep the same
 * heap demand; only the iteration count moves toward [targetMillis]. If even the lowest iteration
 * count is slower than [ceilingMillis], memory is reduced, never below the OWASP floor.
 *
 * The measurement includes JIT warm-up, so it errs on the slow side and therefore on fewer iterations.
 * The cost is written into the key file, so a vault created here opens on any device.
 */
object KdfCalibrator {
    const val TARGET_MILLIS = 700L
    const val CEILING_MILLIS = 3_000L
    private const val KIB_PER_MIB = 1024

    fun calibrate(
        measureMillis: (KdfParams) -> Long,
        base: KdfParams = KdfParams.DEFAULT,
        targetMillis: Long = TARGET_MILLIS,
        ceilingMillis: Long = CEILING_MILLIS,
    ): KdfParams {
        val perIteration = maxOf(1L, measureMillis(base)).toDouble() / base.iterations
        val wanted =
            (targetMillis / perIteration).roundToInt().coerceIn(
                KdfParams.MIN_ITERATIONS,
                KdfParams.MAX_ITERATIONS,
            )
        val predicted = perIteration * wanted
        if (predicted <= ceilingMillis) return base.copy(iterations = wanted)

        val scaled = (base.memoryKiB * ceilingMillis / predicted).toInt()
        val memory = (scaled / KIB_PER_MIB * KIB_PER_MIB).coerceIn(KdfParams.MIN_MEMORY_KIB, base.memoryKiB)
        return base.copy(memoryKiB = memory, iterations = wanted)
    }

    /** Times one real derivation with throwaway input. */
    fun measureArgon2(params: KdfParams): Long {
        val random = SecureRandom()
        val password = ByteArray(PROBE_PASSWORD_BYTES).also { random.nextBytes(it) }
        val salt = ByteArray(Argon2idKdf.SALT_BYTES).also { random.nextBytes(it) }
        val start = System.nanoTime()
        Argon2idKdf.derive(password, salt, params).fill(0)
        password.fill(0)
        return (System.nanoTime() - start) / NANOS_PER_MILLI
    }

    private const val PROBE_PASSWORD_BYTES = 16
    private const val NANOS_PER_MILLI = 1_000_000L
}
