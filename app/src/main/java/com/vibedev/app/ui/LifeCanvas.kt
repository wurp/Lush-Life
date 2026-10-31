package com.vibedev.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun LifeCanvas(controller: LifeController, viewport: Viewport, modifier: Modifier = Modifier) {
    val renderer = remember { WorldRenderer() }

    LaunchedEffect(controller.worldVersion) {
        viewport.fit(controller.world)
    }

    Canvas(
        modifier
            .onSizeChanged {
                val first = viewport.size == IntSize.Zero
                viewport.size = it
                if (first) viewport.fit(controller.world)
            }
            .pointerInput(controller, viewport) { handleTouches(controller, viewport) },
    ) {
        controller.frame // Redraw whenever the cells change.
        val world = controller.world
        val theme = controller.theme
        drawRect(theme.background)
        val image = renderer.render(world, theme, controller.trails)
        val scale = viewport.scale
        // Screen position of this grid's top-left cell.
        val origin = viewport.offset + Offset(world.originX * scale, world.originY * scale)
        drawImage(
            image,
            dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
            dstSize = IntSize((world.width * scale).roundToInt(), (world.height * scale).roundToInt()),
            filterQuality = FilterQuality.None,
        )
        val worldSize = Size(world.width * scale, world.height * scale)
        if (controller.gridLines && scale >= 7f) {
            val firstX = max(0, ((-origin.x) / scale).toInt())
            val lastX = minOf(world.width, ((size.width - origin.x) / scale).toInt() + 1)
            val firstY = max(0, ((-origin.y) / scale).toInt())
            val lastY = minOf(world.height, ((size.height - origin.y) / scale).toInt() + 1)
            val top = maxOf(origin.y, 0f)
            val bottom = minOf(origin.y + worldSize.height, size.height)
            val left = maxOf(origin.x, 0f)
            val right = minOf(origin.x + worldSize.width, size.width)
            for (x in firstX..lastX) {
                val px = origin.x + x * scale
                drawLine(theme.grid, Offset(px, top), Offset(px, bottom), strokeWidth = 1f)
            }
            for (y in firstY..lastY) {
                val py = origin.y + y * scale
                drawLine(theme.grid, Offset(left, py), Offset(right, py), strokeWidth = 1f)
            }
        }
        drawRect(theme.grid.copy(alpha = 0.5f), topLeft = origin, size = worldSize, style = Stroke(2f))
    }
}

/** One finger pans (or paints in Draw mode), two fingers zoom, a tap flips a cell. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.handleTouches(
    controller: LifeController,
    viewport: Viewport,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val slop = viewConfiguration.touchSlop
    var moved = 0f
    var multiTouch = false
    var lastCell = viewport.cellX(down.position) to viewport.cellY(down.position)
    val paintValue: Boolean? = if (controller.drawMode) {
        (!controller.isAliveAt(lastCell.first, lastCell.second)).also {
            controller.setCell(lastCell.first, lastCell.second, it)
        }
    } else null

    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) break
        if (pressed.size >= 2) {
            multiTouch = true
            viewport.zoom(event.calculateZoom(), event.calculateCentroid())
            viewport.pan(event.calculatePan())
        } else if (!multiTouch) {
            val change = pressed.first()
            val delta = change.position - change.previousPosition
            moved += delta.getDistance()
            if (paintValue != null) {
                val cell = viewport.cellX(change.position) to viewport.cellY(change.position)
                paintLine(controller, lastCell, cell, paintValue)
                lastCell = cell
            } else if (moved > slop) {
                viewport.pan(delta)
            }
        }
        event.changes.forEach { it.consume() }
    }

    if (paintValue == null && !multiTouch && moved <= slop) {
        val (x, y) = lastCell
        controller.setCell(x, y, !controller.isAliveAt(x, y))
    }
}

private fun paintLine(controller: LifeController, from: Pair<Int, Int>, to: Pair<Int, Int>, alive: Boolean) {
    val dx = to.first - from.first
    val dy = to.second - from.second
    val steps = max(abs(dx), abs(dy)).coerceAtLeast(1)
    for (i in 1..steps) {
        controller.setCell(from.first + dx * i / steps, from.second + dy * i / steps, alive)
    }
}
