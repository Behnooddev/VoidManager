package io.github.behnooddev.voidmanager.core.common.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggerTest {
    private class RecordingSink : LogSink {
        val lines = mutableListOf<Triple<LogLevel, String, String>>()

        override fun write(
            level: LogLevel,
            tag: String,
            message: String,
        ) {
            lines += Triple(level, tag, message)
        }
    }

    @Test
    fun messagesBelowMinimumLevelAreNotBuilt() {
        val sink = RecordingSink()
        val logger = Logger(sink, LogLevel.WARN)
        var built = false

        logger.debug("t") {
            built = true
            "hidden"
        }
        logger.info("t") { "hidden" }

        assertFalse(built)
        assertTrue(sink.lines.isEmpty())
    }

    @Test
    fun messagesAtOrAboveMinimumLevelAreWritten() {
        val sink = RecordingSink()
        val logger = Logger(sink, LogLevel.INFO)

        logger.info("db") { "opened" }
        logger.warn("db") { "slow" }

        assertEquals(
            listOf(
                Triple(LogLevel.INFO, "db", "opened"),
                Triple(LogLevel.WARN, "db", "slow"),
            ),
            sink.lines,
        )
    }

    @Test
    fun errorAppendsOnlyTheExceptionClassName() {
        val sink = RecordingSink()
        val logger = Logger(sink, LogLevel.DEBUG)

        logger.error("io", IllegalStateException("card 4111111111111111")) { "write failed" }

        val line = sink.lines.single().third
        assertEquals("write failed [IllegalStateException]", line)
        assertFalse(line.contains("4111"))
    }

    @Test
    fun redactedValueNeverAppearsInText() {
        val secret = Redacted("correct horse battery staple")

        val text = "password=$secret"

        assertEquals("password=[redacted]", text)
        assertFalse(text.contains("horse"))
    }

    @Test
    fun redactedStillExposesValueToCode() {
        assertEquals("abc", Redacted("abc").value)
    }
}
