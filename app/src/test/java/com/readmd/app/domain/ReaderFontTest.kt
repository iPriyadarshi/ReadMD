package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFontTest {

    @Test
    fun `fromId round-trips every font`() {
        ReaderFont.entries.forEach { assertEquals(it, ReaderFont.fromId(it.id)) }
    }

    @Test
    fun `unknown or missing id falls back to the default`() {
        assertEquals(ReaderFont.DEFAULT, ReaderFont.fromId("comic"))
        assertEquals(ReaderFont.DEFAULT, ReaderFont.fromId(null))
    }
}
