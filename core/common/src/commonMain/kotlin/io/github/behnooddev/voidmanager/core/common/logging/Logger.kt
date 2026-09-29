package io.github.behnooddev.voidmanager.core.common.logging

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * Destination for log lines. Platform modules provide the implementation.
 * The sink receives no throwable: exception messages can carry user data, so only the
 * exception class name is passed on (see [Logger.error]).
 */
fun interface LogSink {
    fun write(
        level: LogLevel,
        tag: String,
        message: String,
    )
}

/**
 * The only logging entry point for application code.
 * Messages are lambdas so nothing is built for levels that are switched off.
 */
class Logger(
    private val sink: LogSink,
    private val minLevel: LogLevel,
) {
    fun debug(
        tag: String,
        message: () -> String,
    ) = log(LogLevel.DEBUG, tag, message)

    fun info(
        tag: String,
        message: () -> String,
    ) = log(LogLevel.INFO, tag, message)

    fun warn(
        tag: String,
        message: () -> String,
    ) = log(LogLevel.WARN, tag, message)

    fun error(
        tag: String,
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (LogLevel.ERROR < minLevel) return
        val suffix = throwable?.let { " [${it::class.simpleName ?: "Throwable"}]" }.orEmpty()
        sink.write(LogLevel.ERROR, tag, message() + suffix)
    }

    private fun log(
        level: LogLevel,
        tag: String,
        message: () -> String,
    ) {
        if (level < minLevel) return
        sink.write(level, tag, message())
    }

    companion object {
        /** A logger that discards everything. */
        val disabled = Logger(sink = { _, _, _ -> }, minLevel = LogLevel.ERROR)
    }
}
