package io.github.behnooddev.voidmanager.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.behnooddev.voidmanager.shared.App

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "VoidManager",
            state = rememberWindowState(size = DpSize(1100.dp, 760.dp)),
        ) {
            App()
        }
    }
