package io.github.behnooddev.voidmanager.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class VmColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSubtle: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
) {
    companion object {
        val dark =
            VmColors(
                isDark = true,
                background = Color(0xFF07090FL),
                surface = Color(0xFF0D1220L),
                surfaceRaised = Color(0xFF141B2DL),
                border = Color(0x1FFFFFFFL),
                borderStrong = Color(0x3DFFFFFFL),
                textPrimary = Color(0xFFE8ECF5L),
                textSecondary = Color(0xFF9AA5BDL),
                textTertiary = Color(0xFF7683A0L),
                accent = Color(0xFF7C97FFL),
                onAccent = Color(0xFF07090FL),
                accentSubtle = Color(0x267C97FFL),
                danger = Color(0xFFEC6F78L),
                warning = Color(0xFFE3AE55L),
                success = Color(0xFF55BC92L),
            )

        val light =
            VmColors(
                isDark = false,
                background = Color(0xFFF4F6FAL),
                surface = Color(0xFFFFFFFFL),
                surfaceRaised = Color(0xFFEBEFF6L),
                border = Color(0x1F0D1220L),
                borderStrong = Color(0x3D0D1220L),
                textPrimary = Color(0xFF0D1220L),
                textSecondary = Color(0xFF45506BL),
                textTertiary = Color(0xFF66718BL),
                accent = Color(0xFF3453D1L),
                onAccent = Color(0xFFFFFFFFL),
                accentSubtle = Color(0x1F3453D1L),
                danger = Color(0xFFB3261EL),
                warning = Color(0xFF8A5A00L),
                success = Color(0xFF1B7A55L),
            )
    }
}
