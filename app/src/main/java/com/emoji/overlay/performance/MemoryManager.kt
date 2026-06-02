package com.emoji.overlay.performance

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.util.Log

/**
 * Memory manager for handling memory pressure and preventing OOM.
 *
 * Responds to system memory pressure by:
 * - Clearing caches
 * - Reducing memory usage
 * - Monitoring memory levels
 */
class MemoryManager(private val context: Context) : ComponentCallbacks2 {
    companion object {
        private const val TAG = "MemoryManager"
        private const val LOW_MEMORY_THRESHOLD_MB = 50
        private const val CRITICAL_MEMORY_THRESHOLD_MB = 20
    }

    private val listeners = mutableListOf<MemoryPressureListener>()
    private var lastMemoryLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE

    fun register() {
        context.registerComponentCallbacks(this)
    }

    fun unregister() {
        context.unregisterComponentCallbacks(this)
    }

    fun addListener(listener: MemoryPressureListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MemoryPressureListener) {
        listeners.remove(listener)
    }

    override fun onTrimMemory(level: Int) {
        lastMemoryLevel = level
        Log.d(TAG, "Memory pressure level: $level")

        when {
            level >= 80 -> { // TRIM_MEMORY_COMPLETE
                Log.w(TAG, "Critical memory pressure - clearing all caches")
                listeners.forEach { it.onCriticalMemoryPressure() }
            }
            level >= 60 -> { // TRIM_MEMORY_RUNNING_LOW
                Log.w(TAG, "Low memory pressure - reducing cache size")
                listeners.forEach { it.onLowMemoryPressure() }
            }
            level >= 40 -> { // TRIM_MEMORY_RUNNING_MODERATE
                Log.d(TAG, "Moderate memory pressure - trimming caches")
                listeners.forEach { it.onModerateMemoryPressure() }
            }
        }
    }

    override fun onLowMemory() {
        Log.w(TAG, "System low memory callback")
        listeners.forEach { it.onCriticalMemoryPressure() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op
    }

    fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return MemoryInfo(
            totalMemMB = memInfo.totalMem / (1024 * 1024),
            availMemMB = memInfo.availMem / (1024 * 1024),
            usedMemMB = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024),
            isLowMemory = memInfo.lowMemory,
            thresholdMB = memInfo.threshold / (1024 * 1024)
        )
    }

    fun isLowMemory(): Boolean {
        val info = getMemoryInfo()
        return info.isLowMemory || info.availMemMB < LOW_MEMORY_THRESHOLD_MB
    }

    fun isCriticalMemory(): Boolean {
        val info = getMemoryInfo()
        return info.availMemMB < CRITICAL_MEMORY_THRESHOLD_MB
    }
}

data class MemoryInfo(
    val totalMemMB: Long,
    val availMemMB: Long,
    val usedMemMB: Long,
    val isLowMemory: Boolean,
    val thresholdMB: Long
)

interface MemoryPressureListener {
    fun onModerateMemoryPressure() {}
    fun onLowMemoryPressure() {}
    fun onCriticalMemoryPressure() {}
}
