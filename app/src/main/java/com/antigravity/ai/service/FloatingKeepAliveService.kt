package com.antigravity.ai.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.antigravity.ai.MainActivity

class FloatingKeepAliveService : Service() {

    companion object {
        const val CHANNEL_ID = "antigravity_keepalive"
        const val NOTIFICATION_ID = 9921
        const val ACTION_STOP = "com.antigravity.ai.STOP_KEEPALIVE"
        const val PREFS_NAME = "antigravity_floating_prefs"
        const val KEY_ENABLED = "floating_keepalive_enabled"
        const val KEY_MODE = "floating_mode" // "invisible" or "pill"

        var isServiceRunning = false
            private set

        fun startKeepAlive(context: Context) {
            val intent = Intent(context, FloatingKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopKeepAlive(context: Context) {
            val intent = Intent(context, FloatingKeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun canDrawOverlays(context: Context): Boolean {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var prefs: SharedPreferences? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        initOverlayWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Antigravity Arka Plan & Dondurma Koruması",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Termux Node.js ve AI oturumunun arka planda dondurulmasını engeller."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingKeepAliveService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Antigravity AI (Dondurma Koruması)")
            .setContentText("Ön plan yüzen servis aktif • Termux bağlantısı korunuyor")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Kapat", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlayWindow() {
        if (!canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val mode = prefs?.getString(KEY_MODE, "invisible") ?: "invisible"

        if (mode == "invisible") {
            // Invisible 1x1 pixel overlay (Zero UI intrusion, 100% foreground classification)
            val invisibleView = View(this).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
            overlayView = invisibleView

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                1, 1,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            try {
                windowManager?.addView(invisibleView, params)
            } catch (e: Exception) {}
        } else {
            // Compact Floating Pill Mode (Draggable Badge)
            val density = resources.displayMetrics.density
            val pillContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding((8 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
                gravity = Gravity.CENTER_VERTICAL

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 20 * density
                    setColor(Color.parseColor("#E61E1F22"))
                    setStroke((1.5f * density).toInt(), Color.parseColor("#388E3C"))
                }
                background = bg
            }

            val statusDot = View(this).apply {
                val dotBg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#4CAF50"))
                }
                background = dotBg
                val size = (8 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (6 * density).toInt()
                }
            }

            val text = TextView(this).apply {
                setText("⚡ AGY")
                setTextColor(Color.WHITE)
                textSize = 11f
            }

            pillContainer.addView(statusDot)
            pillContainer.addView(text)
            overlayView = pillContainer

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (resources.displayMetrics.widthPixels - (80 * density)).toInt()
                y = (resources.displayMetrics.heightPixels * 0.35f).toInt()
            }

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isClick = false

            pillContainer.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(pillContainer, params)
                        } catch (e: Exception) {}
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            val launchIntent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(launchIntent)
                        }
                        true
                    }
                    else -> false
                }
            }

            try {
                windowManager?.addView(pillContainer, params)
            } catch (e: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {}
        }
    }
}
