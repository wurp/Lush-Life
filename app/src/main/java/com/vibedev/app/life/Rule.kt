package com.vibedev.app.life

/** A Life-like rule in B/S notation, e.g. "B3/S23" for Conway's Game of Life. */
class Rule(val name: String, val code: String, val description: String) {
    private val birth = BooleanArray(9)
    private val survive = BooleanArray(9)

    init {
        val parts = code.uppercase().split("/")
        for (part in parts) {
            val target = when {
                part.startsWith("B") -> birth
                part.startsWith("S") -> survive
                else -> continue
            }
            part.drop(1).forEach { ch -> ch.digitToIntOrNull()?.takeIf { it in 0..8 }?.let { target[it] = true } }
        }
    }

    fun next(alive: Boolean, neighbours: Int): Boolean =
        if (alive) survive[neighbours] else birth[neighbours]

    companion object {
        val CONWAY = Rule("Conway's Life", "B3/S23", "The classic. Born with 3 neighbours, survives with 2 or 3.")
        val presets = listOf(
            CONWAY,
            Rule("HighLife", "B36/S23", "Like Life, plus births on 6. Famous for a self-copying replicator."),
            Rule("Day & Night", "B3678/S34678", "Live and dead cells behave the same. Blobby, symmetric worlds."),
            Rule("Seeds", "B2/S", "Every cell dies at once. Explosive, chaotic growth."),
            Rule("Maze", "B3/S12345", "Grows winding corridors like a labyrinth."),
            Rule("Coral", "B3/S45678", "Slow, branching growth like a reef."),
            Rule("Replicator", "B1357/S1357", "Every shape copies itself over and over."),
            Rule("Life without Death", "B3/S012345678", "Cells never die. Leaves frozen, lacy trails."),
        )

        fun fromCode(code: String?): Rule = presets.firstOrNull { it.code == code } ?: CONWAY
    }
}
