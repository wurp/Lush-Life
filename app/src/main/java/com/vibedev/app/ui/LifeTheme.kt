package com.vibedev.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

/**
 * How the world looks. Young cells take the first colour in [ages] and drift
 * through the rest as they get older; dead cells leave a [trail] that fades out.
 */
class LifeTheme(
    val name: String,
    val background: Color,
    val ages: List<Color>,
    val trail: Color,
    val grid: Color,
    val accent: Color,
    val isLight: Boolean = false,
) {
    /** Colour for each age 0..255 (index 0 unused). */
    val ageColors: IntArray = IntArray(256) { age -> colorAlong(ages, (age - 1).coerceAtLeast(0) / AGE_SPAN).toArgb() }

    /** Colour for each trail strength 0..255. */
    val trailColors: IntArray = IntArray(256) { t -> lerp(background, trail, (t / 255f) * (t / 255f)).toArgb() }

    val backgroundArgb = background.toArgb()

    private companion object {
        const val AGE_SPAN = 40f

        fun colorAlong(stops: List<Color>, t: Float): Color {
            val pos = t.coerceIn(0f, 1f) * (stops.size - 1)
            val i = pos.toInt().coerceAtMost(stops.size - 2)
            return lerp(stops[i], stops[i + 1], pos - i)
        }
    }
}

object LifeThemes {
    val all = listOf(
        LifeTheme(
            "Lush", Color(0xFF06140E),
            listOf(Color(0xFFE8FFB0), Color(0xFF9BE15D), Color(0xFF2FBF71), Color(0xFF138A72), Color(0xFF0E6B5C)),
            trail = Color(0xFF12402F), grid = Color(0x2236D399), accent = Color(0xFF7BE495),
        ),
        LifeTheme(
            "Neon", Color(0xFF07020F),
            listOf(Color(0xFFFFFFFF), Color(0xFF00F0FF), Color(0xFFFF2BD6), Color(0xFF8A2BFF), Color(0xFF4B1FA8)),
            trail = Color(0xFF2A0B4A), grid = Color(0x22FF2BD6), accent = Color(0xFFFF4FD8),
        ),
        LifeTheme(
            "Ocean", Color(0xFF021526),
            listOf(Color(0xFFE0FBFC), Color(0xFF8FE3F0), Color(0xFF3FA7D6), Color(0xFF1C6EA4), Color(0xFF0B4F80)),
            trail = Color(0xFF0A3150), grid = Color(0x223FA7D6), accent = Color(0xFF6CCFF6),
        ),
        LifeTheme(
            "Ember", Color(0xFF120404),
            listOf(Color(0xFFFFF6C2), Color(0xFFFFC94A), Color(0xFFFF7A2F), Color(0xFFD7263D), Color(0xFF7A0E1E)),
            trail = Color(0xFF3B0D08), grid = Color(0x22FF7A2F), accent = Color(0xFFFF9248),
        ),
        LifeTheme(
            "Aurora", Color(0xFF050B1A),
            listOf(Color(0xFFB8FFF2), Color(0xFF52F2A8), Color(0xFF2EC4E6), Color(0xFF7B5CFF), Color(0xFFE05CFF)),
            trail = Color(0xFF142046), grid = Color(0x2252F2A8), accent = Color(0xFF52F2A8),
        ),
        LifeTheme(
            "Candy", Color(0xFF1C1430),
            listOf(Color(0xFFFFF0F7), Color(0xFFFFA6D1), Color(0xFFFF6FB5), Color(0xFF9D8CFF), Color(0xFF6BCBFF)),
            trail = Color(0xFF3A2654), grid = Color(0x22FFA6D1), accent = Color(0xFFFF8CC6),
        ),
        LifeTheme(
            "Matrix", Color(0xFF000000),
            listOf(Color(0xFFE6FFE6), Color(0xFF39FF14), Color(0xFF00B32C), Color(0xFF006B1A)),
            trail = Color(0xFF032B0C), grid = Color(0x2200FF41), accent = Color(0xFF39FF14),
        ),
        LifeTheme(
            "Paper", Color(0xFFF6F1E7),
            listOf(Color(0xFFD1495B), Color(0xFF2E2A27), Color(0xFF3D405B), Color(0xFF5B6C8F)),
            trail = Color(0xFFE6DCC8), grid = Color(0x22000000), accent = Color(0xFFD1495B), isLight = true,
        ),
    )

    fun byName(name: String?) = all.firstOrNull { it.name == name } ?: all.first()
}
