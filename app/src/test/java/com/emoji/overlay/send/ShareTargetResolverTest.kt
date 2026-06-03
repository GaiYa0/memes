package com.emoji.overlay.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareTargetResolverTest {

    @Test
    fun `resolve installed targets preserves priority order`() {
        val installed = setOf(
            "org.telegram.messenger",
            "com.tencent.mm",
            "com.tencent.mobileqq",
            "com.ss.android.ugc.aweme"
        )

        val resolved = ShareTargetResolver.resolveInstalledTargets(installed)

        assertEquals(listOf("QQ", "微信", "抖音", "Telegram"), resolved.map { it.label })
        assertEquals("com.tencent.mobileqq", resolved[0].packageName)
        assertEquals("com.tencent.mm", resolved[1].packageName)
        assertEquals("com.ss.android.ugc.aweme", resolved[2].packageName)
        assertEquals("org.telegram.messenger", resolved[3].packageName)
    }

    @Test
    fun `resolve skips missing apps`() {
        val installed = setOf("com.tencent.mobileqq", "com.tencent.mm")
        val resolved = ShareTargetResolver.resolveInstalledTargets(installed)
        assertEquals(listOf("QQ", "微信"), resolved.map { it.label })
    }

    @Test
    fun `resolve douyin lite fallback`() {
        val installed = setOf("com.ss.android.ugc.aweme.lite")
        val resolved = ShareTargetResolver.resolveInstalledTargets(installed)
        assertEquals(1, resolved.size)
        assertEquals("抖音", resolved[0].label)
        assertEquals("com.ss.android.ugc.aweme.lite", resolved[0].packageName)
    }

    @Test
    fun `resolve telegram web fallback`() {
        val installed = setOf("org.telegram.messenger.web")
        val resolved = ShareTargetResolver.resolveInstalledTargets(installed)
        assertEquals("Telegram", resolved.single().label)
        assertEquals("org.telegram.messenger.web", resolved.single().packageName)
    }

    @Test
    fun `resolve first available package`() {
        val pkg = ShareTargetResolver.resolveFirstAvailablePackage(
            packageNames = listOf("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite"),
            installedPackages = setOf("com.ss.android.ugc.aweme.lite")
        )
        assertEquals("com.ss.android.ugc.aweme.lite", pkg)
    }

    @Test
    fun `resolve first available package returns null when none installed`() {
        val pkg = ShareTargetResolver.resolveFirstAvailablePackage(
            packageNames = listOf("com.tencent.mm"),
            installedPackages = emptySet()
        )
        assertNull(pkg)
    }

    @Test
    fun `resolveDisplayTargets preserves four slot order`() {
        val handlers = setOf(
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.ss.android.ugc.aweme",
            "org.telegram.messenger"
        )
        val resolved = ShareTargetResolver.resolveDisplayTargets(handlers, customSlot4Package = null)
        assertEquals(listOf("QQ", "微信", "抖音", "Telegram"), resolved.map { it.label })
    }

    @Test
    fun `resolveDisplayTargets uses custom slot4 instead of telegram`() {
        val handlers = setOf(
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.ss.android.ugc.aweme",
            "org.telegram.messenger",
            "com.example.kook"
        )
        val resolved = ShareTargetResolver.resolveDisplayTargets(
            shareHandlers = handlers,
            customSlot4Package = "com.example.kook",
            labelResolver = { "KOOK" }
        )
        assertEquals(listOf("QQ", "微信", "抖音", "KOOK"), resolved.map { it.label })
        assertEquals("com.example.kook", resolved[3].packageName)
    }

    @Test
    fun `resolveDisplayTargets shows only custom slot4 when priority missing`() {
        val handlers = setOf("com.example.kook")
        val resolved = ShareTargetResolver.resolveDisplayTargets(
            shareHandlers = handlers,
            customSlot4Package = "com.example.kook",
            labelResolver = { "KOOK" }
        )
        assertEquals(1, resolved.size)
        assertEquals("KOOK", resolved.single().label)
    }

    @Test
    fun `resolveDisplayTargets uses custom slot4 when not in shareHandlers but available`() {
        val handlers = setOf(
            "com.tencent.mobileqq",
            "com.tencent.mm",
            "com.ss.android.ugc.aweme",
            "org.telegram.messenger"
        )
        val resolved = ShareTargetResolver.resolveDisplayTargets(
            shareHandlers = handlers,
            customSlot4Package = "com.example.kook",
            labelResolver = { "KOOK" },
            packageAvailable = { it == "com.example.kook" }
        )
        assertEquals(listOf("QQ", "微信", "抖音", "KOOK"), resolved.map { it.label })
        assertEquals("com.example.kook", resolved[3].packageName)
    }

    @Test
    fun `isPriorityPackage recognizes priority aliases`() {
        assertEquals(true, ShareTargetResolver.isPriorityPackage("com.tencent.mm"))
        assertEquals(false, ShareTargetResolver.isPriorityPackage("com.example.kook"))
    }
}
