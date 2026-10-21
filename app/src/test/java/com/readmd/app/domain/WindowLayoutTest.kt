package com.readmd.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowLayoutTest {

    @Test
    fun `phones in portrait keep the drawer`() {
        assertFalse(WindowLayout.isWide(360))
        assertFalse(WindowLayout.isWide(411))
        assertFalse(WindowLayout.isWide(WindowLayout.WIDE_DP - 1))
    }

    @Test
    fun `landscape phones and tablets get the side pane`() {
        assertTrue(WindowLayout.isWide(WindowLayout.WIDE_DP))
        assertTrue(WindowLayout.isWide(851))
        assertTrue(WindowLayout.isWide(1280))
    }
}
