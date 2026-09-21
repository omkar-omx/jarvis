package com.jarvis.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.jarvis.app.MainActivity

/**
 * Floating Assistant Overlay Service.
 * Displays a Google Assistant style floating holographic HUD over any active application
 * when J.A.R.V.I.S. is summoned via "Hey Jarvis" or floating trigger.
 */
class JarvisAssistantOverlayService : Service() {

    companion object {
        private const val TAG = "JarvisOverlayService"

        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlays(this)) {
            Log.w(TAG, "Cannot draw overlays: SYSTEM_ALERT_WINDOW permission not granted.")
            stopSelf()
            return START_NOT_STICKY
        }

        showAssistantOverlay()
        return START_NOT_STICKY
    }

    private fun showAssistantOverlay() {
        if (overlayView != null) return

        try {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 120
            }

            // Create simple dynamic overlay container
            val container = TextView(this).apply {
                text = "⚡ J.A.R.V.I.S. LISTENING..."
                setTextColor(android.graphics.Color.CYAN)
                setBackgroundColor(android.graphics.Color.argb(220, 5, 11, 23))
                setPadding(36, 20, 36, 20)
                textSize = 14f
                setOnClickListener {
                    val activityIntent = Intent(this@JarvisAssistantOverlayService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(activityIntent)
                    removeOverlay()
                }
            }

            overlayView = container
            windowManager?.addView(container, params)
            Log.i(TAG, "Assistant floating overlay rendered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render floating overlay", e)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
