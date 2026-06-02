package com.emoji.overlay.gesture

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

/**
 * Full-screen touch catcher: tap outside the panel triggers animated dismiss.
 * When the root window is FLAG_NOT_TOUCHABLE (panel dragged down), this listener is inactive.
 */
class OverlayTouchListener(
    private val getPanelBounds: () -> Rect,
    private val isPanelVisible: () -> Boolean,
    @Suppress("UNUSED_PARAMETER")
    private val onToggleOverlay: () -> Unit,
    private val onDismissOverlayAnimated: () -> Unit
) : View.OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (isPanelVisible() && event.actionMasked == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
            val panelBounds = getPanelBounds()
            val x = event.rawX.toInt()
            val y = event.rawY.toInt()

            if (!panelBounds.contains(x, y)) {
                onDismissOverlayAnimated()
                return true
            }
        }

        return false
    }

    fun destroy() {
        // No-op
    }
}
