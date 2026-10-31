package com.vibedev.app.life

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class LifeWorldTest {
    /** Straightforward reference: every cell, every neighbour. */
    private fun reference(cells: Set<Pair<Int, Int>>, w: Int, h: Int, wrap: Boolean): Set<Pair<Int, Int>> {
        val out = HashSet<Pair<Int, Int>>()
        for (y in 0 until h) for (x in 0 until w) {
            var n = 0
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                var nx = x + dx
                var ny = y + dy
                if (wrap) { nx = (nx + w) % w; ny = (ny + h) % h } else if (nx !in 0 until w || ny !in 0 until h) continue
                if ((nx to ny) in cells) n++
            }
            val alive = (x to y) in cells
            if (n == 3 || (alive && n == 2)) out += x to y
        }
        return out
    }

    private fun cellsOf(world: LifeWorld): Set<Pair<Int, Int>> {
        val s = HashSet<Pair<Int, Int>>()
        for (y in 0 until world.height) for (x in 0 until world.width) if (world.isAlive(x, y)) s += x to y
        return s
    }

    @Test
    fun matchesReferenceForWrapAndHardEdges() {
        for (edges in listOf(Edges.WRAP, Edges.HARD)) {
            val random = Random(1)
            repeat(5) {
                val w = 20 + random.nextInt(30)
                val h = 20 + random.nextInt(30)
                val world = LifeWorld(w, h).also { it.edges = edges }
                // Sparse blobs, including some touching the edges.
                repeat(6) {
                    val cx = random.nextInt(w)
                    val cy = random.nextInt(h)
                    repeat(12) { world.set((cx + random.nextInt(5)).coerceAtMost(w - 1), (cy + random.nextInt(5)).coerceAtMost(h - 1), true) }
                }
                var expected = cellsOf(world)
                repeat(60) { gen ->
                    expected = reference(expected, w, h, edges == Edges.WRAP)
                    world.step(Rule.CONWAY)
                    assertEquals("$edges gen $gen", expected, cellsOf(world))
                    assertEquals(expected.size, world.population)
                }
            }
        }
    }

    @Test
    fun infiniteWorldKeepsGliderIntact() {
        var world = LifeWorld(10, 10).also { it.edges = Edges.INFINITE }
        world.place(Rle.parse("bo\$2bo\$3o!"), 3, 3)
        repeat(400) {
            world = world.withRoom()
            world.step(Rule.CONWAY)
        }
        assertEquals(5, world.population)
        // A glider moves one cell diagonally every 4 generations: 100 cells from where it started.
        val cells = cellsOf(world).map { (x, y) -> x + world.originX to y + world.originY }
        assertEquals(103, cells.minOf { it.first })
        assertEquals(103, cells.minOf { it.second })
    }

    @Test
    fun turingMachineRuleFileParses() {
        val text = java.io.File("src/main/assets/patterns/turing-machine.rle").readText()
        val shape = Rle.parse(text)
        assertEquals(1714, shape.width)
        assertEquals(1647, shape.height)
    }
}
