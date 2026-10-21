package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLayoutTest {

    @Test
    fun `line spacing ids round-trip`() {
        LineSpacing.entries.forEach { assertEquals(it, LineSpacing.fromId(it.id)) }
    }

    @Test
    fun `page margin ids round-trip`() {
        PageMargin.entries.forEach { assertEquals(it, PageMargin.fromId(it.id)) }
    }

    @Test
    fun `unknown ids fall back to the defaults`() {
        assertEquals(LineSpacing.DEFAULT, LineSpacing.fromId("tight"))
        assertEquals(LineSpacing.DEFAULT, LineSpacing.fromId(null))
        assertEquals(PageMargin.DEFAULT, PageMargin.fromId("huge"))
        assertEquals(PageMargin.DEFAULT, PageMargin.fromId(null))
    }

    @Test
    fun `presets are ordered from smallest to largest`() {
        assertTrue(LineSpacing.entries.zipWithNext().all { (a, b) -> a.multiplier < b.multiplier })
        assertTrue(PageMargin.entries.zipWithNext().all { (a, b) -> a.dp < b.dp })
    }
}
