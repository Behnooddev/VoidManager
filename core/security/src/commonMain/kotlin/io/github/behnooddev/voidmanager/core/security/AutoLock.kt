package io.github.behnooddev.voidmanager.core.security

/** When the app locks by itself. A null [idleTimeoutMillis] disables the idle timer. */
data class AutoLockPolicy(
    val idleTimeoutMillis: Long? = DEFAULT_IDLE_MILLIS,
    val lockOnBackground: Boolean = true,
    /** How long the app may stay in the background before it locks. Zero locks immediately. */
    val backgroundGraceMillis: Long = 0L,
) {
    init {
        require(idleTimeoutMillis == null || idleTimeoutMillis > 0) { "The idle timeout must be positive" }
        require(backgroundGraceMillis >= 0) { "The background grace period cannot be negative" }
    }

    companion object {
        const val DEFAULT_IDLE_MILLIS = 60_000L
    }
}

/**
 * Decides when an unlocked session must lock. It holds no timers: the platform calls
 * [shouldLock] on a tick and on foreground events, and locks when it returns true.
 * The clock must be monotonic, so changing the device time cannot extend a session.
 */
class AutoLockController(
    private val policy: () -> AutoLockPolicy,
    private val monotonicMillis: () -> Long,
) {
    private var lastInteraction: Long = monotonicMillis()
    private var backgroundedAt: Long? = null

    /** Call when the vault is unlocked and after every user interaction. */
    fun onInteraction() {
        lastInteraction = monotonicMillis()
    }

    fun onBackgrounded() {
        if (backgroundedAt == null) backgroundedAt = monotonicMillis()
    }

    /** Returns true when the time spent in the background requires a lock. */
    fun onForegrounded(): Boolean {
        val since = backgroundedAt ?: return shouldLock()
        backgroundedAt = null
        val current = policy()
        val away = monotonicMillis() - since
        if (current.lockOnBackground && away >= current.backgroundGraceMillis) return true
        return shouldLock()
    }

    fun shouldLock(): Boolean {
        val current = policy()
        val now = monotonicMillis()
        val since = backgroundedAt
        if (since != null && current.lockOnBackground && now - since >= current.backgroundGraceMillis) return true
        val idle = current.idleTimeoutMillis ?: return false
        return now - lastInteraction >= idle
    }

    /** Call after locking so the next unlock starts with fresh timers. */
    fun reset() {
        lastInteraction = monotonicMillis()
        backgroundedAt = null
    }
}

/**
 * Requires a recent authentication before level 3 values are revealed.
 * [windowMillis] is how long one authentication stays valid.
 */
class ReAuthWindow(
    private val windowMillis: Long,
    private val monotonicMillis: () -> Long,
) {
    private var authenticatedAt: Long? = null

    init {
        require(windowMillis > 0) { "The window must be positive" }
    }

    fun recordAuthentication() {
        authenticatedAt = monotonicMillis()
    }

    fun isFresh(): Boolean {
        val at = authenticatedAt ?: return false
        return monotonicMillis() - at < windowMillis
    }

    fun clear() {
        authenticatedAt = null
    }
}

/** Tracks which secret is on screen and hides it after [hideAfterMillis], or on request. */
class RevealController(
    private val hideAfterMillis: Long,
    private val monotonicMillis: () -> Long,
) {
    private var revealedKey: String? = null
    private var revealedAt: Long = 0L

    init {
        require(hideAfterMillis > 0) { "The reveal time must be positive" }
    }

    /** Only one secret is revealed at a time. */
    fun reveal(key: String) {
        revealedKey = key
        revealedAt = monotonicMillis()
    }

    fun isRevealed(key: String): Boolean {
        if (revealedKey != key) return false
        if (monotonicMillis() - revealedAt >= hideAfterMillis) {
            hideAll()
            return false
        }
        return true
    }

    fun hideAll() {
        revealedKey = null
    }
}
