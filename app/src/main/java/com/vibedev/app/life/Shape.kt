package com.vibedev.app.life

data class Cell(val x: Int, val y: Int)

/** A set of live cells, normalised so the top-left is (0, 0). */
class Shape(cells: Collection<Cell>) {
    val cells: List<Cell>
    val width: Int
    val height: Int

    init {
        val minX = cells.minOfOrNull { it.x } ?: 0
        val minY = cells.minOfOrNull { it.y } ?: 0
        this.cells = cells.map { Cell(it.x - minX, it.y - minY) }.distinct()
        width = (this.cells.maxOfOrNull { it.x } ?: -1) + 1
        height = (this.cells.maxOfOrNull { it.y } ?: -1) + 1
    }

    /** One of the 8 rotations/reflections, 0..7. */
    fun oriented(t: Int): Shape = Shape(cells.map { c ->
        var x = if (t and 4 != 0) -c.x else c.x
        var y = c.y
        repeat(t and 3) { val nx = -y; y = x; x = nx }
        Cell(x, y)
    })

    companion object {
        /** Combines shapes placed at the given offsets. */
        fun compose(vararg parts: Pair<Shape, Cell>): Shape =
            Shape(parts.flatMap { (s, at) -> s.cells.map { Cell(it.x + at.x, it.y + at.y) } })
    }
}

/** Reads patterns in the standard RLE format used by Life enthusiasts. */
object Rle {
    fun parse(text: String): Shape {
        val cells = ArrayList<Cell>()
        var x = 0
        var y = 0
        var count = 0
        val body = text.lineSequence()
            .filterNot { it.startsWith("#") || it.trimStart().startsWith("x") }
            .joinToString("")
        for (ch in body) {
            when {
                ch.isDigit() -> { count = count * 10 + (ch - '0'); continue }
                ch == 'b' || ch == '.' -> x += maxOf(count, 1)
                ch == 'o' || ch == 'A' -> { repeat(maxOf(count, 1)) { cells += Cell(x + it, y) }; x += maxOf(count, 1) }
                ch == '$' -> { y += maxOf(count, 1); x = 0 }
                ch == '!' -> break
            }
            count = 0
        }
        return Shape(cells)
    }
}
