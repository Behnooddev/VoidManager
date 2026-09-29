package io.github.behnooddev.voidmanager.shared

import io.github.behnooddev.voidmanager.core.designsystem.components.VmIconKind

/** Primary navigation destinations, in display order. */
enum class Destination(
    val icon: VmIconKind,
) {
    Home(VmIconKind.Home),
    People(VmIconKind.People),
    Personal(VmIconKind.Personal),
    Settings(VmIconKind.Settings),
}
