package com.emoji.overlay.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapSamplingTest {

    @Test
    fun `calculateInSampleSize returns 1 for small images`() {
        assertEquals(1, calculateInSampleSize(320, 240, targetMaxPx = 320))
    }

    @Test
    fun `calculateInSampleSize downsamples large images`() {
        assertEquals(8, calculateInSampleSize(4000, 3000, targetMaxPx = 320))
    }

    @Test
    fun `calculateInSampleSize handles invalid dimensions`() {
        assertEquals(1, calculateInSampleSize(0, 100, targetMaxPx = 320))
        assertEquals(1, calculateInSampleSize(100, -1, targetMaxPx = 320))
    }

    @Test
    fun `calculateInSampleSize uses power of two`() {
        val sample = calculateInSampleSize(4000, 3000, targetMaxPx = 320)
        assertEquals(true, sample and (sample - 1) == 0)
    }
}
