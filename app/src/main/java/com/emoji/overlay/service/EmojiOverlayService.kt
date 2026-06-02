package com.emoji.overlay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.emoji.overlay.MainActivity
import com.emoji.overlay.R
import com.emoji.overlay.gesture.OverlayTouchListener
import com.emoji.overlay.ui.EmojiPanel

/**
 * Foreground service that manages the emoji overlay window.
 *
 * The overlay is a system-level window (TYPE_APPLICATION_OVERLAY) that:
 * - Sits above all apps including the keyboard
 * - Detects two-finger double-tap to toggle visibility
 * - Dismisses when tapping outside the emoji panel
 * - Does NOT interfere with the underlying app's input or gestures
 */
class EmojiOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "emoji_overlay_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_TOGGLE = "com.emoji.overlay.TOGGLE"
        const val ACTION_HIDE = "com.emoji.overlay.HIDE"
        const val ACTION_SHOW = "com.emoji.overlay.SHOW"
        const val ACTION_STOP = "com.emoji.overlay.STOP"

        private var isRunning = false

        fun isActive(): Boolean = isRunning

        fun toggle(context: Context) {
            val intent = Intent(context, EmojiOverlayService::class.java).apply {
                action = ACTION_TOGGLE
            }
            context.startService(intent)
        }

        fun show(context: Context) {
            val intent = Intent(context, EmojiOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            val intent = Intent(context, EmojiOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, EmojiOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var touchInterceptorView: View? = null
    private var overlayTouchListener: OverlayTouchListener? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var isPanelVisible by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> toggleOverlay()
            ACTION_HIDE -> hideOverlay()
            ACTION_SHOW -> showOverlay()
            ACTION_STOP -> {
                hideOverlay()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Emoji overlay service notification"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlay() {
        if (overlayView != null) return

        startForegroundWithNotification()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val screenHeight = getScreenHeight()
        val panelHeight = (screenHeight * 0.4).toInt()

        // --- Touch interceptor view (full screen, transparent) ---
        // Captures all touches for gesture detection and outside-tap dismissal.
        // FLAG_NOT_TOUCH_MODAL allows touches to pass through to underlying windows
        // when the interceptor doesn't consume them.
        val touchParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val interceptor = FrameLayout(this)
        touchInterceptorView = interceptor

        // --- Emoji panel view (bottom portion, Compose) ---
        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            panelHeight,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@EmojiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@EmojiOverlayService)
            setContent {
                EmojiPanel(
                    visible = isPanelVisible,
                    onEmojiSelected = { emoji ->
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("emoji", emoji)
                        clipboard.setPrimaryClip(clip)
                    }
                )
            }
        }

        val panelContainer = FrameLayout(this).apply {
            addView(composeView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        overlayView = panelContainer

        // Set up touch listener on the interceptor
        overlayTouchListener = OverlayTouchListener(
            panelView = panelContainer,
            onToggleOverlay = {
                if (isPanelVisible) hideOverlay() else showOverlay()
            },
            onDismissOverlay = {
                hideOverlay()
            }
        )
        interceptor.setOnTouchListener(overlayTouchListener)

        windowManager.addView(interceptor, touchParams)
        windowManager.addView(panelContainer, panelParams)
    }

    private fun showOverlay() {
        if (overlayView == null) createOverlay()
        isPanelVisible = true
    }

    private fun hideOverlay() {
        isPanelVisible = false
    }

    private fun toggleOverlay() {
        if (isPanelVisible) hideOverlay() else showOverlay()
    }

    private fun removeOverlay() {
        overlayTouchListener?.destroy()
        overlayTouchListener = null

        touchInterceptorView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        touchInterceptorView = null

        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null

        isPanelVisible = false
    }

    private fun getScreenHeight(): Int {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.heightPixels
    }

    @Suppress("DEPRECATION")
    private fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }
}
