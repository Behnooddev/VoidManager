package io.github.behnooddev.voidmanager.core.common.logging

/**
 * Wraps a value that must never reach logs, crash output or string templates.
 * The wrapped value is only available through [value]; every textual form is a fixed mask.
 */
class Redacted<out T>(
    val value: T,
) {
    override fun toString(): String = MASK

    companion object {
        const val MASK = "[redacted]"
    }
}
