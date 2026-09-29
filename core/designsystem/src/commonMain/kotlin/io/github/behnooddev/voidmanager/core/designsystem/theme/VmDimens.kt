package io.github.behnooddev.voidmanager.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4dp grid. */
object VmSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
}

object VmDimens {
    val minTouchTarget: Dp = 48.dp
    val hairline: Dp = 1.dp
    val topBarHeight: Dp = 56.dp
    val navigationBarHeight: Dp = 64.dp
    val navigationRailWidth: Dp = 88.dp
    val iconSize: Dp = 24.dp
}

@Immutable
data class VmShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
) {
    companion object {
        val default =
            VmShapes(
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
            )
    }
}
