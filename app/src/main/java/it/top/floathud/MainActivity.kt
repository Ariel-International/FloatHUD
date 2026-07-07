package it.top.floathud

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var purchaseManager: PurchaseManager
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private lateinit var statusText: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var radioStopwatch: RadioButton
    private lateinit var radioCountdown: RadioButton
    private lateinit var radioWorldClock: RadioButton
    private lateinit var radioClock: RadioButton
    private lateinit var countdownConfig: LinearLayout
    private lateinit var countdownMinutesInput: EditText
    private lateinit var worldClockConfig: LinearLayout
    private lateinit var zoneSearchInput: AutoCompleteTextView
    private lateinit var addZoneButton: Button
    private lateinit var selectedZonesContainer: LinearLayout
    private lateinit var buyProButton: Button
    private lateinit var showOverlayButton: Button

    private val selectedZones = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        purchaseManager = PurchaseManager(this)
        requestNotificationPermissionIfNeeded()

        statusText = findViewById(R.id.statusText)
        modeGroup = findViewById(R.id.modeGroup)
        radioStopwatch = findViewById(R.id.radioStopwatch)
        radioCountdown = findViewById(R.id.radioCountdown)
        radioWorldClock = findViewById(R.id.radioWorldClock)
        radioClock = findViewById(R.id.radioClock)
        countdownConfig = findViewById(R.id.countdownConfig)
        countdownMinutesInput = findViewById(R.id.countdownMinutesInput)
        worldClockConfig = findViewById(R.id.worldClockConfig)
        zoneSearchInput = findViewById(R.id.zoneSearchInput)
        addZoneButton = findViewById(R.id.addZoneButton)
        selectedZonesContainer = findViewById(R.id.selectedZonesContainer)
        buyProButton = findViewById(R.id.buyProButton)
        showOverlayButton = findViewById(R.id.showOverlayButton)

        zoneSearchInput.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, ZoneId.getAvailableZoneIds().sorted())
        )

        selectedZones += prefs.worldClockZoneIds.ifEmpty { listOf(ZoneId.systemDefault().id) }
        renderSelectedZones()

        countdownMinutesInput.setText((prefs.countdownDurationMs / 60_000L).toString())

        radioButtonFor(prefs.lastMode).isChecked = true
        updateConfigVisibility()
        modeGroup.setOnCheckedChangeListener { _, _ -> updateConfigVisibility() }

        addZoneButton.setOnClickListener { addZone() }
        buyProButton.setOnClickListener { purchaseManager.purchase(this) }
        showOverlayButton.setOnClickListener { onShowOverlayClicked() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                purchaseManager.isPro.collect { updateProUi(it) }
            }
        }
        purchaseManager.connect()
    }

    override fun onResume() {
        super.onResume()
        statusText.text = if (OverlayPermission.isGranted(this)) {
            "Overlay permission granted"
        } else {
            "Overlay permission not granted yet — required to show the floating window"
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun updateConfigVisibility() {
        val mode = selectedMode()
        countdownConfig.visibility = if (mode == OverlayMode.COUNTDOWN) LinearLayout.VISIBLE else LinearLayout.GONE
        worldClockConfig.visibility = if (mode == OverlayMode.WORLD_CLOCK) LinearLayout.VISIBLE else LinearLayout.GONE
    }

    private fun selectedMode(): OverlayMode = when (modeGroup.checkedRadioButtonId) {
        R.id.radioCountdown -> OverlayMode.COUNTDOWN
        R.id.radioWorldClock -> OverlayMode.WORLD_CLOCK
        R.id.radioClock -> OverlayMode.CLOCK
        else -> OverlayMode.STOPWATCH
    }

    private fun radioButtonFor(mode: OverlayMode): RadioButton = when (mode) {
        OverlayMode.STOPWATCH -> radioStopwatch
        OverlayMode.COUNTDOWN -> radioCountdown
        OverlayMode.WORLD_CLOCK -> radioWorldClock
        OverlayMode.CLOCK -> radioClock
    }

    private fun onShowOverlayClicked() {
        val mode = selectedMode()
        val isPro = purchaseManager.isPro.value

        if (mode.requiresPro && !isPro) {
            Toast.makeText(this, "Unlock Pro to use this mode", Toast.LENGTH_SHORT).show()
            purchaseManager.purchase(this)
            return
        }

        if (!OverlayPermission.isGranted(this)) {
            Toast.makeText(
                this, "Please allow 'Display over other apps', then tap again", Toast.LENGTH_LONG
            ).show()
            startActivity(OverlayPermission.requestIntent(this))
            return
        }

        prefs.lastMode = mode
        val intent = Intent(this, OverlayService::class.java)
            .setAction(OverlayService.ACTION_ADD_OVERLAY)
            .putExtra(OverlayService.EXTRA_MODE, mode.name)

        when (mode) {
            OverlayMode.COUNTDOWN -> {
                val minutes = countdownMinutesInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 5
                val durationMs = minutes * 60_000L
                prefs.countdownDurationMs = durationMs
                intent.putExtra(OverlayService.EXTRA_COUNTDOWN_MS, durationMs)
            }
            OverlayMode.WORLD_CLOCK -> {
                prefs.worldClockZoneIds = selectedZones
                intent.putStringArrayListExtra(OverlayService.EXTRA_ZONE_IDS, ArrayList(selectedZones))
            }
            else -> {}
        }

        startForegroundService(intent)
        Toast.makeText(this, "Floating overlay shown", Toast.LENGTH_SHORT).show()
        // The whole point is to overlay whatever app the user was in — get out of the way.
        moveTaskToBack(true)
    }

    private fun addZone() {
        val text = zoneSearchInput.text.toString().trim()
        if (text.isEmpty()) return
        val zoneId = runCatching { ZoneId.of(text) }.getOrNull()
        if (zoneId == null) {
            Toast.makeText(this, "Unknown time zone", Toast.LENGTH_SHORT).show()
            return
        }
        if (!selectedZones.contains(zoneId.id)) {
            selectedZones += zoneId.id
            renderSelectedZones()
        }
        zoneSearchInput.text.clear()
    }

    private fun renderSelectedZones() {
        selectedZonesContainer.removeAllViews()
        selectedZones.forEach { id ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = id
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val remove = TextView(this).apply {
                text = "✕"
                setPadding(24, 8, 24, 8)
                setOnClickListener {
                    selectedZones.remove(id)
                    renderSelectedZones()
                }
            }
            row.addView(label)
            row.addView(remove)
            selectedZonesContainer.addView(row)
        }
    }

    private fun updateProUi(isPro: Boolean) {
        buyProButton.visibility = if (isPro) android.view.View.GONE else android.view.View.VISIBLE
        radioStopwatch.text = modeLabel(OverlayMode.STOPWATCH, isPro)
        radioCountdown.text = modeLabel(OverlayMode.COUNTDOWN, isPro)
        radioWorldClock.text = modeLabel(OverlayMode.WORLD_CLOCK, isPro)
        radioClock.text = modeLabel(OverlayMode.CLOCK, isPro)
    }

    private fun modeLabel(mode: OverlayMode, isPro: Boolean): String {
        val base = when (mode) {
            OverlayMode.STOPWATCH -> "Stopwatch"
            OverlayMode.COUNTDOWN -> "Countdown"
            OverlayMode.WORLD_CLOCK -> "World Clock"
            OverlayMode.CLOCK -> "Clock"
        }
        return if (mode.requiresPro && !isPro) "$base 🔒 Pro" else base
    }
}
