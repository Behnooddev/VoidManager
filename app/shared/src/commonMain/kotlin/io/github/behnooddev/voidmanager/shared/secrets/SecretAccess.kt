package io.github.behnooddev.voidmanager.shared.secrets

import io.github.behnooddev.voidmanager.core.model.Sensitivity
import io.github.behnooddev.voidmanager.core.security.ReAuthWindow
import io.github.behnooddev.voidmanager.core.security.RevealController
import io.github.behnooddev.voidmanager.shared.settings.AppSettings

enum class Access {
    Granted,

    /** The user has to authenticate again (biometrics or the master password) first. */
    NeedsReauth,
}

/**
 * The rules for showing and copying protected values.
 *
 * Level 2 values need only an unlocked vault. Level 3 values also need an authentication within the
 * re-authentication window. Only one value is revealed at a time and it hides itself after the
 * reveal time. The timing comes from [settings] and is read on every call, so a change in the
 * settings applies at once (and clears what is revealed).
 */
class SecretAccess(
    private val settings: () -> AppSettings,
    private val monotonicMillis: () -> Long,
) {
    private var appliedReveal = 0
    private var appliedWindow = 0
    private var reauth = ReAuthWindow(DEFAULT_MILLIS, monotonicMillis)
    private var reveals = RevealController(DEFAULT_MILLIS, monotonicMillis)

    private fun sync() {
        val current = settings()
        if (current.revealSeconds != appliedReveal) {
            reveals = RevealController(current.revealMillis, monotonicMillis)
            appliedReveal = current.revealSeconds
        }
        if (current.reauthWindowSeconds != appliedWindow) {
            reauth = ReAuthWindow(current.reauthWindowMillis, monotonicMillis)
            appliedWindow = current.reauthWindowSeconds
        }
    }

    val clipboardClearMillis: Long get() = settings().clipboardClearMillis

    val clipboardClearSeconds: Int get() = settings().clipboardClearSeconds

    fun accessFor(sensitivity: Sensitivity): Access {
        sync()
        return if (sensitivity.level >= SECRET_LEVEL && !reauth.isFresh()) Access.NeedsReauth else Access.Granted
    }

    fun recordReauthentication() {
        sync()
        reauth.recordAuthentication()
    }

    fun reveal(key: String) {
        sync()
        reveals.reveal(key)
    }

    fun isRevealed(key: String): Boolean {
        sync()
        return reveals.isRevealed(key)
    }

    fun hideAll() {
        sync()
        reveals.hideAll()
    }

    /** Forgets the authentication and hides everything. Called when the vault locks. */
    fun clear() {
        sync()
        reveals.hideAll()
        reauth.clear()
    }

    private companion object {
        const val SECRET_LEVEL = 3
        const val DEFAULT_MILLIS = 1L
    }
}
