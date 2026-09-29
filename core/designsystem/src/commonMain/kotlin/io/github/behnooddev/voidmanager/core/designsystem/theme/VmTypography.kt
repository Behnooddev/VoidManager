package io.github.behnooddev.voidmanager.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class VmTypography(
    val display: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
) {
    companion object {
        /** The typeface is the platform default until a licensed family is chosen (see docs/open-decisions.md). */
        val default = create(FontFamily.Default)

        fun create(fontFamily: FontFamily) =
            VmTypography(
                display =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.2).sp,
                    ),
                title =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
                subtitle =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                    ),
                body =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                bodyStrong =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                label =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                caption =
                    TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
            )
    }
}
