package io.github.behnooddev.voidmanager.shared.secrets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.behnooddev.voidmanager.core.model.Sensitivity

/** What the screen says after a copy. */
sealed interface CopyNotice {
    /** [clearAfterSeconds] is null when the text was not protected and so is not cleared. */
    data class Copied(
        val clearAfterSeconds: Int?,
    ) : CopyNotice

    data object Failed : CopyNotice
}

/**
 * The state behind the reveal and copy buttons, with a re-authentication step in between when a
 * level 3 value is involved. One per unlocked vault; it is dropped when the vault locks.
 */
class SecretsModel(
    private val access: SecretAccess,
    private val clipboard: SecretClipboard?,
) {
    /** The key of the value that is shown right now, or null. */
    var revealedKey: String? by mutableStateOf(null)
        private set

    /** True while the screen should ask the user to authenticate before going on. */
    var needsReauth: Boolean by mutableStateOf(false)
        private set

    var notice: CopyNotice? by mutableStateOf(null)
        private set

    private var pending: (() -> Unit)? = null

    val canCopy: Boolean get() = clipboard != null

    fun isRevealed(key: String): Boolean = revealedKey == key

    /** Shows the value, or hides it if it is shown. A level 3 value first needs a fresh authentication. */
    fun toggleReveal(
        key: String,
        sensitivity: Sensitivity,
    ) {
        if (revealedKey == key) {
            hideAll()
            return
        }
        guarded(sensitivity) {
            access.reveal(key)
            revealedKey = key
            notice = null
        }
    }

    /** Copies [text]. A [protected] value is marked sensitive and cleared after a while. */
    fun copy(
        text: String,
        sensitivity: Sensitivity,
        protected: Boolean,
    ) {
        guarded(sensitivity) {
            val written = clipboard?.copy(text, protected, access.clipboardClearMillis) == true
            notice =
                if (written) {
                    CopyNotice.Copied(
                        if (protected) access.clipboardClearSeconds else null,
                    )
                } else {
                    CopyNotice.Failed
                }
        }
    }

    /** Called after the user proved who they are. Goes on with what was waiting. */
    fun reauthenticated() {
        access.recordReauthentication()
        needsReauth = false
        val action = pending
        pending = null
        action?.invoke()
    }

    fun cancelReauth() {
        pending = null
        needsReauth = false
    }

    /** Called about once a second: hides a value whose time is up. */
    fun tick() {
        val key = revealedKey ?: return
        if (!access.isRevealed(key)) revealedKey = null
    }

    fun dismissNotice() {
        notice = null
    }

    fun hideAll() {
        access.hideAll()
        revealedKey = null
    }

    /** Forgets everything: what is shown, the authentication, and any waiting action. */
    fun clear() {
        pending = null
        needsReauth = false
        notice = null
        revealedKey = null
        access.clear()
    }

    private fun guarded(
        sensitivity: Sensitivity,
        action: () -> Unit,
    ) {
        if (access.accessFor(sensitivity) == Access.Granted) {
            action()
        } else {
            pending = action
            needsReauth = true
        }
    }
}
