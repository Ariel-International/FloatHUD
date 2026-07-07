package it.top.floathud

import android.view.View
import android.widget.TextView
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Plain always-on local-time display — the free mode of the "Floating Clock" flavor. */
class ClockController(view: View) : OverlayModeController {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val clockText: TextView = view.findViewById(R.id.overlayClockText)

    init {
        onTick()
    }

    override fun onTick() {
        clockText.text = ZonedDateTime.now().format(formatter)
    }

    override fun onDestroy() {}
}
