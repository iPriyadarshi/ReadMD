package com.readmd.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.rules.ActivityScenarioRule

typealias ReadMdRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

private const val WAIT_MS = 10_000L

/** A string from the app's resources, so the tests work in whichever language the device uses. */
fun ReadMdRule.string(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)

/** Waits until something is on screen; the reader loads and renders asynchronously. */
fun ReadMdRule.waitFor(condition: () -> Boolean) {
    waitUntil(WAIT_MS, condition)
}

fun SemanticsNodeInteractionsProvider.hasNodeWithDescription(description: String): Boolean =
    onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

fun SemanticsNodeInteractionsProvider.hasTextNode(text: String, substring: Boolean = false): Boolean =
    onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

/**
 * Opens the bundled "Welcome to ReadMD" sample from the home screen and waits for the reader.
 * The list scrolls, so the entry is scrolled into view first.
 */
fun ReadMdRule.openWelcomeSample() {
    val sample = string(R.string.sample_welcome)
    onNode(hasScrollAction()).performScrollToNode(hasText(sample))
    onNodeWithText(sample).performClick()
    // The reader shows the bundled document's name ("welcome") in its app bar once it has loaded.
    waitFor { hasTextNode(WELCOME_TITLE) }
}

/** The bundled sample is stored as res/raw/welcome.md, and its name is the reader's title. */
const val WELCOME_TITLE = "welcome"
