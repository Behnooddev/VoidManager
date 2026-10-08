package io.github.behnooddev.voidmanager.shared

/**
 * Lets the platform's back action reach the screen that is showing. A screen that can go back
 * registers [handler] while it is visible; the platform calls [dispatch] and does its own default
 * (leaving the app) only when nothing handled the event.
 */
class BackDispatcher {
    var handler: (() -> Boolean)? = null

    /** Returns true when a screen handled the back action. */
    fun dispatch(): Boolean = handler?.invoke() ?: false
}
