package it.top.floathud

import android.content.Context

/** Persists user choices: entitlement cache, last mode used, and per-mode configuration. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("floathud_prefs", Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = sp.getBoolean(KEY_PRO, false)
        set(value) = sp.edit().putBoolean(KEY_PRO, value).apply()

    var lastMode: OverlayMode
        get() = runCatching { OverlayMode.valueOf(sp.getString(KEY_MODE, null) ?: "") }
            .getOrDefault(OverlayMode.freeMode)
        set(value) = sp.edit().putString(KEY_MODE, value.name).apply()

    var countdownDurationMs: Long
        get() = sp.getLong(KEY_COUNTDOWN_MS, DEFAULT_COUNTDOWN_MS)
        set(value) = sp.edit().putLong(KEY_COUNTDOWN_MS, value).apply()

    var worldClockZoneIds: List<String>
        get() = sp.getString(KEY_ZONES, "")!!.split(",").filter { it.isNotBlank() }
        set(value) = sp.edit().putString(KEY_ZONES, value.joinToString(",")).apply()

    companion object {
        private const val KEY_PRO = "is_pro"
        private const val KEY_MODE = "last_mode"
        private const val KEY_COUNTDOWN_MS = "countdown_duration_ms"
        private const val KEY_ZONES = "world_clock_zones"
        private const val DEFAULT_COUNTDOWN_MS = 5 * 60 * 1000L
    }
}
