package com.emoji.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeButtonsNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeButton_import_navigate_withoutCrash() {
        verifyNavigation("导入表情", "从相册选择")
    }

    @Test
    fun homeButton_browse_navigate_withoutCrash() {
        verifyNavigation("浏览表情", "还没有表情")
    }

    @Test
    fun homeButton_favorites_navigate_withoutCrash() {
        verifyNavigation("收藏", "还没有收藏表情")
    }

    @Test
    fun homeButton_recent_navigate_withoutCrash() {
        verifyNavigation("最近使用", "还没有使用记录")
    }

    @Test
    fun homeButton_categories_navigate_withoutCrash() {
        verifyNavigation("分类管理", "新增分类")
    }

    @Test
    fun homeButton_search_navigate_withoutCrash() {
        verifyNavigation("搜索", "输入关键词搜索表情")
    }

    @Test
    fun homeScreen_renders_overlay_section_withoutCrash() {
        composeRule.onNodeWithText("Overlay 状态").assertIsDisplayed()
    }

    private fun verifyNavigation(buttonLabel: String, markerLabel: String) {
        composeRule.onNodeWithText(buttonLabel).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(markerLabel, substring = true).assertIsDisplayed()
    }

}
