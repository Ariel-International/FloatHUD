package it.top.floathud

import android.view.View

/**
 * Hides a group of views a few seconds after being shown, so an overlay's chrome shrinks down to
 * just its main readout. [onTick] is meant to be driven off the same shared ticker each mode
 * controller already ticks on; [reveal] shows the views again and restarts the countdown.
 */
class AutoHideControls(
    private val views: List<View>,
    private val tickMs: Long,
    private val hideAfterMs: Long = 4000L
) {
    private var msSinceRevealed = 0L
    private var hidden = false

    fun reveal() {
        views.forEach { it.visibility = View.VISIBLE }
        msSinceRevealed = 0L
        hidden = false
    }

    fun onTick() {
        if (hidden) return
        msSinceRevealed += tickMs
        if (msSinceRevealed >= hideAfterMs) {
            views.forEach { it.visibility = View.GONE }
            hidden = true
        }
    }
}
