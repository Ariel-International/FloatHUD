package it.top.floathud

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** SYSTEM_ALERT_WINDOW ("Display over other apps") permission flow, shared by every mode. */
object OverlayPermission {

    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun requestIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )
}
