package com.vibedev.app.life

/**
 * The grid of cells. Each cell stores its age (0 = dead) so it can be coloured
 * by how long it has lived, and when it last died so it can leave a fading trail.
 *
 * Work is skipped for 16-cell blocks with nothing alive nearby, so big, mostly
 * empty worlds (like the Turing machine) stay fast.
 */
class LifeWorld(val width: Int, val height: Int) {
    private var ages = ByteArray(width * height)
    private var scratch = ByteArray(width * height)
    private val diedAt = IntArray(width * height) { NEVER }

    // Which blocks of each row may hold live cells, for [ages] and for [scratch].
    private val chunks = (width + CHUNK - 1) / CHUNK
    private var occupied = BooleanArray(height * chunks)
    private var scratchOccupied = BooleanArray(height * chunks)

    var edges = Edges.WRAP

    /** Position of this grid's top-left cell in the endless plane. Changes when an infinite world grows left or up. */
    var originX = 0
        private set
    var originY = 0
        private set
    var generation = 0
        private set
    var population = 0
        private set

    fun index(x: Int, y: Int) = y * width + x
    fun contains(x: Int, y: Int) = x in 0 until width && y in 0 until height

    fun isAlive(x: Int, y: Int) = contains(x, y) && ages[index(x, y)].toInt() != 0

    /** Age 1..255 for live cells, 0 for dead. */
    fun ageAt(i: Int) = ages[i].toInt() and 0xFF

    /** Trail strength 0..255 for a dead cell: 255 just after dying, fading by [fadePerGeneration]. */
    fun trailAt(i: Int, fadePerGeneration: Int): Int {
        val since = generation - diedAt[i]
        return if (since < 0 || since > 255) 0 else maxOf(0, 255 - since * fadePerGeneration)
    }

    fun set(x: Int, y: Int, alive: Boolean) {
        if (!contains(x, y)) return
        val i = index(x, y)
        val was = ages[i].toInt() != 0
        if (was == alive) return
        ages[i] = if (alive) 1 else 0
        if (alive) occupied[y * chunks + x / CHUNK] = true else diedAt[i] = generation
        population += if (alive) 1 else -1
    }

    fun clear() {
        ages.fill(0); scratch.fill(0); diedAt.fill(NEVER)
        occupied.fill(false); scratchOccupied.fill(false)
        population = 0; generation = 0
    }

    fun place(shape: Shape, left: Int, top: Int) {
        shape.cells.forEach { set(left + it.x, top + it.y, true) }
    }

    fun placeCentered(shape: Shape) =
        place(shape, (width - shape.width) / 2, (height - shape.height) / 2)

    fun step(rule: Rule) {
        val w = width
        val src = ages
        val dst = scratch
        val wrapping = edges == Edges.WRAP
        val nextGen = generation + 1
        var pop = 0
        for (y in 0 until height) {
            val up = neighbourRow(y - 1)
            val down = neighbourRow(y + 1)
            val row = y * w
            for (c in 0 until chunks) {
                val block = y * chunks + c
                val start = c * CHUNK
                val end = minOf(start + CHUNK, w)
                if (!nearLife(y, c, up, down, wrapping)) {
                    // Nothing alive nearby: everything here stays dead.
                    if (scratchOccupied[block]) dst.fill(0, row + start, row + end)
                    scratchOccupied[block] = false
                    continue
                }
                var any = false
                for (x in start until end) {
                    val i = row + x
                    val left = neighbourCol(x - 1)
                    val right = neighbourCol(x + 1)
                    var n = 0
                    if (up >= 0) n += alive(src, up + left) + alive(src, up + x) + alive(src, up + right)
                    n += alive(src, row + left) + alive(src, row + right)
                    if (down >= 0) n += alive(src, down + left) + alive(src, down + x) + alive(src, down + right)
                    val age = src[i].toInt() and 0xFF
                    if (rule.next(age != 0, n)) {
                        dst[i] = (if (age == 0) 1 else minOf(age + 1, 255)).toByte()
                        pop++
                        any = true
                    } else {
                        dst[i] = 0
                        if (age != 0) diedAt[i] = nextGen
                    }
                }
                scratchOccupied[block] = any
            }
        }
        ages = dst
        scratch = src
        occupied = scratchOccupied.also { scratchOccupied = occupied }
        population = pop
        generation = nextGen
    }

    /** True if block [c] of row [y], or any block touching it, may hold a live cell. */
    private fun nearLife(y: Int, c: Int, up: Int, down: Int, wrapping: Boolean): Boolean {
        for (r in 0..2) {
            val rowStart = when (r) { 0 -> up; 1 -> y * width; else -> down }
            if (rowStart < 0) continue
            val base = (rowStart / width) * chunks
            for (dc in -1..1) {
                var cc = c + dc
                if (cc < 0 || cc >= chunks) {
                    if (!wrapping) continue
                    cc = (cc + chunks) % chunks
                }
                if (occupied[base + cc]) return true
            }
        }
        return false
    }

    // Returns -1 for "outside" so missing neighbours count as dead.
    private fun neighbourRow(y: Int): Int = when {
        y < 0 -> if (edges == Edges.WRAP) (height - 1) * width else -1
        y >= height -> if (edges == Edges.WRAP) 0 else -1
        else -> y * width
    }

    // Off-grid columns point at a sentinel handled by alive().
    private fun neighbourCol(x: Int): Int = when {
        x < 0 -> if (edges == Edges.WRAP) width - 1 else OUTSIDE
        x >= width -> if (edges == Edges.WRAP) 0 else OUTSIDE
        else -> x
    }

    private fun alive(a: ByteArray, i: Int): Int = if (i < 0 || i >= a.size || a[i].toInt() == 0) 0 else 1

    /**
     * In infinite mode, returns a bigger copy of this world when something is close
     * enough to an edge that it could cross it next step. Otherwise returns this world.
     */
    fun withRoom(): LifeWorld {
        if (edges != Edges.INFINITE || population == 0) return this
        val left = if (columnHasLife(0) || columnHasLife(1)) GROW else 0
        val right = if (columnHasLife(width - 1) || columnHasLife(width - 2)) GROW else 0
        val top = if (rowHasLife(0) || rowHasLife(1)) GROW else 0
        val bottom = if (rowHasLife(height - 1) || rowHasLife(height - 2)) GROW else 0
        if (left + right + top + bottom == 0) return this

        val bigger = LifeWorld(width + left + right, height + top + bottom)
        bigger.edges = edges
        bigger.originX = originX - left
        bigger.originY = originY - top
        bigger.generation = generation
        for (y in 0 until height) {
            val to = (y + top) * bigger.width + left
            System.arraycopy(ages, y * width, bigger.ages, to, width)
            System.arraycopy(diedAt, y * width, bigger.diedAt, to, width)
        }
        bigger.population = population
        bigger.markOccupied()
        return bigger
    }

    private fun markOccupied() {
        occupied.fill(false)
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (ages[row + x].toInt() != 0) occupied[y * chunks + x / CHUNK] = true
            }
        }
    }

    private fun rowHasLife(y: Int): Boolean {
        val start = y * width
        for (i in start until start + width) if (ages[i].toInt() != 0) return true
        return false
    }

    private fun columnHasLife(x: Int): Boolean {
        for (y in 0 until height) if (ages[y * width + x].toInt() != 0) return true
        return false
    }

    private companion object {
        /** How many cells an infinite world grows by on each side that needs room. */
        const val GROW = 32
        /** Width of the blocks used to skip empty space. */
        const val CHUNK = 16
        const val NEVER = Int.MIN_VALUE / 2

        // Large negative so (row + OUTSIDE) is always < 0 and reads as dead.
        const val OUTSIDE = -1_000_000_000
    }
}
