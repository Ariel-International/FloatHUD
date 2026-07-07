package it.top.floathud

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.time.ZoneId
import java.util.UUID

/**
 * Hosts every currently-shown floating overlay window. A single instance manages all of them
 * (one per [OverlayWindow]) so the free tier can be capped at one, and Pro can stack several at
 * once, without needing multiple Service processes.
 */
class OverlayService : Service() {

    private class OverlayWindow(
        val view: View,
        val params: WindowManager.LayoutParams,
        val controller: OverlayModeController
    )

    private lateinit var windowManager: WindowManager
    private val windows = mutableMapOf<String, OverlayWindow>()
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            windows.values.forEach { it.controller.onTick() }
            if (windows.isNotEmpty()) handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_OVERLAY -> handleAdd(intent)
            ACTION_REMOVE_OVERLAY -> {
                val id = intent.getStringExtra(EXTRA_INSTANCE_ID)
                if (id != null) removeOverlay(id) else removeAll()
            }
        }
        if (windows.isEmpty()) stopSelf()
        return START_NOT_STICKY
    }

    private fun handleAdd(intent: Intent) {
        val mode = intent.getStringExtra(EXTRA_MODE)?.let {
            runCatching { OverlayMode.valueOf(it) }.getOrNull()
        } ?: OverlayMode.freeMode

        val prefs = Prefs(this)
        // Defense in depth: MainActivity already gates this before sending the intent.
        if (!prefs.isPro && (mode.requiresPro || windows.isNotEmpty())) return

        val instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID) ?: UUID.randomUUID().toString()
        addOverlay(instanceId, mode, intent)
    }

    private fun addOverlay(instanceId: String, mode: OverlayMode, intent: Intent) {
        val layoutRes = when (mode) {
            OverlayMode.STOPWATCH -> R.layout.overlay_stopwatch
            OverlayMode.COUNTDOWN -> R.layout.overlay_countdown
            OverlayMode.WORLD_CLOCK -> R.layout.overlay_world_clock
            OverlayMode.CLOCK -> R.layout.overlay_clock
        }
        val view = LayoutInflater.from(this).inflate(layoutRes, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        // Cascade new windows so stacked overlays (Pro) don't land exactly on top of each other.
        val cascade = windows.size * 40
        params.x = 40 + cascade
        params.y = 120 + cascade

        val controller: OverlayModeController = when (mode) {
            OverlayMode.STOPWATCH -> StopwatchController(view)
            OverlayMode.COUNTDOWN -> {
                val durationMs = intent.getLongExtra(EXTRA_COUNTDOWN_MS, Prefs(this).countdownDurationMs)
                CountdownController(this, view, durationMs)
            }
            OverlayMode.WORLD_CLOCK -> {
                val zoneIds = intent.getStringArrayListExtra(EXTRA_ZONE_IDS)
                    ?.takeIf { it.isNotEmpty() }
                    ?: arrayListOf(ZoneId.systemDefault().id)
                WorldClockController(view, zoneIds.map { ZoneId.of(it) })
            }
            OverlayMode.CLOCK -> ClockController(view)
        }

        view.findViewById<View>(R.id.overlayClose)?.setOnClickListener { removeOverlay(instanceId) }
        enableDrag(view, params)

        windowManager.addView(view, params)
        windows[instanceId] = OverlayWindow(view, params, controller)

        if (windows.size == 1) handler.post(ticker)
    }

    private fun removeOverlay(instanceId: String) {
        windows.remove(instanceId)?.let {
            windowManager.removeView(it.view)
            it.controller.onDestroy()
        }
        if (windows.isEmpty()) stopSelf()
    }

    private fun removeAll() {
        windows.keys.toList().forEach { removeOverlay(it) }
    }

    private fun enableDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    false // let button clicks still register
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        removeAll()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_ADD_OVERLAY = "it.top.floathud.ADD_OVERLAY"
        const val ACTION_REMOVE_OVERLAY = "it.top.floathud.REMOVE_OVERLAY"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_INSTANCE_ID = "extra_instance_id"
        const val EXTRA_COUNTDOWN_MS = "extra_countdown_ms"
        const val EXTRA_ZONE_IDS = "extra_zone_ids"
        private const val TICK_MS = 250L
    }
}
