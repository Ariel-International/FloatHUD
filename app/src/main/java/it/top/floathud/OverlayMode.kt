package it.top.floathud

enum class OverlayMode {
    STOPWATCH,
    COUNTDOWN,
    WORLD_CLOCK,
    CLOCK;

    /**
     * Each build flavor picks one mode as its free "home" mode (BuildConfig.FREE_MODE);
     * the other three require the Pro purchase in that flavor's app.
     */
    val requiresPro: Boolean get() = name != BuildConfig.FREE_MODE

    companion object {
        val freeMode: OverlayMode get() = valueOf(BuildConfig.FREE_MODE)
    }
}
