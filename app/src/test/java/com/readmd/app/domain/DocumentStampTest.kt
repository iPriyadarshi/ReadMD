package com.readmd.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentStampTest {

    private val base = DocumentStamp(lastModified = 1000L, size = 50L)

    @Test
    fun `identical stamps are not a change`() {
        assertFalse(DocumentStamp.changed(base, base.copy()))
    }

    @Test
    fun `a newer modified time is a change`() {
        assertTrue(DocumentStamp.changed(base, base.copy(lastModified = 2000L)))
    }

    @Test
    fun `a different size is a change even with the same time`() {
        assertTrue(DocumentStamp.changed(base, base.copy(size = 51L)))
    }

    @Test
    fun `stamps with only a time or only a size are compared on what is known`() {
        val timeOnly = DocumentStamp(1000L, null)
        assertFalse(DocumentStamp.changed(timeOnly, DocumentStamp(1000L, null)))
        assertTrue(DocumentStamp.changed(timeOnly, DocumentStamp(1500L, null)))
        val sizeOnly = DocumentStamp(null, 10L)
        assertTrue(DocumentStamp.changed(sizeOnly, DocumentStamp(null, 11L)))
    }

    @Test
    fun `missing or empty stamps never report a change`() {
        val empty = DocumentStamp(null, null)
        assertTrue(empty.isEmpty)
        assertFalse(DocumentStamp.changed(null, base))
        assertFalse(DocumentStamp.changed(base, null))
        assertFalse(DocumentStamp.changed(empty, base))
        assertFalse(DocumentStamp.changed(base, empty))
    }
}
