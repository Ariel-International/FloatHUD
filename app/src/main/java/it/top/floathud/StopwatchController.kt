package it.top.floathud

import android.os.SystemClock
import android.view.View
import android.widget.ImageButton
import android.widget.TextView

/** mm:ss stopwatch — direct port of the original OverlayService's counting logic. */
class StopwatchController(view: View) : OverlayModeController {

    private val timerText: TextView = view.findViewById(R.id.overlayTimerText)
    private val startStopButton: ImageButton = view.findViewById(R.id.overlayStartStop)
    private val resetButton: ImageButton = view.findViewById(R.id.overlayReset)

    private var running = false
    private var elapsedMs = 0L
    private var lastTickBase = SystemClock.elapsedRealtime()

    init {
        startStopButton.setOnClickListener { toggle() }
        resetButton.setOnClickListener { reset() }
        updateText()
    }

    private fun toggle() {
        running = !running
        startStopButton.setImageResource(
            if (running) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        startStopButton.contentDescription = if (running) "Stop" else "Start"
        if (running) lastTickBase = SystemClock.elapsedRealtime()
    }

    private fun reset() {
        elapsedMs = 0L
        lastTickBase = SystemClock.elapsedRealtime()
        updateText()
    }

    override fun onTick() {
        if (!running) return
        val now = SystemClock.elapsedRealtime()
        elapsedMs += now - lastTickBase
        lastTickBase = now
        updateText()
    }

    private fun updateText() {
        val totalSeconds = elapsedMs / 1000
        timerText.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {}
}
