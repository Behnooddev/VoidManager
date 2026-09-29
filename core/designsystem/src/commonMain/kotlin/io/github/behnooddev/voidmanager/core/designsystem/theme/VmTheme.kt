package io.github.behnooddev.voidmanager.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class VmThemeMode { System, Dark, Light }

private val LocalVmColors = staticCompositionLocalOf { VmColors.dark }
private val LocalVmTypography = staticCompositionLocalOf { VmTypography.default }
private val LocalVmShapes = staticCompositionLocalOf { VmShapes.default }

object VmTheme {
    val colors: VmColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVmColors.current

    val typography: VmTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalVmTypography.current

    val shapes: VmShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalVmShapes.current
}

@Composable
fun VoidManagerTheme(
    themeMode: VmThemeMode = VmThemeMode.Dark,
    content: @Composable () -> Unit,
) {
    val dark =
        when (themeMode) {
            VmThemeMode.System -> isSystemInDarkTheme()
            VmThemeMode.Dark -> true
            VmThemeMode.Light -> false
        }
    val colors = if (dark) VmColors.dark else VmColors.light

    CompositionLocalProvider(
        LocalVmColors provides colors,
        LocalVmTypography provides VmTypography.default,
        LocalVmShapes provides VmShapes.default,
        content = content,
    )
}
