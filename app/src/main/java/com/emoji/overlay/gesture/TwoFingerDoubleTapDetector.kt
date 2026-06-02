package com.emoji.overlay.gesture

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent

/**
 * Detects a two-finger double-tap gesture.
 *
 * A two-finger double-tap is defined as:
 * 1. Two fingers touch down simultaneously (within a tolerance window)
 * 2. Both fingers lift up (tap complete)
 * 3. Within [DOUBLE_TAP_TIMEOUT_MS], two fingers touch down again
 * 4. Both fingers lift up again
 *
 * Single-finger taps, drags, and other multi-touch gestures are ignored.
 */
class TwoFingerDoubleTapDetector(
    private val onTwoFingerDoubleTap: () -> Unit,
    private val scheduler: Scheduler = AndroidScheduler()
) {
    companion object {
        // Timing constants
        const val DOUBLE_TAP_TIMEOUT_MS = 300L
        const val SIMULTANEOUS_TOUCH_TOLERANCE_MS = 100L
        const val MAX_TAP_DURATION_MS = 300L

        // MotionEvent action constants
        const val ACTION_DOWN = 0
        const val ACTION_UP = 1
        const val ACTION_MOVE = 2
        const val ACTION_CANCEL = 3
        const val ACTION_POINTER_DOWN = 5
        const val ACTION_POINTER_UP = 6
    }

    /**
     * Abstraction for delayed task scheduling.
     * Production uses Android Handler; tests can use a no-op implementation.
     */
    interface Scheduler {
        fun postDelayed(delayMs: Long, action: () -> Unit)
        fun cancelAll()
    }

    /**
     * Production scheduler backed by Android Handler.
     */
    class AndroidScheduler : Scheduler {
        private val handler = Handler(Looper.getMainLooper())
        private var pendingAction: Runnable? = null

        override fun postDelayed(delayMs: Long, action: () -> Unit) {
            pendingAction?.let { handler.removeCallbacks(it) }
            val runnable = Runnable { action() }
            pendingAction = runnable
            handler.postDelayed(runnable, delayMs)
        }

        override fun cancelAll() {
            pendingAction?.let { handler.removeCallbacks(it) }
            pendingAction = null
        }
    }

    /**
     * No-op scheduler for testing — timeouts are not tested via real delays.
     */
    class NoOpScheduler : Scheduler {
        override fun postDelayed(delayMs: Long, action: () -> Unit) { /* no-op */ }
        override fun cancelAll() { /* no-op */ }
    }

    private enum class State {
        IDLE,
        FIRST_TAP_DOWN,
        FIRST_TAP_UP,
        SECOND_TAP_DOWN
    }

    private var state = State.IDLE

    private var pointer0DownX = 0f
    private var pointer0DownY = 0f
    private var pointer1DownX = 0f
    private var pointer1DownY = 0f
    private var firstPointerDownTime = 0L
    private var secondPointerDownTime = 0L
    private var tapSlop = 20f

    /**
     * Process a touch event from the system.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        processEvent(
            actionMasked = event.actionMasked,
            pointerCount = event.pointerCount,
            x0 = if (event.pointerCount > 0) event.getX(0) else 0f,
            y0 = if (event.pointerCount > 0) event.getY(0) else 0f,
            x1 = if (event.pointerCount > 1) event.getX(1) else 0f,
            y1 = if (event.pointerCount > 1) event.getY(1) else 0f,
            eventTime = event.eventTime
        )
        return true
    }

    /**
     * Core event processing with primitive parameters.
     * Visible for testing — allows tests without Robolectric.
     */
    internal fun processEvent(
        actionMasked: Int,
        pointerCount: Int,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        eventTime: Long
    ) {
        when (actionMasked) {
            ACTION_DOWN -> handleActionDown(x0, y0, eventTime)
            ACTION_POINTER_DOWN -> handlePointerDown(pointerCount, x1, y1, eventTime)
            ACTION_UP -> handleActionUp()
            ACTION_POINTER_UP -> handlePointerUp(pointerCount, eventTime)
            ACTION_MOVE -> handleActionMove(pointerCount, x0, y0, x1, y1)
            ACTION_CANCEL -> reset()
        }
    }

    fun setTapSlop(tapSlopPx: Float) {
        tapSlop = tapSlopPx
    }

    private fun handleActionDown(x: Float, y: Float, eventTime: Long) {
        when (state) {
            State.IDLE -> {
                state = State.FIRST_TAP_DOWN
                pointer0DownX = x
                pointer0DownY = y
                firstPointerDownTime = eventTime
                scheduler.cancelAll()
                scheduler.postDelayed(DOUBLE_TAP_TIMEOUT_MS + MAX_TAP_DURATION_MS) { reset() }
            }
            State.FIRST_TAP_UP -> {
                state = State.SECOND_TAP_DOWN
                scheduler.cancelAll()
                scheduler.postDelayed(SIMULTANEOUS_TOUCH_TOLERANCE_MS + MAX_TAP_DURATION_MS) { reset() }
            }
            else -> reset()
        }
    }

    private fun handlePointerDown(
        pointerCount: Int,
        x: Float,
        y: Float,
        eventTime: Long
    ) {
        when (state) {
            State.FIRST_TAP_DOWN -> {
                if (pointerCount == 2) {
                    val timeDiff = eventTime - firstPointerDownTime
                    if (timeDiff <= SIMULTANEOUS_TOUCH_TOLERANCE_MS) {
                        pointer1DownX = x
                        pointer1DownY = y
                        secondPointerDownTime = eventTime
                    } else {
                        reset()
                    }
                } else {
                    reset()
                }
            }
            State.SECOND_TAP_DOWN -> {
                if (pointerCount != 2) reset()
            }
            else -> reset()
        }
    }

    private fun handleActionUp() {
        when (state) {
            State.FIRST_TAP_DOWN -> reset()
            // FIRST_TAP_UP: valid first tap completed, last finger lifting — stay in this state
            State.FIRST_TAP_UP -> { /* keep waiting for second tap */ }
            State.SECOND_TAP_DOWN -> reset()
            State.IDLE -> { /* ignore */ }
        }
    }

    private fun handlePointerUp(pointerCount: Int, eventTime: Long) {
        when (state) {
            State.FIRST_TAP_DOWN -> {
                if (pointerCount == 2) {
                    val duration = eventTime - firstPointerDownTime
                    if (duration <= MAX_TAP_DURATION_MS) {
                        state = State.FIRST_TAP_UP
                        scheduler.cancelAll()
                        scheduler.postDelayed(DOUBLE_TAP_TIMEOUT_MS) { reset() }
                    } else {
                        reset()
                    }
                }
            }
            State.SECOND_TAP_DOWN -> {
                if (pointerCount == 2) {
                    val duration = eventTime - secondPointerDownTime
                    if (duration <= MAX_TAP_DURATION_MS) {
                        scheduler.cancelAll()
                        state = State.IDLE
                        onTwoFingerDoubleTap()
                    } else {
                        reset()
                    }
                }
            }
            else -> { /* ignore */ }
        }
    }

    private fun handleActionMove(
        pointerCount: Int,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float
    ) {
        if (state == State.FIRST_TAP_DOWN && pointerCount >= 2) {
            val dx0 = x0 - pointer0DownX
            val dy0 = y0 - pointer0DownY
            val dx1 = x1 - pointer1DownX
            val dy1 = y1 - pointer1DownY
            if (dx0 * dx0 + dy0 * dy0 > tapSlop * tapSlop ||
                dx1 * dx1 + dy1 * dy1 > tapSlop * tapSlop
            ) {
                reset()
            }
        }
    }

    private fun reset() {
        state = State.IDLE
        scheduler.cancelAll()
    }

    fun destroy() {
        scheduler.cancelAll()
    }
}
