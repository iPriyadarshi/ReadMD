package com.readmd.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeOffersToOpenAFileOrAFolder() {
        rule.onNodeWithText(rule.string(R.string.open_file)).assertIsDisplayed()
        rule.onNodeWithText(rule.string(R.string.open_folder)).assertIsDisplayed()
    }

    @Test
    fun homeListsTheBundledSamples() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(rule.string(R.string.sample_welcome)))
        rule.onNodeWithText(rule.string(R.string.sample_welcome)).assertIsDisplayed()

        rule.onNode(hasScrollAction()).performScrollToNode(hasText(rule.string(R.string.sample_cheatsheet)))
        rule.onNodeWithText(rule.string(R.string.sample_cheatsheet)).assertIsDisplayed()
    }
}
