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
class ClockController(view: View, tickMs: Long) : OverlayModeController {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val clockText: TextView = view.findViewById(R.id.overlayClockText)
    private val closeButton: View = view.findViewById(R.id.overlayClose)
    private val autoHide = AutoHideControls(listOf(closeButton), tickMs)

    init {
        onTick()
    }

    fun reveal() = autoHide.reveal()

    override fun onTick() {
        clockText.text = ZonedDateTime.now().format(formatter)
        autoHide.onTick()
    }

    override fun onDestroy() {}
}
