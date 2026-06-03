package com.emoji.overlay.send

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class ShareTargetStore internal constructor(
    private val prefs: android.content.SharedPreferences,
) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun recordChosenPackage(packageName: String) {
        if (ShareTargetResolver.isPriorityPackage(packageName)) {
            clearCustomSlot4()
        } else {
            saveCustomSlot4(packageName)
        }
    }

    fun loadCustomSlot4Package(): String? {
        return prefs.getString(KEY_CUSTOM_SLOT4, null)?.takeIf { it.isNotBlank() }
    }

    fun clearCustomSlot4() {
        prefs.edit().remove(KEY_CUSTOM_SLOT4).apply()
    }

    private fun saveCustomSlot4(packageName: String) {
        prefs.edit().putString(KEY_CUSTOM_SLOT4, packageName).apply()
    }

    companion object {
        private const val PREFS_NAME = "share_target_store"
        private const val KEY_CUSTOM_SLOT4 = "custom_slot4_package"

        @Volatile
        private var instance: ShareTargetStore? = null

        fun getInstance(context: Context): ShareTargetStore {
            return instance ?: synchronized(this) {
                instance ?: ShareTargetStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
