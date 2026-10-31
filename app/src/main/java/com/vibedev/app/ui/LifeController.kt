package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibedev.app.life.Edges
import com.vibedev.app.life.LifeWorld
import com.vibedev.app.life.PatternLibrary
import com.vibedev.app.life.Rule
import com.vibedev.app.life.StartPattern
import kotlin.random.Random

/** World sizes offered in settings, in cells (portrait shaped to suit phones). */
enum class WorldSize(val label: String, val width: Int, val height: Int) {
    SMALL("Small", 72, 120),
    MEDIUM("Medium", 120, 200),
    LARGE("Large", 200, 330),
}

/** Holds the game state the screen shows, and the actions the screen can take. */
class LifeController(private val settings: Settings, val library: PatternLibrary) {
    var world by mutableStateOf(LifeWorld(1, 1))
        private set
    /** Bumped whenever cells change, so the canvas knows to redraw. */
    var frame by mutableLongStateOf(0L)
        private set
    /** Bumped when a new world is created, so the view can re-fit it. */
    var worldVersion by mutableIntStateOf(0)
        private set

    var pattern by mutableStateOf(library.byName(settings.pattern))
        private set
    var running by mutableStateOf(false)
    var drawMode by mutableStateOf(false)

    var theme by mutableStateOf(LifeThemes.byName(settings.theme))
        private set
    var rule by mutableStateOf(Rule.fromCode(settings.rule))
        private set
    var speed by mutableIntStateOf(settings.speed)
        private set
    var trails by mutableStateOf(settings.trails)
        private set
    var gridLines by mutableStateOf(settings.gridLines)
        private set
    var edges by mutableStateOf(Edges.entries.firstOrNull { it.name == settings.edges } ?: Edges.WRAP)
        private set

    /** True when an infinite world has grown past [sizeLimit] and the author should decide what to do. */
    var sizeWarning by mutableStateOf(false)
        private set
    private var sizeLimit = START_SIZE_LIMIT
    var worldSize by mutableStateOf(WorldSize.entries.getOrElse(settings.worldSize) { WorldSize.MEDIUM })
        private set

    private val random = Random.Default

    init { load(pattern) }

    fun load(p: StartPattern) {
        pattern = p
        settings.pattern = p.name
        running = false
        sizeWarning = false
        val w = maxOf(worldSize.width, p.minWidth)
        val h = maxOf(worldSize.height, p.minHeight)
        sizeLimit = maxOf(START_SIZE_LIMIT, w.toLong() * h * 2)
        world = LifeWorld(w, h).also {
            it.edges = edges
            p.seed(it, random)
        }
        worldVersion++
        changed()
    }

    fun restart() = load(pattern)

    fun clear() {
        world.clear()
        running = false
        changed()
    }

    fun step() = stepMany(1)

    /** Runs up to [count] generations, stopping early once [budgetNanos] has passed. Returns how many ran. */
    fun stepMany(count: Int, budgetNanos: Long = Long.MAX_VALUE): Int {
        val start = System.nanoTime()
        var done = 0
        while (done < count && advance()) {
            done++
            if (System.nanoTime() - start > budgetNanos) break
        }
        changed()
        return done
    }

    /** One generation. Returns false if the world got too big and needs the author's decision first. */
    private fun advance(): Boolean {
        val roomy = world.withRoom()
        if (roomy !== world) {
            world = roomy
            if (roomy.width.toLong() * roomy.height > sizeLimit) {
                running = false
                sizeWarning = true
                return false
            }
        }
        world.step(rule)
        return true
    }

    /** [x], [y] are positions in the endless plane, as the screen sees them. */
    fun isAliveAt(x: Int, y: Int) = world.isAlive(x - world.originX, y - world.originY)

    fun setCell(x: Int, y: Int, alive: Boolean) {
        if (isAliveAt(x, y) == alive) return
        world.set(x - world.originX, y - world.originY, alive)
        changed()
    }

    // Answers to the "world is getting very large" question.
    fun useHardEdges() { changeEdges(Edges.HARD); sizeWarning = false; running = true }
    fun useWrap() { changeEdges(Edges.WRAP); sizeWarning = false; running = true }
    fun keepGrowing() { sizeLimit *= 4; sizeWarning = false; running = true }
    fun stopSimulating() { sizeWarning = false; running = false }

    fun chooseTheme(t: LifeTheme) { theme = t; settings.theme = t.name; changed() }
    fun chooseRule(r: Rule) { rule = r; settings.rule = r.code }
    fun changeSpeed(s: Int) { speed = s; settings.speed = s }
    fun changeTrails(on: Boolean) { trails = on; settings.trails = on; changed() }
    fun changeGridLines(on: Boolean) { gridLines = on; settings.gridLines = on }
    fun changeEdges(e: Edges) { edges = e; settings.edges = e.name; world.edges = e }
    fun changeWorldSize(s: WorldSize) { worldSize = s; settings.worldSize = s.ordinal; restart() }

    private fun changed() { frame++ }

    private companion object {
        const val START_SIZE_LIMIT = 1_000_000L
    }
}
