package it.top.floathud

import android.os.SystemClock
import android.view.View
import android.widget.ImageButton
import android.widget.TextView

/**
 * mm:ss stopwatch — direct port of the original OverlayService's counting logic. The button row
 * auto-hides a few seconds after being shown; [reveal] (wired to a tap on the overlay, and to
 * any button press) brings it back and resets the countdown.
 */
class StopwatchController(view: View, tickMs: Long) : OverlayModeController {

    private val timerText: TextView = view.findViewById(R.id.overlayTimerText)
    private val startStopButton: ImageButton = view.findViewById(R.id.overlayStartStop)
    private val resetButton: ImageButton = view.findViewById(R.id.overlayReset)
    private val closeButton: View = view.findViewById(R.id.overlayClose)
    private val autoHide = AutoHideControls(listOf(startStopButton, resetButton, closeButton), tickMs)

    private var running = false
    private var elapsedMs = 0L
    private var lastTickBase = SystemClock.elapsedRealtime()

    init {
        startStopButton.setOnClickListener { toggle() }
        resetButton.setOnClickListener { reset() }
        updateText()
    }

    fun reveal() = autoHide.reveal()

    private fun toggle() {
        running = !running
        startStopButton.setImageResource(
            if (running) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        startStopButton.contentDescription = if (running) "Stop" else "Start"
        if (running) lastTickBase = SystemClock.elapsedRealtime()
        autoHide.reveal()
    }

    private fun reset() {
        elapsedMs = 0L
        lastTickBase = SystemClock.elapsedRealtime()
        updateText()
        autoHide.reveal()
    }

    override fun onTick() {
        if (running) {
            val now = SystemClock.elapsedRealtime()
            elapsedMs += now - lastTickBase
            lastTickBase = now
            updateText()
        }
        autoHide.onTick()
    }

    private fun updateText() {
        val totalSeconds = elapsedMs / 1000
        timerText.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {}
}
