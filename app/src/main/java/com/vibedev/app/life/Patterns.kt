package com.vibedev.app.life

import kotlin.random.Random

enum class Category(val title: String) {
    RANDOM("Random worlds"),
    CHAOS("Chaos from tiny seeds"),
    SHIPS("Spaceships"),
    OSCILLATORS("Oscillators"),
    GUNS("Guns"),
    COMPUTER("Computer parts (Turing complete)"),
    MACHINES("Machines that compute"),
}

/** Anything that can fill a fresh world. New kinds of start conditions just implement this. */
interface StartPattern {
    val name: String
    val category: Category
    val description: String

    /** Minimum world size this pattern needs (0 = any). */
    val minWidth: Int get() = 0
    val minHeight: Int get() = 0

    fun seed(world: LifeWorld, random: Random)
}

/** A fixed shape, drawn in the middle of the world. The shape is only built when first needed. */
class ShapePattern(
    override val name: String,
    override val category: Category,
    override val description: String,
    private val margin: Int = 24,
    makeShape: () -> Shape,
) : StartPattern {
    constructor(name: String, category: Category, description: String, shape: Shape, margin: Int = 24) :
        this(name, category, description, margin, { shape })
    constructor(name: String, category: Category, description: String, rle: String, margin: Int = 24) :
        this(name, category, description, margin, { Rle.parse(rle) })

    private val shape by lazy(makeShape)
    override val minWidth get() = shape.width + margin * 2
    override val minHeight get() = shape.height + margin * 2
    override fun seed(world: LifeWorld, random: Random) = world.placeCentered(shape)
}

/** Reads bundled pattern files. Lets the library stay independent of Android. */
fun interface PatternFiles {
    fun read(path: String): String
}

class RandomSoup(
    override val name: String,
    override val description: String,
    private val density: Float,
    private val fill: Float,
    private val symmetric: Boolean,
) : StartPattern {
    override val category = Category.RANDOM

    override fun seed(world: LifeWorld, random: Random) {
        val side = (minOf(world.width, world.height) * fill).toInt()
        val w = if (symmetric) side else (world.width * fill).toInt()
        val h = if (symmetric) side else (world.height * fill).toInt()
        val left = (world.width - w) / 2
        val top = (world.height - h) / 2
        // For symmetry, pick one quadrant at random and mirror it into the other three.
        val qw = (w + 1) / 2
        val qh = (h + 1) / 2
        val quadrant = BooleanArray(qw * qh) { random.nextFloat() < density }
        for (y in 0 until h) for (x in 0 until w) {
            val alive = if (symmetric) {
                quadrant[minOf(y, h - 1 - y) * qw + minOf(x, w - 1 - x)]
            } else {
                random.nextFloat() < density
            }
            if (alive) world.set(left + x, top + y, true)
        }
    }
}

class GliderRain(override val name: String, override val description: String, private val count: Int) : StartPattern {
    override val category = Category.RANDOM
    private val glider = Rle.parse(Library.GLIDER)

    override fun seed(world: LifeWorld, random: Random) {
        repeat(count) {
            val g = glider.oriented(random.nextInt(8))
            world.place(g, random.nextInt(world.width - 3), random.nextInt(world.height - 3))
        }
    }
}

object Library {
    const val GLIDER = "bo\$2bo\$3o!"
    private const val LWSS = "bo2bo\$o4b\$o3bo\$4o!"
    private const val MWSS = "3bo2b\$bo3bo\$o5b\$o4bo\$5o!"
    private const val HWSS = "3b2o2b\$bo4bo\$o6b\$o5bo\$6o!"
    private const val GOSPER = "24bo\$22bobo\$12b2o6b2o12b2o\$11bo3bo4b2o12b2o\$2o8bo5bo3b2o\$2o8bo3bob2o4bobo\$10bo5bo7bo\$11bo3bo\$12b2o!"

    private val fleet = Shape.compose(
        Rle.parse(LWSS) to Cell(0, 0),
        Rle.parse(MWSS) to Cell(14, 12),
        Rle.parse(HWSS) to Cell(2, 26),
        Rle.parse(LWSS) to Cell(18, 40),
        Rle.parse(MWSS) to Cell(6, 52),
    )

    private const val GATE_TIP = "\n\nA glider is a 1, a gap is a 0. Tip: switch to Draw and erase a glider from an input stream to send a 0, then watch the output change."

    val builtIn: List<StartPattern> = listOf(
        RandomSoup("Symmetric bloom", "A four-way mirrored random seed. Grows into kaleidoscope patterns.", 0.38f, 0.5f, true),
        RandomSoup("Random soup", "A random scattering of live cells that settles into a busy ecosystem.", 0.3f, 0.8f, false),
        RandomSoup("Sparse dust", "A thin random sprinkle. Watch tiny islands of life appear.", 0.12f, 0.95f, false),
        GliderRain("Glider storm", "Dozens of gliders flying in every direction, colliding and exploding.", 40),

        ShapePattern("R-pentomino", Category.CHAOS, "Just 5 cells, yet it churns for over 1,100 generations.", "b2o\$2o\$bo!", 60),
        ShapePattern("Acorn", Category.CHAOS, "7 cells that take 5,206 generations to settle, throwing off gliders.", "bo5b\$3bo3b\$2o2b3o!", 80),
        ShapePattern("Diehard", Category.CHAOS, "Lives for exactly 130 generations, then vanishes completely.", "6bob\$2o6b\$bo3b3o!", 30),
        ShapePattern("Infinite line", Category.CHAOS, "A single line of cells that grows forever.", "8ob5o3b3o6b7ob5o!", 50),

        ShapePattern("Glider", Category.SHIPS, "The smallest spaceship. Walks diagonally forever.", GLIDER),
        ShapePattern("Lightweight spaceship", Category.SHIPS, "Flies sideways at half the speed of light.", LWSS),
        ShapePattern("Spaceship fleet", Category.SHIPS, "Light, middle and heavy spaceships flying in formation.", fleet, 30),
        ShapePattern("Copperhead", Category.SHIPS, "A slow, elegant ship discovered in 2016.", "b2o2b2ob\$3b2o3b\$3b2o3b\$obo2bobo\$o6bo2\$o6bo\$b2o2b2ob\$2b4o2b2\$3b2o3b\$3b2o!"),

        ShapePattern("Pulsar", Category.OSCILLATORS, "A large, beautiful pattern that pulses every 3 generations.", "2b3o3b3o2b2\$o4bobo4bo\$o4bobo4bo\$o4bobo4bo\$2b3o3b3o2b2\$2b3o3b3o2b\$o4bobo4bo\$o4bobo4bo\$o4bobo4bo2\$2b3o3b3o!"),
        ShapePattern("Pentadecathlon", Category.OSCILLATORS, "Repeats every 15 generations.", "2bo4bo2b\$2ob4ob2o\$2bo4bo!"),
        ShapePattern("Queen bee shuttle", Category.OSCILLATORS, "A queen bee bounces back and forth between two blocks.", "9bo12b\$7bobo12b\$6bobo13b\$2o3bo2bo11b2o\$2o4bobo11b2o\$7bobo12b\$9bo!"),
        ShapePattern("Figure eight", Category.OSCILLATORS, "Two blocks dancing in a figure-eight, period 8.", "3o3b\$3o3b\$3o3b\$3b3o\$3b3o\$3b3o!"),

        ShapePattern("Gosper glider gun", Category.GUNS, "The first pattern found that grows forever. Fires a glider every 30 generations.", GOSPER, 40),
        ShapePattern("Simkin glider gun", Category.GUNS, "The smallest known glider gun, firing every 120 generations.", "2o5b2o\$2o5b2o2\$4b2o\$4b2o5\$22b2ob2o\$21bo5bo\$21bo6bo2b2o\$21b3o3bo3b2o\$26bo4\$20b2o\$20bo\$21b3o\$23bo!", 40),

        ShapePattern("NOT gate", Category.COMPUTER, "The top-left gun sends a coded message. Where it crosses the other gun's stream, gliders destroy each other in pairs, so the output (heading down-left) carries the opposite bits.$GATE_TIP", Rle.parse(Circuits.NOT_GATE)),
        ShapePattern("AND gate", Category.COMPUTER, "Two coded input streams flow down-right. A third gun cancels every 1 of the upper input, then hits the lower one. Only when both inputs are 1 does a glider survive to the output.$GATE_TIP", Rle.parse(Circuits.AND_GATE)),
        ShapePattern("OR gate", Category.COMPUTER, "Two coded inputs and two helper guns. A glider reaches the output (the bottom stream) when either input is 1. NOT, AND and OR together are enough to build any computer, which is why Life is Turing complete.$GATE_TIP", Rle.parse(Circuits.OR_GATE)),
    )

}

/** Every start pattern: the built-in ones plus big machines loaded from bundled files. */
class PatternLibrary(private val files: PatternFiles) {
    private fun machine(name: String, description: String, file: String) =
        ShapePattern(name, Category.MACHINES, description, margin = 60) { Rle.parse(files.read("patterns/$file")) }

    val all: List<StartPattern> = Library.builtIn + listOf(
        machine(
            "Turing machine",
            "Paul Rendell's Turing machine (2000), a real programmable computer built from Life: a 3-state, 3-symbol machine " +
                "with a tape, a read/write head and a table of rules, all made of glider streams. Each machine step takes " +
                "thousands of generations, so turn the speed up and be patient. Big: 1714 × 1647 cells.",
            "turing-machine.rle",
        ),
        machine(
            "Primer (prime numbers)",
            "Four versions of Dean Hickerson's prime sieve (1991, plus 2005 variants by Jason Summers). Each one fires a " +
                "stream of spaceships; gaps are knocked out at every multiple, so only primes get through. " +
                "Best with Infinite or Hard edges.",
            "four-primers.rle",
        ),
        machine(
            "Twin primes",
            "Dean Hickerson (1994). A spaceship escapes past the gun only when 6n−1 and 6n+1 are both prime. " +
                "Best with Infinite or Hard edges.",
            "twin-primes.rle",
        ),
        machine(
            "Fermat primes",
            "Tests every number 2ⁿ+1 for primality. The row of small tubs stands for the known Fermat primes; a tub is " +
                "destroyed each time one is found. If an unknown Fermat prime exists, the whole thing self-destructs. " +
                "Nobody knows if it does, which makes this pattern an unsolved maths problem. Best with Infinite edges.",
            "fermat-primes.rle",
        ),
    )

    val default: StartPattern get() = all.first()
    fun byName(name: String?) = all.firstOrNull { it.name == name } ?: default
}
