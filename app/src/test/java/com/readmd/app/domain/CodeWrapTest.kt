package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeWrapTest {

    @Test
    fun `ids round-trip and unknown ids fall back to wrapping`() {
        CodeWrap.entries.forEach { assertEquals(it, CodeWrap.fromId(it.id)) }
        assertEquals(CodeWrap.WRAP, CodeWrap.fromId("sideways"))
        assertEquals(CodeWrap.WRAP, CodeWrap.fromId(null))
    }

    @Test
    fun `a line is long only above the limit`() {
        assertFalse(CodeWrap.hasLongLine("x".repeat(CodeWrap.LONG_LINE_CHARS)))
        assertTrue(CodeWrap.hasLongLine("x".repeat(CodeWrap.LONG_LINE_CHARS + 1)))
    }

    @Test
    fun `any long line makes the block long`() {
        val code = "short\n" + "y".repeat(CodeWrap.LONG_LINE_CHARS + 5) + "\nshort"
        assertTrue(CodeWrap.hasLongLine(code))
        assertFalse(CodeWrap.hasLongLine(""))
    }
}
