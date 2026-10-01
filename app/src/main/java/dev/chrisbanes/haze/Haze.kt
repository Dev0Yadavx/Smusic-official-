package dev.chrisbanes.haze

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize

@Stable
class HazeState {
    internal var graphicsLayer: GraphicsLayer? by mutableStateOf(null)
    internal var positionInRoot: Offset by mutableStateOf(Offset.Zero)
    internal var size: IntSize by mutableStateOf(IntSize.Zero)
    internal var drawTick: Long by mutableLongStateOf(0L)
}

@Composable
fun rememberHazeState(): HazeState {
    return remember { HazeState() }
}

sealed interface HazeInput {
    data class Sources(val states: List<HazeState>) : HazeInput {
        constructor(vararg states: HazeState) : this(states.toList())
    }
}

fun Modifier.hazeSource(state: HazeState): Modifier = composed {
    val layer = rememberGraphicsLayer()
    state.graphicsLayer = layer

    this
        .onGloballyPositioned { coordinates ->
            state.positionInRoot = coordinates.positionInRoot()
            state.size = coordinates.size
        }
        .drawWithContent {
            layer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(layer)
            state.drawTick++
        }
}
