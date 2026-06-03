package com.emoji.overlay.send

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareIntentHelperTest {

    @Test
    fun `resolveShareComponent prefers QQ JumpActivity over main activity`() {
        val candidates = listOf(
            ShareIntentHelper.ShareActivityCandidate(
                "com.tencent.mobileqq",
                "com.tencent.mobileqq.activity.SplashActivity"
            ),
            ShareIntentHelper.ShareActivityCandidate(
                "com.tencent.mobileqq",
                "com.tencent.mobileqq.activity.JumpActivity"
            ),
        )

        val component = ShareIntentHelper.resolveShareComponent("com.tencent.mobileqq", candidates)

        assertEquals("com.tencent.mobileqq.activity.JumpActivity", component?.className)
    }

    @Test
    fun `resolveShareComponent prefers WeChat ShareImgUI`() {
        val candidates = listOf(
            ShareIntentHelper.ShareActivityCandidate(
                "com.tencent.mm",
                "com.tencent.mm.ui.LauncherUI"
            ),
            ShareIntentHelper.ShareActivityCandidate(
                "com.tencent.mm",
                "com.tencent.mm.ui.tools.ShareImgUI"
            ),
        )

        val component = ShareIntentHelper.resolveShareComponent("com.tencent.mm", candidates)

        assertEquals("com.tencent.mm.ui.tools.ShareImgUI", component?.className)
    }

    @Test
    fun `resolveShareComponent prefers share like activity for douyin`() {
        val candidates = listOf(
            ShareIntentHelper.ShareActivityCandidate(
                "com.ss.android.ugc.aweme",
                "com.ss.android.ugc.aweme.main.MainActivity"
            ),
            ShareIntentHelper.ShareActivityCandidate(
                "com.ss.android.ugc.aweme",
                "com.ss.android.ugc.aweme.share.ShareActivity"
            ),
        )

        val component = ShareIntentHelper.resolveShareComponent("com.ss.android.ugc.aweme", candidates)

        assertEquals("com.ss.android.ugc.aweme.share.ShareActivity", component?.className)
    }

    @Test
    fun `resolveShareComponent prefers share like activity for telegram`() {
        val candidates = listOf(
            ShareIntentHelper.ShareActivityCandidate(
                "org.telegram.messenger",
                "org.telegram.ui.LaunchActivity"
            ),
            ShareIntentHelper.ShareActivityCandidate(
                "org.telegram.messenger",
                "org.telegram.ui.Components.ShareActivity"
            ),
        )

        val component = ShareIntentHelper.resolveShareComponent("org.telegram.messenger", candidates)

        assertEquals("org.telegram.ui.Components.ShareActivity", component?.className)
    }

    @Test
    fun `resolveShareComponent skips launcher when only generic handler exists`() {
        val candidates = listOf(
            ShareIntentHelper.ShareActivityCandidate(
                "com.example.app",
                "com.example.app.MainActivity"
            ),
            ShareIntentHelper.ShareActivityCandidate(
                "com.example.app",
                "com.example.app.SendImageActivity"
            ),
        )

        val component = ShareIntentHelper.resolveShareComponent("com.example.app", candidates)

        assertEquals("com.example.app.SendImageActivity", component?.className)
    }

    @Test
    fun `resolveShareComponent returns null when no candidates`() {
        assertNull(
            ShareIntentHelper.resolveShareComponent(
                "com.tencent.mobileqq",
                emptyList()
            )
        )
    }

    @Test
    fun `mimeTypesForQuery falls back to image wildcard`() {
        assertEquals(
            listOf("image/png", "image/*"),
            ShareIntentHelper.mimeTypesForQuery("image/png")
        )
    }

    @Test
    fun `mimeTypesForQuery keeps wildcard only`() {
        assertEquals(
            listOf("image/*"),
            ShareIntentHelper.mimeTypesForQuery("image/*")
        )
    }
}
