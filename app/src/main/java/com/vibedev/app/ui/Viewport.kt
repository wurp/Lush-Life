package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.vibedev.app.life.LifeWorld
import kotlin.math.floor

/** Where the endless plane sits on screen: [scale] pixels per cell, plane position (0, 0) at [offset]. */
class Viewport {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var size = IntSize.Zero
    private var fitScale = 1f

    /** Zooms so the whole world fills the screen. */
    fun fit(world: LifeWorld) {
        if (size.width == 0 || size.height == 0) return
        fitScale = minOf(size.width / world.width.toFloat(), size.height / world.height.toFloat())
        scale = fitScale
        offset = Offset(
            (size.width - world.width * scale) / 2f - world.originX * scale,
            (size.height - world.height * scale) / 2f - world.originY * scale,
        )
    }

    fun pan(by: Offset) { offset += by }

    fun zoom(factor: Float, around: Offset) {
        val newScale = (scale * factor).coerceIn(fitScale * 0.5f, MAX_SCALE)
        val k = newScale / scale
        offset = (offset - around) * k + around
        scale = newScale
    }

    fun cellX(screen: Offset) = floor((screen.x - offset.x) / scale).toInt()
    fun cellY(screen: Offset) = floor((screen.y - offset.y) / scale).toInt()

    private companion object {
        const val MAX_SCALE = 64f
    }
}
