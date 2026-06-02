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
        // Timing constants — relaxed for real-world finger behavior
        const val DOUBLE_TAP_TIMEOUT_MS = 500L
        const val SIMULTANEOUS_TOUCH_TOLERANCE_MS = 200L
        const val MAX_TAP_DURATION_MS = 500L

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
        var pendingAction: Runnable? = null

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

    internal enum class State {
        IDLE,
        FIRST_TAP_DOWN,
        FIRST_TAP_UP,
        SECOND_TAP_DOWN
    }

    internal var state = State.IDLE

    private var pointer0DownX = 0f
    private var pointer0DownY = 0f
    private var pointer1DownX = 0f
    private var pointer1DownY = 0f
    private var firstPointerDownTime = 0L
    private var secondPointerDownTime = 0L
    private var firstTapUpTime = 0L
    private var tapSlop = 30f

    // Track whether we have two pointers for the current tap
    private var hasTwoPointers = false

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
            ACTION_UP -> handleActionUp(pointerCount, eventTime)
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
                // Start of first tap
                state = State.FIRST_TAP_DOWN
                pointer0DownX = x
                pointer0DownY = y
                firstPointerDownTime = eventTime
                hasTwoPointers = false
                scheduler.cancelAll()
                scheduler.postDelayed(DOUBLE_TAP_TIMEOUT_MS + MAX_TAP_DURATION_MS) { reset() }
            }
            State.FIRST_TAP_UP -> {
                // Start of second tap — check time since first tap completed
                val timeSinceFirstTap = eventTime - firstTapUpTime
                if (timeSinceFirstTap > DOUBLE_TAP_TIMEOUT_MS) {
                    // Too late — treat as new first tap
                    state = State.FIRST_TAP_DOWN
                    pointer0DownX = x
                    pointer0DownY = y
                    firstPointerDownTime = eventTime
                    hasTwoPointers = false
                    scheduler.cancelAll()
                    scheduler.postDelayed(DOUBLE_TAP_TIMEOUT_MS + MAX_TAP_DURATION_MS) { reset() }
                } else {
                    state = State.SECOND_TAP_DOWN
                    pointer0DownX = x
                    pointer0DownY = y
                    secondPointerDownTime = eventTime
                    hasTwoPointers = false
                    scheduler.cancelAll()
                    scheduler.postDelayed(SIMULTANEOUS_TOUCH_TOLERANCE_MS + MAX_TAP_DURATION_MS) { reset() }
                }
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
                        hasTwoPointers = true
                    } else {
                        reset()
                    }
                } else {
                    reset()
                }
            }
            State.SECOND_TAP_DOWN -> {
                if (pointerCount == 2) {
                    val timeDiff = eventTime - secondPointerDownTime
                    if (timeDiff <= SIMULTANEOUS_TOUCH_TOLERANCE_MS) {
                        pointer1DownX = x
                        pointer1DownY = y
                        hasTwoPointers = true
                    } else {
                        reset()
                    }
                } else {
                    reset()
                }
            }
            else -> reset()
        }
    }

    /**
     * Handle ACTION_UP. Check pointerCount to distinguish between:
     * - Last finger lifting (pointerCount == 1 at ACTION_UP) → tap complete
     * - One of two fingers lifting while other stays → not yet complete
     */
    private fun handleActionUp(pointerCount: Int, eventTime: Long) {
        when (state) {
            State.FIRST_TAP_DOWN -> {
                if (hasTwoPointers && pointerCount == 1) {
                    // Both fingers were down, now the last one lifts → first tap complete
                    val duration = eventTime - firstPointerDownTime
                    if (duration <= MAX_TAP_DURATION_MS) {
                        state = State.FIRST_TAP_UP
                        firstTapUpTime = eventTime
                        scheduler.cancelAll()
                        scheduler.postDelayed(DOUBLE_TAP_TIMEOUT_MS) { reset() }
                    } else {
                        reset()
                    }
                } else if (!hasTwoPointers && pointerCount == 1) {
                    // Single finger tap only — not a two-finger tap
                    reset()
                }
                // If pointerCount > 1, one finger lifted but another stays — wait
            }
            State.FIRST_TAP_UP -> {
                // Already completed first tap, just waiting — ignore extra ups
            }
            State.SECOND_TAP_DOWN -> {
                if (hasTwoPointers && pointerCount == 1) {
                    // Both fingers were down, now the last one lifts → second tap complete!
                    val duration = eventTime - secondPointerDownTime
                    if (duration <= MAX_TAP_DURATION_MS) {
                        scheduler.cancelAll()
                        state = State.IDLE
                        onTwoFingerDoubleTap()
                    } else {
                        reset()
                    }
                } else if (!hasTwoPointers && pointerCount == 1) {
                    // Single finger only — not a valid two-finger tap
                    reset()
                }
                // If pointerCount > 1, one finger lifted but another stays — wait
            }
            State.IDLE -> { /* ignore */ }
        }
    }

    private fun handlePointerUp(pointerCount: Int, eventTime: Long) {
        when (state) {
            State.FIRST_TAP_DOWN -> {
                if (hasTwoPointers && pointerCount == 2) {
                    // One finger of a two-finger tap lifts first
                    // Don't complete yet — wait for ACTION_UP with pointerCount==1
                    // But also validate duration
                    val duration = eventTime - firstPointerDownTime
                    if (duration > MAX_TAP_DURATION_MS) {
                        reset()
                    }
                }
            }
            State.SECOND_TAP_DOWN -> {
                if (hasTwoPointers && pointerCount == 2) {
                    // One finger of the second tap lifts first
                    // Don't complete yet — wait for ACTION_UP with pointerCount==1
                    val duration = eventTime - secondPointerDownTime
                    if (duration > MAX_TAP_DURATION_MS) {
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
        if (!hasTwoPointers) return
        if (pointerCount < 2) return

        val referenceX0: Float
        val referenceY0: Float
        val referenceX1: Float
        val referenceY1: Float

        when (state) {
            State.FIRST_TAP_DOWN -> {
                referenceX0 = pointer0DownX
                referenceY0 = pointer0DownY
                referenceX1 = pointer1DownX
                referenceY1 = pointer1DownY
            }
            State.SECOND_TAP_DOWN -> {
                referenceX0 = pointer0DownX
                referenceY0 = pointer0DownY
                referenceX1 = pointer1DownX
                referenceY1 = pointer1DownY
            }
            else -> return
        }

        val dx0 = x0 - referenceX0
        val dy0 = y0 - referenceY0
        val dx1 = x1 - referenceX1
        val dy1 = y1 - referenceY1
        if (dx0 * dx0 + dy0 * dy0 > tapSlop * tapSlop ||
            dx1 * dx1 + dy1 * dy1 > tapSlop * tapSlop
        ) {
            reset()
        }
    }

    private fun reset() {
        state = State.IDLE
        hasTwoPointers = false
        scheduler.cancelAll()
    }

    fun destroy() {
        scheduler.cancelAll()
    }
}
