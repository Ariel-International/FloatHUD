package it.top.floathud

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

/**
 * Counts down from [durationMs] to zero, then alarms (vibrate + ringtone) until dismissed. The
 * button row auto-hides a few seconds after being shown (paused while the alarm is ringing, so
 * Dismiss never disappears on you); [reveal] (wired to a tap on the overlay, and to any button
 * press) brings it back and resets the countdown.
 */
class CountdownController(
    context: Context,
    view: View,
    private val durationMs: Long,
    tickMs: Long
) : OverlayModeController {

    private val appContext = context.applicationContext
    private val timerText: TextView = view.findViewById(R.id.overlayTimerText)
    private val normalControls: View = view.findViewById(R.id.overlayNormalControls)
    private val startStopButton: ImageButton = view.findViewById(R.id.overlayStartStop)
    private val resetButton: ImageButton = view.findViewById(R.id.overlayReset)
    private val dismissButton: Button = view.findViewById(R.id.overlayDismiss)
    private val closeButton: View = view.findViewById(R.id.overlayClose)
    private val autoHide = AutoHideControls(listOf(normalControls, closeButton), tickMs)

    private var running = false
    private var remainingMs = durationMs
    private var lastTickBase = SystemClock.elapsedRealtime()
    private var alarming = false
    private var ringtone: Ringtone? = null

    init {
        startStopButton.setOnClickListener { toggle() }
        resetButton.setOnClickListener { reset() }
        dismissButton.setOnClickListener { dismissAlarm() }
        updateText()
    }

    fun reveal() {
        if (!alarming) autoHide.reveal()
    }

    private fun toggle() {
        running = !running
        setStartStopIcon(running)
        if (running) lastTickBase = SystemClock.elapsedRealtime()
        autoHide.reveal()
    }

    private fun reset() {
        dismissAlarm()
        remainingMs = durationMs
        lastTickBase = SystemClock.elapsedRealtime()
        running = false
        setStartStopIcon(false)
        updateText()
        autoHide.reveal()
    }

    private fun setStartStopIcon(isRunning: Boolean) {
        startStopButton.setImageResource(
            if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        startStopButton.contentDescription = if (isRunning) "Stop" else "Start"
    }

    override fun onTick() {
        if (running && !alarming) {
            val now = SystemClock.elapsedRealtime()
            remainingMs -= (now - lastTickBase)
            lastTickBase = now
            if (remainingMs <= 0) {
                remainingMs = 0
                running = false
                triggerAlarm()
            }
            updateText()
        }
        if (!alarming) autoHide.onTick()
    }

    private fun triggerAlarm() {
        alarming = true
        normalControls.visibility = View.GONE
        closeButton.visibility = View.VISIBLE
        dismissButton.visibility = View.VISIBLE

        val vibrator = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), 0))

        ringtone = RingtoneManager.getRingtone(
            appContext,
            RingtoneManager.getActualDefaultRingtoneUri(appContext, RingtoneManager.TYPE_ALARM)
        )?.apply { play() }
    }

    private fun dismissAlarm() {
        if (!alarming) return
        alarming = false
        ringtone?.stop()
        ringtone = null
        (appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
        dismissButton.visibility = View.GONE
        autoHide.reveal()
    }

    private fun updateText() {
        val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
        timerText.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {
        ringtone?.stop()
        (appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.cancel()
    }
}
