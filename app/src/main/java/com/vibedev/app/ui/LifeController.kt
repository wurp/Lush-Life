package com.vibedev.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vibedev.app.life.LifeWorld
import com.vibedev.app.life.Library
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
class LifeController(private val settings: Settings) {
    var world by mutableStateOf(LifeWorld(1, 1))
        private set
    /** Bumped whenever cells change, so the canvas knows to redraw. */
    var frame by mutableLongStateOf(0L)
        private set
    /** Bumped when a new world is created, so the view can re-fit it. */
    var worldVersion by mutableIntStateOf(0)
        private set

    var pattern by mutableStateOf(Library.byName(settings.pattern))
        private set
    var running by mutableStateOf(true)
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
    var wrap by mutableStateOf(settings.wrap)
        private set
    var worldSize by mutableStateOf(WorldSize.entries.getOrElse(settings.worldSize) { WorldSize.MEDIUM })
        private set

    private val random = Random.Default

    init { load(pattern) }

    fun load(p: StartPattern) {
        pattern = p
        settings.pattern = p.name
        val w = maxOf(worldSize.width, p.minWidth)
        val h = maxOf(worldSize.height, p.minHeight)
        world = LifeWorld(w, h).also {
            it.wrap = wrap
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

    fun step() {
        world.step(rule, TRAIL_FADE)
        changed()
    }

    fun stepMany(count: Int) {
        repeat(count) { world.step(rule, TRAIL_FADE) }
        changed()
    }

    fun setCell(x: Int, y: Int, alive: Boolean) {
        if (world.isAlive(x, y) == alive) return
        world.set(x, y, alive)
        changed()
    }

    fun chooseTheme(t: LifeTheme) { theme = t; settings.theme = t.name; changed() }
    fun chooseRule(r: Rule) { rule = r; settings.rule = r.code }
    fun changeSpeed(s: Int) { speed = s; settings.speed = s }
    fun changeTrails(on: Boolean) { trails = on; settings.trails = on; changed() }
    fun changeGridLines(on: Boolean) { gridLines = on; settings.gridLines = on }
    fun changeWrap(on: Boolean) { wrap = on; settings.wrap = on; world.wrap = on }
    fun changeWorldSize(s: WorldSize) { worldSize = s; settings.worldSize = s.ordinal; restart() }

    private fun changed() { frame++ }

    private companion object {
        const val TRAIL_FADE = 6
    }
}
