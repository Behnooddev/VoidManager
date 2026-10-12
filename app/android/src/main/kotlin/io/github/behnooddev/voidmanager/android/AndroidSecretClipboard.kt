package io.github.behnooddev.voidmanager.android

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import io.github.behnooddev.voidmanager.shared.secrets.SecretClipboard
import java.util.UUID

/**
 * Copies text to the system clipboard. A sensitive copy is flagged so that Android 13 and later
 * keep it out of the clipboard preview, and is cleared after a delay if the clipboard still holds it.
 *
 * Each copy carries a unique label. When the delay is up the label is compared with the one on the
 * clipboard, so a newer copy from this or any other app is never cleared by mistake. The clipboard
 * text itself is never read, which Android does not allow in the background. The delay runs only
 * while the process is alive; if the app is closed first, the text stays on the clipboard.
 */
class AndroidSecretClipboard(
    context: Context,
) : SecretClipboard {
    private val manager = context.getSystemService(ClipboardManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    override fun copy(
        text: String,
        sensitive: Boolean,
        clearAfterMillis: Long,
    ): Boolean {
        cancelPending()
        val label = "$LABEL_PREFIX${UUID.randomUUID()}"
        val clip = ClipData.newPlainText(label, text)
        if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        try {
            manager.setPrimaryClip(clip)
        } catch (e: RuntimeException) {
            return false
        }
        if (sensitive) {
            val task = Runnable { clearIfStillOurs(label) }
            pending = task
            handler.postDelayed(task, clearAfterMillis)
        }
        return true
    }

    private fun cancelPending() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }

    private fun clearIfStillOurs(label: String) {
        pending = null
        val description = manager.primaryClipDescription ?: return
        if (description.label?.toString() != label) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.clearPrimaryClip()
            } else {
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: RuntimeException) {
            // Nothing more can be done; the text stays until something else is copied.
        }
    }

    private companion object {
        const val LABEL_PREFIX = "voidmanager-"
    }
}
