package com.vibedev.app.ui

import android.content.Context

/** Remembers the author's choices between launches. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("lush_life", Context.MODE_PRIVATE)

    var theme: String?
        get() = prefs.getString("theme", null)
        set(v) = prefs.edit().putString("theme", v).apply()
    var rule: String?
        get() = prefs.getString("rule", null)
        set(v) = prefs.edit().putString("rule", v).apply()
    var pattern: String?
        get() = prefs.getString("pattern", null)
        set(v) = prefs.edit().putString("pattern", v).apply()
    var speed: Int
        get() = prefs.getInt("speed", 20)
        set(v) = prefs.edit().putInt("speed", v).apply()
    var trails: Boolean
        get() = prefs.getBoolean("trails", true)
        set(v) = prefs.edit().putBoolean("trails", v).apply()
    var gridLines: Boolean
        get() = prefs.getBoolean("grid", true)
        set(v) = prefs.edit().putBoolean("grid", v).apply()
    var wrap: Boolean
        get() = prefs.getBoolean("wrap", true)
        set(v) = prefs.edit().putBoolean("wrap", v).apply()
    var worldSize: Int
        get() = prefs.getInt("worldSize", 1)
        set(v) = prefs.edit().putInt("worldSize", v).apply()
}
