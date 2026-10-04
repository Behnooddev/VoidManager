package io.github.behnooddev.voidmanager.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KdfCalibratorTest {
    private val base = KdfParams.DEFAULT

    /** A device where one iteration at the default memory takes [perIterationMillis]. */
    private fun device(perIterationMillis: Long): (KdfParams) -> Long =
        { params -> perIterationMillis * params.iterations * params.memoryKiB / base.memoryKiB }

    @Test
    fun aDeviceNearTheTargetKeepsTheDefaultCost() {
        // 3 iterations x 230 ms = 690 ms, already at the 700 ms target.
        assertEquals(base, KdfCalibrator.calibrate(device(230)))
    }

    @Test
    fun aFastDeviceGetsMoreIterationsUpToTheCap() {
        val fast = KdfCalibrator.calibrate(device(100))
        assertEquals(7, fast.iterations)
        assertEquals(base.memoryKiB, fast.memoryKiB)

        val veryFast = KdfCalibrator.calibrate(device(10))
        assertEquals(KdfParams.MAX_ITERATIONS, veryFast.iterations)
    }

    @Test
    fun aSlowDeviceDropsToTheMinimumIterationsButKeepsTheMemory() {
        val slow = KdfCalibrator.calibrate(device(1_000))
        assertEquals(KdfParams.MIN_ITERATIONS, slow.iterations)
        assertEquals(base.memoryKiB, slow.memoryKiB)
    }

    @Test
    fun aVerySlowDeviceAlsoLowersMemoryButNeverBelowTheFloor() {
        val verySlow = KdfCalibrator.calibrate(device(3_000))
        assertEquals(KdfParams.MIN_ITERATIONS, verySlow.iterations)
        assertTrue(verySlow.memoryKiB < base.memoryKiB)
        assertTrue(verySlow.memoryKiB >= KdfParams.MIN_MEMORY_KIB)

        val hopeless = KdfCalibrator.calibrate(device(1_000_000))
        assertEquals(KdfParams.FLOOR, hopeless)
    }

    @Test
    fun theChosenMemoryIsAWholeNumberOfMebibytes() {
        val params = KdfCalibrator.calibrate(device(3_000))
        assertEquals(0, params.memoryKiB % 1024)
    }

    @Test
    fun aZeroMeasurementDoesNotDivideByZero() {
        assertEquals(KdfParams.MAX_ITERATIONS, KdfCalibrator.calibrate({ 0L }).iterations)
    }

    @Test
    fun theChosenParametersAreAlwaysAcceptedByTheKeyFileRules() {
        for (perIteration in listOf(1L, 50L, 233L, 800L, 2_500L, 9_999L, 500_000L)) {
            val params = KdfCalibrator.calibrate(device(perIteration))
            assertTrue(params.memoryKiB in KdfParams.MIN_MEMORY_KIB..KdfParams.MAX_MEMORY_KIB)
            assertTrue(params.iterations in KdfParams.MIN_ITERATIONS..KdfParams.MAX_ITERATIONS)
        }
    }
}
