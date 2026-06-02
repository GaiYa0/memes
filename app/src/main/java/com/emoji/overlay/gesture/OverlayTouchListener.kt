package com.emoji.overlay.gesture

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * Attach this to the root overlay view to:
 * 1. Detect two-finger double-tap anywhere → toggle overlay
 * 2. Detect tap on empty area (outside emoji panel) → dismiss overlay
 *
 * @param panelView the emoji panel view; taps on it are consumed normally
 * @param onToggleOverlay called when two-finger double-tap is detected
 * @param onDismissOverlay called when a tap lands outside the panel
 */
class OverlayTouchListener(
    private val panelView: View,
    private val onToggleOverlay: () -> Unit,
    private val onDismissOverlay: () -> Unit
) : View.OnTouchListener {

    private val gestureDetector = TwoFingerDoubleTapDetector(onToggleOverlay)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // Feed every event to the two-finger double-tap detector
        gestureDetector.onTouchEvent(event)

        // For single-finger taps that land outside the panel, dismiss
        if (event.actionMasked == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
            val location = IntArray(2)
            panelView.getLocationOnScreen(location)
            val panelLeft = location[0]
            val panelTop = location[1]
            val panelRight = panelLeft + panelView.width
            val panelBottom = panelTop + panelView.height

            val rawX = event.rawX
            val rawY = event.rawY

            if (rawX < panelLeft || rawX > panelRight ||
                rawY < panelTop || rawY > panelBottom
            ) {
                // Tap is outside the panel → dismiss
                onDismissOverlay()
                return true
            }
        }

        // Let touches on the panel pass through for normal interaction
        return false
    }

    fun destroy() {
        gestureDetector.destroy()
    }
}
