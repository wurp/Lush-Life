package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor

/** Where the world sits on screen: [scale] pixels per cell, world corner at [offset]. */
class Viewport {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var size = IntSize.Zero
    private var fitScale = 1f

    fun fit(worldWidth: Int, worldHeight: Int) {
        if (size.width == 0 || size.height == 0) return
        fitScale = minOf(size.width / worldWidth.toFloat(), size.height / worldHeight.toFloat())
        scale = fitScale
        offset = Offset((size.width - worldWidth * scale) / 2f, (size.height - worldHeight * scale) / 2f)
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
