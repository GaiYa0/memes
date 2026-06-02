package com.emoji.overlay.gesture

/**
 * Decides whether a panel drag gesture should dismiss or snap back open.
 */
object PanelDragController {
    private const val DISMISS_OFFSET_FRACTION = 0.35f
    private const val DISMISS_VELOCITY_PX_PER_S = 1200f

    fun shouldDismiss(offsetY: Float, panelHeightPx: Float, velocityY: Float): Boolean {
        if (panelHeightPx <= 0f) return false
        return offsetY > panelHeightPx * DISMISS_OFFSET_FRACTION || velocityY > DISMISS_VELOCITY_PX_PER_S
    }
}
