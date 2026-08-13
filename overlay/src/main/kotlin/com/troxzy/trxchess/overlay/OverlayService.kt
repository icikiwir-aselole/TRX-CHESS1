package com.troxzy.trxchess.overlay

import com.troxzy.trxchess.overlay.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.troxzy.trxchess.core.common.FeatureFlags

/**
 * Foreground overlay window showing the current analysis snapshot.
 *
 * The service only hosts and renders the window. Analysis data flows from the
 * app layer through [OverlayController]; the service never drives the engine.
 *
 * Lifecycle: the service starts when the user enables the overlay and stops
 * when it is disabled or the panel is dismissed. Permission is re-checked on
 * every start; without [Settings.canDrawOverlays] the service no-ops instead
 * of failing.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var panel: OverlayPanelView? = null
    private var started = false

    override fun onCreate() {
        super.onCreate()
        if (!FeatureFlags.overlay) return
        if (!Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        started = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!started || !Settings.canDrawOverlays(this)) return START_NOT_STICKY
        startAsForeground()
        if (panel == null) {
            attachPanel()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.overlay_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun attachPanel() {
        val wm = windowManager ?: return
        val controller = OverlayController.get()
        val view = OverlayPanelView(this, controller, null)
        view.windowUpdater = { lp -> runCatching { wm.updateViewLayout(view, lp) } }
        view.setWindowWidth(resources.displayMetrics.widthPixels)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 24
        params.y = 180

        view.layoutParams = params
        runCatching { wm.addView(view, params) }.onFailure {
            panel = null
            return
        }
        panel = view
    }

    override fun onDestroy() {
        val wm = windowManager
        panel?.let { view -> runCatching { wm?.removeView(view) } }
        panel = null
        windowManager = null
        started = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "trx_overlay"
        private const val NOTIFICATION_ID = 0x5448 // "TH"
    }
}