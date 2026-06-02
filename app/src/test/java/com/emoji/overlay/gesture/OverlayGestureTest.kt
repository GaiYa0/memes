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
 * Comprehensive gesture tests for the overlay system.
 *
 * Tests cover:
 * - Two-finger double-tap recognition
 * - Single-finger tap rejection
 * - Two-finger drag rejection
 * - Delayed tap rejection
 * - Multiple double-taps in sequence
 * - Different finger-lift orders
 * - Edge cases
 */
class OverlayGestureTest {

    private var triggerCount = 0
    private lateinit var detector: TwoFingerDoubleTapDetector

    @Before
    fun setup() {
        triggerCount = 0
        detector = TwoFingerDoubleTapDetector(
            onTwoFingerDoubleTap = { triggerCount++ },
            scheduler = TwoFingerDoubleTapDetector.NoOpScheduler()
        )
        detector.setTapSlop(50f)
    }

    // ==================== BASIC TRIGGER TESTS ====================

    @Test
    fun `two-finger double-tap triggers once`() {
        sendTwoFingerTap(1000, 1060)
        sendTwoFingerTap(1200, 1260)
        assertEquals("Should trigger exactly once", 1, triggerCount)
    }

    @Test
    fun `two consecutive double-taps trigger twice`() {
        sendTwoFingerTap(1000, 1060)
        sendTwoFingerTap(1200, 1260)
        assertEquals("First double-tap should trigger", 1, triggerCount)

        sendTwoFingerTap(1500, 1560)
        sendTwoFingerTap(1700, 1760)
        assertEquals("Second double-tap should trigger", 2, triggerCount)
    }

    @Test
    fun `double-tap with reverse finger lift order works`() {
        // Finger 2 lifts before finger 1 (ACTION_POINTER_UP first, then ACTION_UP)
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1000)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, 1020)
        // Finger 1 lifts first (ACTION_UP with pointerCount=1)
        detector.processEvent(ACTION_UP, 1, 200f, 200f, 0f, 0f, 1050)
        assertEquals("First tap should not trigger", 0, triggerCount)

        // Second tap
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1200)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, 1220)
        detector.processEvent(ACTION_UP, 1, 200f, 200f, 0f, 0f, 1250)
        assertEquals("Double-tap should trigger", 1, triggerCount)
    }

    // ==================== REJECTION TESTS ====================

    @Test
    fun `single-finger tap never triggers`() {
        repeat(5) { i ->
            sendSingleFingerTap(1000L + i * 200, 1060L + i * 200)
        }
        assertEquals("Single-finger taps should never trigger", 0, triggerCount)
    }

    @Test
    fun `two-finger single-tap does not trigger`() {
        sendTwoFingerTap(1000, 1060)
        assertEquals("Single two-finger tap should not trigger", 0, triggerCount)
    }

    @Test
    fun `two-finger drag does not trigger`() {
        sendTwoFingerDrag(1000, 1080)
        assertEquals("Drag should not trigger", 0, triggerCount)
    }

    @Test
    fun `delayed second tap does not trigger`() {
        sendTwoFingerTap(1000, 1060)
        // Second tap 1000ms later (> 500ms timeout)
        sendTwoFingerTap(2000, 2060)
        assertEquals("Delayed second tap should not trigger", 0, triggerCount)
    }

    @Test
    fun `three-finger tap does not trigger`() {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1000)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, 1020)
        detector.processEvent(ACTION_POINTER_DOWN, 3, 100f, 100f, 300f, 300f, 1040)
        detector.processEvent(ACTION_CANCEL, 3, 100f, 100f, 300f, 300f, 1060)
        assertEquals("Three-finger tap should not trigger", 0, triggerCount)
    }

    // ==================== STATE RESET TESTS ====================

    @Test
    fun `action cancel resets state`() {
        sendTwoFingerTap(1000, 1060)
        detector.processEvent(ACTION_CANCEL, 1, 0f, 0f, 0f, 0f, 1100)
        sendTwoFingerTap(1200, 1260)
        assertEquals("After cancel, should not trigger", 0, triggerCount)
    }

    @Test
    fun `single finger down after first tap resets`() {
        sendTwoFingerTap(1000, 1060)
        // Single finger down (not a valid second tap start)
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1200)
        detector.processEvent(ACTION_UP, 1, 100f, 100f, 0f, 0f, 1260)
        assertEquals("Should not trigger after single finger", 0, triggerCount)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `boundary timing - just within timeout`() {
        sendTwoFingerTap(1000, 1060)
        // Second tap at 490ms after first tap up (1060 + 490 = 1550 < 500ms timeout)
        sendTwoFingerTap(1550, 1610)
        assertEquals("Should trigger within timeout", 1, triggerCount)
    }

    @Test
    fun `boundary timing - just outside timeout`() {
        sendTwoFingerTap(1000, 1060)
        // Second tap at 510ms after first tap up (1060 + 510 = 1570 > 500ms timeout)
        sendTwoFingerTap(1570, 1630)
        assertEquals("Should not trigger outside timeout", 0, triggerCount)
    }

    @Test
    fun `rapid taps after failed attempt`() {
        // First attempt fails (delayed)
        sendTwoFingerTap(1000, 1060)
        sendTwoFingerTap(2000, 2060)
        assertEquals(0, triggerCount)

        // Second attempt succeeds
        sendTwoFingerTap(2200, 2260)
        sendTwoFingerTap(2400, 2460)
        assertEquals("Should trigger on retry", 1, triggerCount)
    }

    @Test
    fun `pointer count 3 then cancel does not corrupt state`() {
        detector.processEvent(ACTION_DOWN, 1, 100f, 100f, 0f, 0f, 1000)
        detector.processEvent(ACTION_POINTER_DOWN, 2, 100f, 100f, 200f, 200f, 1020)
        detector.processEvent(ACTION_POINTER_DOWN, 3, 100f, 100f, 300f, 300f, 1040)
        detector.processEvent(ACTION_CANCEL, 3, 100f, 100f, 300f, 300f, 1060)

        // Should be able to do a valid double-tap after cancel
        sendTwoFingerTap(1200, 1260)
        sendTwoFingerTap(1400, 1460)
        assertEquals("Should work after cancel", 1, triggerCount)
    }

    // ==================== HELPER METHODS ====================

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
