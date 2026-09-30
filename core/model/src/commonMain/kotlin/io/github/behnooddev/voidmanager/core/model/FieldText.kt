package io.github.behnooddev.voidmanager.core.model

/**
 * Text read from storage. Values of protected sensitivity levels print as a mask, so an accidental
 * log line or string template cannot leak them. Reading the value is an explicit call to [reveal].
 */
class FieldText(
    private val raw: String,
    val isProtected: Boolean,
) {
    fun reveal(): String = raw

    override fun toString(): String = if (isProtected) MASK else raw

    override fun equals(other: Any?): Boolean =
        other is FieldText && other.raw == raw && other.isProtected == isProtected

    override fun hashCode(): Int = 31 * raw.hashCode() + isProtected.hashCode()

    companion object {
        const val MASK = "[redacted]"
    }
}
