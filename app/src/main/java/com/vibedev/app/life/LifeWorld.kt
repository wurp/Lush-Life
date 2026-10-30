package com.vibedev.app.life

/**
 * The grid of cells. Each cell stores its age (0 = dead) so it can be coloured
 * by how long it has lived, and a fading "trail" value after it dies.
 */
class LifeWorld(val width: Int, val height: Int) {
    private var ages = ByteArray(width * height)
    private var scratch = ByteArray(width * height)
    val trail = ByteArray(width * height)

    var wrap = true
    var generation = 0L
        private set
    var population = 0
        private set

    fun index(x: Int, y: Int) = y * width + x
    fun contains(x: Int, y: Int) = x in 0 until width && y in 0 until height

    fun isAlive(x: Int, y: Int) = contains(x, y) && ages[index(x, y)].toInt() != 0

    /** Age 1..255 for live cells, 0 for dead. */
    fun ageAt(i: Int) = ages[i].toInt() and 0xFF
    fun trailAt(i: Int) = trail[i].toInt() and 0xFF

    fun set(x: Int, y: Int, alive: Boolean) {
        if (!contains(x, y)) return
        val i = index(x, y)
        val was = ages[i].toInt() != 0
        if (was == alive) return
        ages[i] = if (alive) 1 else 0
        if (!alive) trail[i] = 0xFF.toByte()
        population += if (alive) 1 else -1
    }

    fun clear() {
        ages.fill(0); trail.fill(0); population = 0; generation = 0
    }

    fun place(shape: Shape, left: Int, top: Int) {
        shape.cells.forEach { set(left + it.x, top + it.y, true) }
    }

    fun placeCentered(shape: Shape) =
        place(shape, (width - shape.width) / 2, (height - shape.height) / 2)

    fun step(rule: Rule, trailFade: Int) {
        val w = width
        val h = height
        val src = ages
        val dst = scratch
        var pop = 0
        for (y in 0 until h) {
            val up = neighbourRow(y - 1)
            val down = neighbourRow(y + 1)
            val row = y * w
            for (x in 0 until w) {
                val left = neighbourCol(x - 1)
                val right = neighbourCol(x + 1)
                var n = 0
                if (up >= 0) n += alive(src, up + left) + alive(src, up + x) + alive(src, up + right)
                n += alive(src, row + left) + alive(src, row + right)
                if (down >= 0) n += alive(src, down + left) + alive(src, down + x) + alive(src, down + right)
                val i = row + x
                val age = src[i].toInt() and 0xFF
                if (rule.next(age != 0, n)) {
                    dst[i] = (if (age == 0) 1 else minOf(age + 1, 255)).toByte()
                    pop++
                } else {
                    dst[i] = 0
                    trail[i] = if (age != 0) 0xFF.toByte() else fade(trail[i], trailFade)
                }
            }
        }
        ages = dst
        scratch = src
        population = pop
        generation++
    }

    // Returns -1 for "outside" so missing neighbours count as dead.
    private fun neighbourRow(y: Int): Int = when {
        y < 0 -> if (wrap) (height - 1) * width else -1
        y >= height -> if (wrap) 0 else -1
        else -> y * width
    }

    // Off-grid columns point at a sentinel handled by alive().
    private fun neighbourCol(x: Int): Int = when {
        x < 0 -> if (wrap) width - 1 else OUTSIDE
        x >= width -> if (wrap) 0 else OUTSIDE
        else -> x
    }

    private fun alive(a: ByteArray, i: Int): Int = if (i < 0 || i >= a.size || a[i].toInt() == 0) 0 else 1

    private fun fade(b: Byte, by: Int): Byte = maxOf(0, (b.toInt() and 0xFF) - by).toByte()


    private companion object {
        // Large negative so (row + OUTSIDE) is always < 0 and reads as dead.
        const val OUTSIDE = -1_000_000_000
    }
}
