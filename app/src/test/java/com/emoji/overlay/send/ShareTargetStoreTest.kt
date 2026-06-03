package com.emoji.overlay.send

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ShareTargetStoreTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var store: ShareTargetStore

    @Before
    fun setUp() {
        prefs = InMemorySharedPreferences()
        store = ShareTargetStore(prefs)
    }

    @Test
    fun `recordChosenPackage saves custom app to slot4`() {
        store.recordChosenPackage("com.example.kook")
        assertEquals("com.example.kook", store.loadCustomSlot4Package())
    }

    @Test
    fun `recordChosenPackage clears custom slot4 for priority app`() {
        store.recordChosenPackage("com.example.kook")
        store.recordChosenPackage("com.tencent.mm")
        assertNull(store.loadCustomSlot4Package())
    }

    @Test
    fun `recordChosenPackage overwrites custom slot4`() {
        store.recordChosenPackage("com.example.kook")
        store.recordChosenPackage("com.example.other")
        assertEquals("com.example.other", store.loadCustomSlot4Package())
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = values.toMap()
        override fun getString(key: String, defValue: String?): String? =
            values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (values[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(values)
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
        ) = Unit

        private class Editor(
            private val values: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                pending[key] = value
                return this
            }

            override fun putStringSet(
                key: String,
                values: MutableSet<String>?
            ): SharedPreferences.Editor {
                pending[key] = values
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                pending[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                pending[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                pending[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                pending[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                removals.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) {
                    values.clear()
                }
                removals.forEach { values.remove(it) }
                values.putAll(pending)
                pending.clear()
                removals.clear()
                clearAll = false
            }
        }
    }
}
