package it.top.floathud

/** Drives the content of one overlay window. Ticked on a shared timer while its window is shown. */
interface OverlayModeController {
    fun onTick()
    fun onDestroy()
}
