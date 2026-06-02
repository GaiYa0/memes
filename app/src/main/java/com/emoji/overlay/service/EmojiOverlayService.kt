package com.emoji.overlay.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Notification.Action
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.emoji.overlay.R
import com.emoji.overlay.data.repository.EmojiRepositoryHolder
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.gesture.OverlayTouchListener
import com.emoji.overlay.gesture.PanelDragController
import com.emoji.overlay.send.manager.EmojiSendManager
import com.emoji.overlay.ui.EmojiPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that manages the emoji overlay window.
 *
 * Two overlay windows:
 * - Full-screen dismiss catcher (touchable only when panel fully expanded)
 * - Bottom panel window (Compose UI, draggable via translationY)
 */
class EmojiOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "emoji_overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val PANEL_ANIM_MS = 260L

        const val ACTION_TOGGLE = "com.emoji.overlay.TOGGLE"
        const val ACTION_HIDE = "com.emoji.overlay.HIDE"
        const val ACTION_SHOW = "com.emoji.overlay.SHOW"
        const val ACTION_STOP = "com.emoji.overlay.STOP"

        @Volatile
        private var isRunning = false

        fun isActive(): Boolean = isRunning

        fun show(context: Context) {
            val intent = Intent(context, EmojiOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
    private var overlayRootView: View? = null
    private var overlayTouchListener: OverlayTouchListener? = null
    private var panelContainerView: FrameLayout? = null
    private var foregroundStarted = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val serviceTag = "EmojiOverlayService"
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var isPanelVisible by mutableStateOf(false)
    private var panelOffsetY by mutableFloatStateOf(0f)
    private var panelHeightPx: Int = 0
    private var panelAnimator: ValueAnimator? = null
    /** Live translation during drag; may differ from [panelOffsetY] until drag ends. */
    private var dragOffsetPx: Float = 0f
    private var dismissCapturePassThrough: Boolean = false

    private val sendManager: EmojiSendManager by lazy {
        val appContext = applicationContext
        EmojiSendManager(
            context = appContext,
            repository = EmojiRepositoryHolder.getRepository(appContext),
            resourceManager = ResourceManager.getInstance(appContext)
        )
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> {
                if (!toggleOverlay()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            ACTION_HIDE -> dismissOverlayAnimated()
            ACTION_SHOW -> {
                if (!startForegroundWithNotification()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!showOverlay()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            ACTION_STOP -> {
                dismissOverlayAnimated {
                    stopSelf()
                }
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification(): Boolean {
        if (foregroundStarted) return true
        try {
            createNotificationChannel()
            val notification = buildNotification()
            startForeground(NOTIFICATION_ID, notification)
            foregroundStarted = true
            return true
        } catch (e: Exception) {
            Log.e(serviceTag, "Failed to start foreground", e)
            return false
        }
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
                enableVibration(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, EmojiOverlayService::class.java).apply { action = ACTION_SHOW },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, EmojiOverlayService::class.java).apply { action = ACTION_SHOW },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hidePendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, EmojiOverlayService::class.java).apply { action = ACTION_HIDE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, EmojiOverlayService::class.java).apply { action = ACTION_STOP },
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .addAction(
                Action.Builder(
                    null,
                    if (isPanelVisible) "隐藏面板" else "打开面板",
                    if (isPanelVisible) hidePendingIntent else openPendingIntent
                ).build()
            )
            .addAction(Action.Builder(null, "停止服务", stopPendingIntent).build())
            .build()
    }

    private fun refreshNotification() {
        if (!foregroundStarted) return
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(serviceTag, "Failed to refresh notification", e)
        }
    }

    private fun baseWindowFlags(): Int {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlay(): Boolean {
        if (overlayRootView != null) return true

        if (!Settings.canDrawOverlays(this)) {
            Log.w(serviceTag, "Overlay permission missing, skip createOverlay")
            return false
        }
        if (!startForegroundWithNotification()) {
            Log.e(serviceTag, "Foreground service not ready, skip createOverlay")
            return false
        }
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val screenHeight = getScreenHeight()
        panelHeightPx = (screenHeight * 0.5).toInt()

        val windowType = getWindowType()

        val dismissParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            baseWindowFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            panelHeightPx,
            windowType,
            baseWindowFlags(),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val rootContainer = FrameLayout(this).apply {
            visibility = View.VISIBLE
            setViewTreeLifecycleOwner(this@EmojiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@EmojiOverlayService)
        }

        val panelContainer = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(this@EmojiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@EmojiOverlayService)
            visibility = View.GONE
        }
        panelContainerView = panelContainer

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@EmojiOverlayService)
            setViewTreeSavedStateRegistryOwner(this@EmojiOverlayService)
            setContent {
                EmojiPanel(
                    visible = isPanelVisible,
                    panelHeightPx = panelHeightPx,
                    onPanelDragStart = ::onPanelDragStart,
                    onPanelOffsetChange = ::onPanelDragOffset,
                    onDragEnd = ::handlePanelDragEnd,
                    onEmojiSelected = { emoji ->
                        serviceScope.launch {
                            val success = sendManager.sendViaShare(emoji)
                            if (success) {
                                dismissOverlayAnimated()
                            } else {
                                Toast.makeText(
                                    this@EmojiOverlayService,
                                    "分享失败，请确认已安装 QQ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }

        panelContainer.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        overlayTouchListener = OverlayTouchListener(
            getPanelBounds = ::getPanelBoundsOnScreen,
            isPanelVisible = { isPanelVisible },
            onToggleOverlay = {
                if (isPanelVisible) dismissOverlayAnimated() else showOverlay()
            },
            onDismissOverlayAnimated = { dismissOverlayAnimated() }
        )
        rootContainer.setOnTouchListener(overlayTouchListener)

        overlayRootView = rootContainer
        try {
            windowManager.addView(rootContainer, dismissParams)
            windowManager.addView(panelContainer, panelParams)
        } catch (e: Exception) {
            Log.e(serviceTag, "addView failed", e)
            overlayRootView = null
            panelContainerView = null
            overlayTouchListener?.destroy()
            overlayTouchListener = null
            return false
        }
        return true
    }

    private fun getPanelBoundsOnScreen(): Rect {
        val panel = panelContainerView ?: return Rect()
        val loc = IntArray(2)
        panel.getLocationOnScreen(loc)
        val top = loc[1] + panel.translationY.toInt()
        return Rect(
            loc[0],
            top,
            loc[0] + panel.width,
            top + panel.height
        )
    }

    private fun onPanelDragStart(): Float {
        panelAnimator?.cancel()
        return dragOffsetPx
    }

    /** Drag in progress: update View only, avoid Compose recomposition per frame. */
    private fun onPanelDragOffset(offset: Float) {
        applyPanelTranslation(offset, syncComposeState = false)
    }

    private fun setPanelOffset(offset: Float) {
        applyPanelTranslation(offset, syncComposeState = true)
    }

    private fun applyPanelTranslation(offset: Float, syncComposeState: Boolean) {
        val clamped = offset.coerceIn(0f, panelHeightPx.toFloat())
        dragOffsetPx = clamped
        panelContainerView?.translationY = clamped
        updateDismissCaptureTouchability(clamped)
        if (syncComposeState) {
            panelOffsetY = clamped
        }
    }

    private fun updateDismissCaptureTouchability(offsetPx: Float) {
        val passThrough = offsetPx > 0f
        if (passThrough == dismissCapturePassThrough) return
        dismissCapturePassThrough = passThrough

        overlayRootView?.let { view ->
            try {
                val params = view.layoutParams as WindowManager.LayoutParams
                params.flags = if (passThrough) {
                    params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                } else if (isPanelVisible) {
                    params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                } else {
                    params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                Log.e(serviceTag, "updateDismissCaptureTouchability failed", e)
            }
        }
    }

    private fun handlePanelDragEnd(velocityY: Float) {
        panelOffsetY = dragOffsetPx
        if (PanelDragController.shouldDismiss(dragOffsetPx, panelHeightPx.toFloat(), velocityY)) {
            dismissOverlayAnimated()
        } else {
            animatePanelOffsetTo(0f)
        }
    }

    private fun animatePanelOffsetTo(target: Float, onEnd: (() -> Unit)? = null) {
        panelAnimator?.cancel()
        val start = dragOffsetPx
        if (kotlin.math.abs(start - target) < 1f) {
            setPanelOffset(target)
            onEnd?.invoke()
            return
        }
        ValueAnimator.ofFloat(start, target).apply {
            duration = PANEL_ANIM_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                setPanelOffset(animator.animatedValue as Float)
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }.also { panelAnimator = it }
    }

    private fun dismissOverlayAnimated(onEnd: (() -> Unit)? = null) {
        if (!isPanelVisible) {
            onEnd?.invoke()
            return
        }
        animatePanelOffsetTo(panelHeightPx.toFloat()) {
            hideOverlay()
            onEnd?.invoke()
        }
    }

    private fun showOverlay(): Boolean {
        if (overlayRootView == null) {
            if (!createOverlay()) return false
        }
        isPanelVisible = true
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        panelContainerView?.visibility = View.VISIBLE
        setPanelOffset(panelHeightPx.toFloat())
        animatePanelOffsetTo(0f) {
            refreshNotification()
        }
        refreshNotification()
        return true
    }

    private fun hideOverlay() {
        panelAnimator?.cancel()
        isPanelVisible = false
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        panelOffsetY = 0f
        dragOffsetPx = 0f
        dismissCapturePassThrough = false
        panelContainerView?.apply {
            translationY = 0f
            visibility = View.GONE
        }
        updateDismissCaptureTouchability(0f)
        refreshNotification()
    }

    private fun toggleOverlay(): Boolean {
        if (isPanelVisible) {
            dismissOverlayAnimated()
            return true
        }
        return showOverlay()
    }

    private fun removeOverlay() {
        panelAnimator?.cancel()
        overlayTouchListener?.destroy()
        overlayTouchListener = null

        panelContainerView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayRootView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayRootView = null
        panelContainerView = null
        isPanelVisible = false
        panelOffsetY = 0f
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
        serviceScope.cancel()
        isRunning = false
        foregroundStarted = false
        super.onDestroy()
    }
}
