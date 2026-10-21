package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingPositionsTest {

    @Test
    fun `get returns null for an unknown document`() {
        assertNull(ReadingPositions.get(emptySet(), "doc"))
    }

    @Test
    fun `upsert then get round-trips the position`() {
        val entries = ReadingPositions.upsert(emptySet(), "content://a/b:c", 420, now = 1L)
        assertEquals(420, ReadingPositions.get(entries, "content://a/b:c"))
    }

    @Test
    fun `upsert replaces the previous entry for the same document`() {
        val first = ReadingPositions.upsert(emptySet(), "doc", 100, now = 1L)
        val second = ReadingPositions.upsert(first, "doc", 700, now = 2L)
        assertEquals(1, second.size)
        assertEquals(700, ReadingPositions.get(second, "doc"))
    }

    @Test
    fun `upsert clamps out of range positions`() {
        val entries = ReadingPositions.upsert(emptySet(), "doc", 5000, now = 1L)
        assertEquals(ReadingPositions.FULL, ReadingPositions.get(entries, "doc"))
    }

    @Test
    fun `upsert keeps only the most recent entries`() {
        var entries = emptySet<String>()
        for (i in 1..5) entries = ReadingPositions.upsert(entries, "doc$i", i, now = i.toLong(), max = 3)
        assertEquals(3, entries.size)
        assertNull(ReadingPositions.get(entries, "doc1"))
        assertNull(ReadingPositions.get(entries, "doc2"))
        assertEquals(5, ReadingPositions.get(entries, "doc5"))
    }
}
