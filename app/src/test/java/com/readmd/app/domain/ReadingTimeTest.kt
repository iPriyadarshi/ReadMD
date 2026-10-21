package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimeTest {

    @Test
    fun `wordCount ignores extra whitespace and blank text`() {
        assertEquals(3, ReadingTime.wordCount("  one two\n\nthree  "))
        assertEquals(0, ReadingTime.wordCount("   \n "))
    }

    @Test
    fun `minutesLeft rounds up from the unread part`() {
        assertEquals(5, ReadingTime.minutesLeft(1000, 0f))
        assertEquals(3, ReadingTime.minutesLeft(1000, 0.4f))
        assertEquals(1, ReadingTime.minutesLeft(1000, 0.99f))
    }

    @Test
    fun `minutesLeft is zero at the end and tolerates out of range progress`() {
        assertEquals(0, ReadingTime.minutesLeft(1000, 1f))
        assertEquals(0, ReadingTime.minutesLeft(1000, 2f))
        assertEquals(5, ReadingTime.minutesLeft(1000, -1f))
    }

    @Test
    fun `label formats minutes and handles edge cases`() {
        assertEquals("5 min left", ReadingTime.label(1000, 0f))
        assertEquals("Finished", ReadingTime.label(1000, 1f))
        assertNull(ReadingTime.label(0, 0f))
    }
}
