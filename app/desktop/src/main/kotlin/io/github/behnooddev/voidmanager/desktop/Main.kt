package io.github.behnooddev.voidmanager.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.behnooddev.voidmanager.core.crypto.FileKeyEnvelopeStore
import io.github.behnooddev.voidmanager.core.crypto.VaultManager
import io.github.behnooddev.voidmanager.core.crypto.VaultService
import io.github.behnooddev.voidmanager.core.database.JvmDriverFactory
import io.github.behnooddev.voidmanager.core.security.AutoLockController
import io.github.behnooddev.voidmanager.core.security.AutoLockPolicy
import io.github.behnooddev.voidmanager.shared.App
import io.github.behnooddev.voidmanager.shared.vault.AttemptThrottle
import io.github.behnooddev.voidmanager.shared.vault.VaultController
import io.github.behnooddev.voidmanager.shared.vault.VaultFlow
import java.io.File

private const val NANOS_PER_MILLI = 1_000_000L

/**
 * The desktop build is a development and testing tool for now. The vault lives in a folder in the
 * user's home directory, and the window has no background state, so only the idle timer locks it.
 */
private fun createController(): VaultController {
    val directory = File(System.getProperty("user.home"), ".voidmanager").apply { mkdirs() }
    val manager = VaultManager(FileKeyEnvelopeStore(File(directory, "vault.keys")))
    val gateway = VaultService(manager, JvmDriverFactory(File(directory, "vault.db")))
    val clock = { System.nanoTime() / NANOS_PER_MILLI }
    val autoLock = AutoLockController(policy = { AutoLockPolicy(lockOnBackground = false) }, monotonicMillis = clock)
    return VaultController(VaultFlow(gateway, autoLock, AttemptThrottle(clock)))
}

fun main() {
    val controller = createController()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "VoidManager",
            state = rememberWindowState(size = DpSize(1100.dp, 760.dp)),
        ) {
            App(controller)
        }
    }
}
