package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {

    @Test
    fun `documents up to the limit can be shared`() {
        assertTrue(ShareText.fits(""))
        assertTrue(ShareText.fits("x".repeat(ShareText.MAX_CHARS)))
    }

    @Test
    fun `documents over the limit cannot be shared as text`() {
        assertFalse(ShareText.fits("x".repeat(ShareText.MAX_CHARS + 1)))
    }

    @Test
    fun `the subject drops the markdown extension`() {
        assertEquals("Notes", ShareText.subject("Notes.md"))
        assertEquals("Notes", ShareText.subject("Notes.markdown"))
        assertEquals("My file", ShareText.subject("  My file  "))
    }

    @Test
    fun `an empty title gets a generic subject`() {
        assertEquals("Document", ShareText.subject(""))
        assertEquals("Document", ShareText.subject(".md"))
    }
}
