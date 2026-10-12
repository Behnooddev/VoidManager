package io.github.behnooddev.voidmanager.shared.secrets

/** Copies text to the system clipboard on an explicit user action. */
interface SecretClipboard {
    /**
     * Copies [text]. With [sensitive] set the platform is told to keep it out of previews and
     * history where it can, and the clipboard is cleared after [clearAfterMillis] if it still holds
     * this text. Returns false when the clipboard could not be written. The text is never logged.
     */
    fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterMillis: Long,
    ): Boolean
}
