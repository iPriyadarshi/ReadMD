package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarksTest {

    private fun bookmark(doc: String = "doc", permille: Int, label: String = "L", at: Long = 1L) =
        Bookmark(doc, permille, label, at)

    @Test
    fun `add then forDoc round-trips and sorts by position`() {
        var entries = emptySet<String>()
        entries = Bookmarks.add(entries, bookmark(permille = 700, label = "Later"))
        entries = Bookmarks.add(entries, bookmark(permille = 100, label = "Earlier"))
        assertEquals(listOf(100, 700), Bookmarks.forDoc(entries, "doc").map { it.permille })
        assertEquals("Earlier", Bookmarks.forDoc(entries, "doc").first().label)
    }

    @Test
    fun `bookmarks are kept per document`() {
        var entries = Bookmarks.add(emptySet(), bookmark("a", 100))
        entries = Bookmarks.add(entries, bookmark("b", 200))
        assertEquals(listOf(100), Bookmarks.forDoc(entries, "a").map { it.permille })
        assertEquals(listOf(200), Bookmarks.forDoc(entries, "b").map { it.permille })
    }

    @Test
    fun `adding at the same position replaces the bookmark`() {
        var entries = Bookmarks.add(emptySet(), bookmark(permille = 300, label = "Old", at = 1L))
        entries = Bookmarks.add(entries, bookmark(permille = 300, label = "New", at = 2L))
        assertEquals(listOf("New"), Bookmarks.forDoc(entries, "doc").map { it.label })
    }

    @Test
    fun `remove deletes only that position of that document`() {
        var entries = Bookmarks.add(emptySet(), bookmark("a", 100))
        entries = Bookmarks.add(entries, bookmark("a", 200))
        entries = Bookmarks.add(entries, bookmark("b", 100))
        entries = Bookmarks.remove(entries, "a", 100)
        assertEquals(listOf(200), Bookmarks.forDoc(entries, "a").map { it.permille })
        assertEquals(listOf(100), Bookmarks.forDoc(entries, "b").map { it.permille })
    }

    @Test
    fun `oldest bookmarks are dropped beyond the per-document limit`() {
        var entries = emptySet<String>()
        for (i in 1..5) entries = Bookmarks.add(entries, bookmark(permille = i * 10, at = i.toLong()), maxPerDoc = 3)
        assertEquals(listOf(30, 40, 50), Bookmarks.forDoc(entries, "doc").map { it.permille })
    }

    @Test
    fun `doc ids with separators and out of range positions are handled`() {
        val id = "content://auth/tree/primary%3ADocs/document/primary%3ADocs%2Fa.md"
        val entries = Bookmarks.add(emptySet(), bookmark(id, 5000, "x"))
        assertEquals(ReadingPositions.FULL, Bookmarks.forDoc(entries, id).single().permille)
    }

    @Test
    fun `labelFor picks the closest heading above the position`() {
        val headings = listOf("Intro" to 0f, "Setup" to 500f, "Usage" to 1200f)
        assertEquals("Intro", Bookmarks.labelFor(headings, 100f))
        assertEquals("Setup", Bookmarks.labelFor(headings, 900f))
        assertEquals("Usage", Bookmarks.labelFor(headings, 1190f))
        assertNull(Bookmarks.labelFor(emptyList(), 100f))
        assertNull(Bookmarks.labelFor(listOf("Late" to 500f), 100f))
    }

    @Test
    fun `malformed entries are ignored`() {
        assertTrue(Bookmarks.forDoc(setOf("garbage"), "doc").isEmpty())
    }
}
