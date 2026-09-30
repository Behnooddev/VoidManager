package io.github.behnooddev.voidmanager.core.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidV7GeneratorTest {
    private val fixed = UuidV7Generator(clock = { 1_700_000_000_123L }, random = Random(7))

    @Test
    fun hasCanonicalShape() {
        val id = fixed.next()

        assertEquals(36, id.length)
        assertEquals(listOf(8, 4, 4, 4, 12), id.split('-').map { it.length })
    }

    @Test
    fun carriesVersionSevenAndRfcVariant() {
        repeat(200) {
            val id = fixed.next()
            assertEquals('7', id[14])
            assertTrue(id[19] in "89ab")
        }
    }

    @Test
    fun encodesTheTimestampInTheFirstFortyEightBits() {
        val id = fixed.next()
        val hex = id.substring(0, 8) + id.substring(9, 13)

        assertEquals(1_700_000_000_123L, hex.toLong(16))
    }

    @Test
    fun laterTimestampsSortAfterEarlierOnes() {
        var now = 1_000L
        val generator = UuidV7Generator(clock = { now })
        val ids =
            (0 until 50).map {
                now += 1
                generator.next()
            }

        assertEquals(ids.sorted(), ids)
    }

    @Test
    fun identifiersAreUnique() {
        val generator = UuidV7Generator(clock = { 5L })

        val ids = (0 until 5_000).map { generator.next() }.toSet()

        assertEquals(5_000, ids.size)
    }
}
