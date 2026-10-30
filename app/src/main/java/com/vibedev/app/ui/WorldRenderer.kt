package com.vibedev.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vibedev.app.life.LifeWorld

/** Turns a world into a bitmap with one pixel per cell. Reuses its buffers between frames. */
class WorldRenderer {
    private var bitmap: Bitmap? = null
    private var image: ImageBitmap? = null
    private var pixels = IntArray(0)

    fun render(world: LifeWorld, theme: LifeTheme, trails: Boolean): ImageBitmap {
        val w = world.width
        val h = world.height
        var bmp = bitmap
        if (bmp == null || bmp.width != w || bmp.height != h) {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap = bmp
            image = bmp.asImageBitmap()
            pixels = IntArray(w * h)
        }
        val ages = theme.ageColors
        val trailColors = theme.trailColors
        val bg = theme.backgroundArgb
        for (i in pixels.indices) {
            val age = world.ageAt(i)
            pixels[i] = when {
                age != 0 -> ages[age]
                trails -> trailColors[world.trailAt(i)]
                else -> bg
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return image!!
    }
}
