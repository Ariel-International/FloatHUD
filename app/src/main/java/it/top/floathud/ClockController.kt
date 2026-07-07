package it.top.floathud

import android.view.View
import android.widget.TextView
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Plain always-on local-time display — the free mode of the "Floating Clock" flavor. The close
 * button auto-hides after a few seconds so the overlay shrinks down to just the time; [reveal]
 * (wired to a tap on the overlay) brings it back and resets the countdown.
 */
class ClockController(view: View, private val tickMs: Long) : OverlayModeController {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val clockText: TextView = view.findViewById(R.id.overlayClockText)
    private val closeButton: View = view.findViewById(R.id.overlayClose)

    private var msSinceRevealed = 0L

    init {
        onTick()
    }

    fun reveal() {
        closeButton.visibility = View.VISIBLE
        msSinceRevealed = 0L
    }

    override fun onTick() {
        clockText.text = ZonedDateTime.now().format(formatter)
        if (closeButton.visibility == View.VISIBLE) {
            msSinceRevealed += tickMs
            if (msSinceRevealed >= HIDE_AFTER_MS) closeButton.visibility = View.GONE
        }
    }

    override fun onDestroy() {}

    companion object {
        private const val HIDE_AFTER_MS = 4000L
    }
}
