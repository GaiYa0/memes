package com.emoji.overlay.gesture

import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_CANCEL
import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_DOWN
import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_MOVE
import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_POINTER_DOWN
import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_POINTER_UP
import com.emoji.overlay.gesture.TwoFingerDoubleTapDetector.Companion.ACTION_UP
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TwoFingerDoubleTapDetector].
 *
 * Uses [TwoFingerDoubleTapDetector.NoOpScheduler] to avoid Android Looper dependency.
 * Uses [TwoFingerDoubleTapDetector.processEvent] to avoid Robolectric dependency.
 *
 * Tests cover:
 * - Two-finger double-tap triggers callback
 * - Single-finger tap does NOT trigger
 * - Single-finger double-tap does NOT trigger
 * - Two-finger single-tap does NOT trigger
 * - Two-finger drag does NOT trigger
 * - Delayed second tap does NOT trigger
 */
class TwoFingerDoubleTapDetectorTest {

    private var triggered = false
    private lateinit var detector: TwoFingerDoubleTapDetector

    @Before
    fun setup() {
        triggered = false
        detector = TwoFingerDoubleTapDetector(
            onTwoFingerDoubleTap = { triggered = true },
            scheduler = TwoFingerDoubleTapDetector.NoOpScheduler()
        )
        detector.setTapSlop(50f)
    }

    @Test
    fun `two-finger double-tap triggers callback`() {
        sendTwoFingerTap(downTime = 1000, upTime = 1060)
        assertFalse("Should not trigger after first tap", triggered)

        sendTwoFingerTap(downTime = 1200, upTime = 1260)
        assertTrue("Should trigger after two-finger double-tap", triggered)
    }

    @Test
    fun `single-finger tap does not trigger`() {
        sendSingleFingerTap(downTime = 1000, upTime = 1060)
        assertFalse("Single-finger tap should not trigger", triggered)
    }

    @Test
    fun `single-finger double-tap does not trigger`() {
        sendSingleFingerTap(downTime = 1000, upTime = 1060)
        sendSingleFingerTap(downTime = 1200, upTime = 1260)
        assertFalse("Single-finger double-tap should not trigger", triggered)
    }

    @Test
    fun `two-finger single-tap does not trigger`() {
        sendTwoFingerTap(downTime = 1000, upTime = 1060)
        assertFalse("Two-finger single-tap should not trigger", triggered)
    }

    @Test
    fun `two-finger drag does not trigger`() {
        sendTwoFingerDrag(downTime = 1000, upTime = 1080)
        assertFalse("Two-finger drag should not trigger", triggered)

        sendTwoFingerTap(downTime = 1300, upTime = 1360)
        assertFalse("After drag, double-tap should not trigger", triggered)
    }

    @Test
    fun `delayed second tap does not trigger`() {
        sendTwoFingerTap(downTime = 1000, upTime = 1060)
        sendTwoFingerTap(downTime = 2000, upTime = 2060)
        assertFalse("Delayed second tap should not trigger", triggered)
    }

    @Test
    fun `three-finger tap does not trigger`() {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1000)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, 1020)
        detector.processEvent(ACTION_POINTER_DOWN, 3, 100f, 100f, 300f, 300f, 1040)
        detector.processEvent(ACTION_CANCEL, 3, 100f, 100f, 300f, 300f, 1060)
        assertFalse("Three-finger tap should not trigger", triggered)
    }

    @Test
    fun `action cancel resets state`() {
        sendTwoFingerTap(downTime = 1000, upTime = 1060)
        detector.processEvent(ACTION_CANCEL, 1, 0f, 0f, 0f, 0f, 1100)
        sendTwoFingerTap(downTime = 1200, upTime = 1260)
        // After cancel, the state should have reset, so this second tap
        // is treated as a first tap, not a second tap
        assertFalse("After cancel, should not trigger", triggered)
    }

    // --- Helper methods using processEvent ---

    private fun sendSingleFingerTap(downTime: Long, upTime: Long) {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, downTime)
        detector.processEvent(ACTION_UP, 1, 100f, 100f, 0f, 0f, upTime)
    }

    private fun sendTwoFingerTap(downTime: Long, upTime: Long) {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, downTime)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, downTime + 20)
        detector.processEvent(ACTION_POINTER_UP, 2, 100f, 100f, 200f, 200f, upTime - 10)
        detector.processEvent(ACTION_UP, 1, 100f, 100f, 0f, 0f, upTime)
    }

    private fun sendTwoFingerDrag(downTime: Long, upTime: Long) {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, downTime)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, downTime + 20)
        detector.processEvent(ACTION_MOVE, 2, 300f, 300f, 400f, 400f, downTime + 40)
        detector.processEvent(ACTION_POINTER_UP, 2, 300f, 300f, 400f, 400f, upTime - 10)
        detector.processEvent(ACTION_UP, 1, 300f, 300f, 0f, 0f, upTime)
    }
}
