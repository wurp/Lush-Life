package com.vibedev.app.life

/** What happens at the edge of the world. */
enum class Edges(val label: String, val description: String) {
    WRAP("Wrap around", "Things leaving one side come back on the other"),
    HARD("Hard edges", "Things that reach the edge drop off"),
    INFINITE("Infinite", "The world grows as things spread, so nothing is lost"),
}
