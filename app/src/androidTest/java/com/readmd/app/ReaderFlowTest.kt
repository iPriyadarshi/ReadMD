package com.readmd.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Drives the reader with the bundled "Welcome to ReadMD" sample, which has known headings:
 * "Opening files", "A taste of Markdown" and others (see res/raw/welcome.md).
 */
class ReaderFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openingTheSampleShowsTheReader() {
        rule.openWelcomeSample()

        rule.onNodeWithText(WELCOME_TITLE).assertIsDisplayed()
        rule.onNodeWithContentDescription(rule.string(R.string.cd_raw_markdown)).assertIsDisplayed()
        rule.onNodeWithContentDescription(rule.string(R.string.cd_search_document)).assertIsDisplayed()
    }

    @Test
    fun theContentsListsHeadingsAndSelectingOneClosesTheDrawer() {
        rule.openWelcomeSample()

        // On wide screens the contents are a permanent pane and there is no button to press.
        val toc = rule.string(R.string.cd_toc)
        val drawerMode = rule.hasNodeWithDescription(toc)
        if (drawerMode) rule.onNodeWithContentDescription(toc).performClick()

        rule.waitFor { rule.hasTextNode(HEADING) }
        rule.onNodeWithText(HEADING).assertIsDisplayed()
        rule.onNodeWithText(HEADING).performClick()

        if (drawerMode) {
            rule.waitFor {
                runCatching { rule.onNodeWithText(HEADING).assertIsNotDisplayed() }.isSuccess
            }
        }
    }

    @Test
    fun searchingCountsTheMatches() {
        rule.openWelcomeSample()

        rule.onNodeWithContentDescription(rule.string(R.string.cd_search_document)).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Markdown")

        // The counter reads "1/N" once the rendered text has been searched.
        rule.waitFor { rule.onAllNodesWithText("1/", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("0/0").fetchSemanticsNodes().let { assertFalse(it.isNotEmpty()) }
    }

    @Test
    fun theRawToggleShowsTheMarkdownSourceAndSwitchesBack() {
        rule.openWelcomeSample()

        rule.onNodeWithContentDescription(rule.string(R.string.cd_raw_markdown)).performClick()
        rule.waitFor { rule.hasTextNode("# Welcome to ReadMD", substring = true) }
        rule.onNodeWithContentDescription(rule.string(R.string.cd_rendered_view)).assertIsDisplayed()

        rule.onNodeWithContentDescription(rule.string(R.string.cd_rendered_view)).performClick()
        rule.waitFor { rule.hasNodeWithDescription(rule.string(R.string.cd_raw_markdown)) }
        rule.onNodeWithContentDescription(rule.string(R.string.cd_raw_markdown)).assertIsDisplayed()
    }

    private companion object {
        const val HEADING = "Opening files"
    }
}
