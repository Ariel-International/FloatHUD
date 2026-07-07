package it.top.floathud

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Ticks one or more time zones using java.time (DST-correct automatically, no manual UTC math).
 */
class WorldClockController(
    view: View,
    zoneIds: List<ZoneId>
) : OverlayModeController {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val rows: List<Pair<ZoneId, TextView>>

    init {
        val container: LinearLayout = view.findViewById(R.id.worldClockContainer)
        rows = zoneIds.map { zoneId ->
            val row = LinearLayout(view.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(view.context).apply {
                text = zoneId.id.substringAfterLast('/').replace('_', ' ')
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val time = TextView(view.context).apply {
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                typeface = android.graphics.Typeface.MONOSPACE
            }
            row.addView(label)
            row.addView(time)
            container.addView(row)
            zoneId to time
        }
        onTick()
    }

    override fun onTick() {
        rows.forEach { (zoneId, textView) ->
            textView.text = ZonedDateTime.now(zoneId).format(formatter)
        }
    }

    override fun onDestroy() {}
}
