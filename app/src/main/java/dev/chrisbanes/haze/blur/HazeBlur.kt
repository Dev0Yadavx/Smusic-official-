package dev.chrisbanes.haze.blur

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import kotlin.math.roundToInt

class HazeBlurStyleScope {
    var blurRadius: Dp = 28.dp
    var cornerRadius: Dp = Dp.Unspecified

    fun blurRadius(radius: Dp) {
        this.blurRadius = radius
    }

    fun cornerRadius(radius: Dp) {
        this.cornerRadius = radius
    }
}

@Immutable
data class HazeBlurStyle(
    val blurRadius: Dp = 28.dp,
    val cornerRadius: Dp = Dp.Unspecified
)

fun HazeBlurStyle(block: HazeBlurStyleScope.() -> Unit): HazeBlurStyle {
    val scope = HazeBlurStyleScope().apply(block)
    return HazeBlurStyle(
        blurRadius = scope.blurRadius,
        cornerRadius = scope.cornerRadius
    )
}

fun Modifier.hazeBlur(
    input: HazeInput,
    style: HazeBlurStyle = HazeBlurStyle()
): Modifier = composed {
    val blurLayer = rememberGraphicsLayer()
    var targetPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    val clipRoundedPath = remember { Path() }

    this
        .onGloballyPositioned { coordinates ->
            targetPositionInRoot = coordinates.positionInRoot()
        }
        .drawWithContent {
            val sources = when (input) {
                is HazeInput.Sources -> input.states
            }

            if (sources.isNotEmpty() && size.width > 0f && size.height > 0f) {
                try {
                    val blurRadiusPx = style.blurRadius.toPx().coerceAtLeast(1f)
                    val padPx = blurRadiusPx.roundToInt().coerceIn(8, 120)
                    val recordWidth = (size.width.roundToInt() + padPx * 2).coerceAtLeast(1)
                    val recordHeight = (size.height.roundToInt() + padPx * 2).coerceAtLeast(1)

                    blurLayer.clip = true
                    blurLayer.renderEffect = BlurEffect(
                        radiusX = blurRadiusPx,
                        radiusY = blurRadiusPx,
                        edgeTreatment = TileMode.Clamp
                    )

                    var hasValidSource = false
                    blurLayer.record(size = IntSize(recordWidth, recordHeight)) {
                        for (sourceState in sources) {
                            // Subscribe to source redraws so scrolling/animations update the blur at 60/120fps
                            val tick = sourceState.drawTick
                            val srcLayer = sourceState.graphicsLayer
                            if (srcLayer != null && tick >= 0L) {
                                hasValidSource = true
                                val dx = sourceState.positionInRoot.x - targetPositionInRoot.x + padPx
                                val dy = sourceState.positionInRoot.y - targetPositionInRoot.y + padPx
                                translate(left = dx, top = dy) {
                                    drawLayer(srcLayer)
                                }
                            }
                        }
                    }

                    if (hasValidSource) {
                        val resolvedCornerPx = if (style.cornerRadius != Dp.Unspecified) {
                            style.cornerRadius.toPx()
                        } else {
                            minOf(size.width, size.height) / 2f
                        }

                        clipRoundedPath.reset()
                        clipRoundedPath.addRoundRect(
                            RoundRect(
                                rect = Rect(0f, 0f, size.width, size.height),
                                cornerRadius = CornerRadius(resolvedCornerPx, resolvedCornerPx)
                            )
                        )

                        clipPath(clipRoundedPath) {
                            translate(left = -padPx.toFloat(), top = -padPx.toFloat()) {
                                drawLayer(blurLayer)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    // Fail gracefully without crashing
                }
            }

            this@drawWithContent.drawContent()
        }
}
