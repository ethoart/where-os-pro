package com.watchlauncher

import android.content.Context
import android.graphics.Color

enum class WatchTheme(val id: String, val displayName: String) {
    NOTHING("nothing", "Nothing OS"),
    RETRO("retro", "Retro Dot"),
    NEON("neon", "Neon Glow"),
    BOLD("bold", "Bold Color"),
    MINIMAL("minimal", "Minimal");
    companion object { fun fromId(id: String) = values().find { it.id == id } ?: NOTHING }
}

enum class WatchFaceStyle(val id: String, val displayName: String) {
    DIGITAL_NOTHING("digital_nothing", "Nothing Digital"),
    RETRO_DOT("retro_dot", "Retro Dot Matrix"),
    HALF_PAST("half_past", "Half Past"),
    BOLD_BLOCK("bold_block", "Bold Block"),
    MINIMAL_HANDS("minimal_hands", "Minimal Hands"),
    NEON_GLOW("neon_glow", "Neon Glow"),
    TYPE_CLOCK("type_clock", "Type Clock");
    companion object { fun fromId(id: String) = values().find { it.id == id } ?: DIGITAL_NOTHING }
}

// ── Only 5 guaranteed fields — all extras are computed via extension functions ──
data class ThemeColors(
    val background: Int,
    val primary: Int,
    val accent: Int,
    val textPrimary: Int,
    val textSecondary: Int
)

// Computed helpers — no stored fields to go out of sync
fun ThemeColors.surface(): Int {
    val r = (Color.red(background) + 30).coerceAtMost(255)
    val g = (Color.green(background) + 30).coerceAtMost(255)
    val b = (Color.blue(background) + 30).coerceAtMost(255)
    return Color.rgb(r, g, b)
}
fun ThemeColors.iconBackground(): Int = Color.argb(50, Color.red(primary), Color.green(primary), Color.blue(primary))
fun ThemeColors.dockBackground(): Int = Color.argb(180, Color.red(background), Color.green(background), Color.blue(background))

object ThemeManager {
    private const val PREFS = "launcher_prefs"

    fun getTheme(ctx: Context): WatchTheme =
        WatchTheme.fromId(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("theme", WatchTheme.NOTHING.id) ?: WatchTheme.NOTHING.id)
    fun setTheme(ctx: Context, t: WatchTheme) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("theme", t.id).apply()

    fun getWatchFace(ctx: Context): WatchFaceStyle =
        WatchFaceStyle.fromId(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("watch_face", WatchFaceStyle.DIGITAL_NOTHING.id) ?: WatchFaceStyle.DIGITAL_NOTHING.id)
    fun setWatchFace(ctx: Context, f: WatchFaceStyle) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("watch_face", f.id).apply()

    fun getGridCols(ctx: Context): Int =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("grid_cols", 3)
    fun setGridCols(ctx: Context, c: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("grid_cols", c).apply()

    fun getShowLabels(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("show_labels", true)
    fun setShowLabels(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("show_labels", v).apply()

    fun getColors(theme: WatchTheme): ThemeColors = when (theme) {
        WatchTheme.NOTHING -> ThemeColors(
            background  = Color.parseColor("#0A0A0A"),
            primary     = Color.WHITE,
            accent      = Color.parseColor("#FF4444"),
            textPrimary = Color.WHITE,
            textSecondary = Color.parseColor("#888888")
        )
        WatchTheme.RETRO -> ThemeColors(
            background  = Color.parseColor("#0A0F05"),
            primary     = Color.parseColor("#39FF14"),
            accent      = Color.parseColor("#FFFF00"),
            textPrimary = Color.parseColor("#39FF14"),
            textSecondary = Color.parseColor("#1A7A00")
        )
        WatchTheme.NEON -> ThemeColors(
            background  = Color.parseColor("#050010"),
            primary     = Color.parseColor("#FF00FF"),
            accent      = Color.parseColor("#00FFFF"),
            textPrimary = Color.parseColor("#FF00FF"),
            textSecondary = Color.parseColor("#8800AA")
        )
        WatchTheme.BOLD -> ThemeColors(
            background  = Color.parseColor("#FF3B00"),
            primary     = Color.WHITE,
            accent      = Color.parseColor("#FFDD00"),
            textPrimary = Color.WHITE,
            textSecondary = Color.parseColor("#FFAA88")
        )
        WatchTheme.MINIMAL -> ThemeColors(
            background  = Color.parseColor("#F5F5F0"),
            primary     = Color.parseColor("#111111"),
            accent      = Color.parseColor("#2244FF"),
            textPrimary = Color.parseColor("#111111"),
            textSecondary = Color.parseColor("#777777")
        )
    }
}
